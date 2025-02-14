package org.immregistries.iis.kernal.fhir.mdm;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.model.entity.StorageSettings;
import ca.uhn.fhir.jpa.partition.IRequestPartitionHelperSvc;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.jpa.subscription.match.matcher.matching.SubscriptionMatchingStrategy;
import ca.uhn.fhir.jpa.subscription.match.matcher.matching.SubscriptionStrategyEvaluator;
import ca.uhn.fhir.jpa.subscription.match.registry.SubscriptionCanonicalizer;
import ca.uhn.fhir.jpa.subscription.model.CanonicalSubscription;
import ca.uhn.fhir.jpa.subscription.model.CanonicalSubscriptionChannelType;
import ca.uhn.fhir.jpa.subscription.submit.interceptor.SubscriptionQueryValidator;
import ca.uhn.fhir.jpa.subscription.submit.interceptor.SubscriptionValidatingInterceptor;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.util.HapiExtensions;
import com.google.common.annotations.VisibleForTesting;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.SubscriptionTopic;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;


//@Interceptor

/**
 * Modified from original to support R5 Subscription (experimental), especially for MDM
 */
public class IisSubscriptionValidatingInterceptor extends SubscriptionValidatingInterceptor {


	@Autowired
	SubscriptionCanonicalizer mySubscriptionCanonicalizer;

	@Autowired
	DaoRegistry myDaoRegistry;

	@Autowired
	StorageSettings myStorageSettings;

	@Autowired
	SubscriptionStrategyEvaluator mySubscriptionStrategyEvaluator;

	@Autowired
	FhirContext myFhirContext;

	@Autowired
	IRequestPartitionHelperSvc myRequestPartitionHelperSvc;

	@Autowired
	SubscriptionQueryValidator mySubscriptionQueryValidator;

	@Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_CREATED)
	@Override
	public void resourcePreCreate(
		IBaseResource theResource, RequestDetails theRequestDetails, RequestPartitionId theRequestPartitionId) {
		validateSubmittedSubscription(
			theResource, theRequestDetails, theRequestPartitionId, Pointcut.STORAGE_PRESTORAGE_RESOURCE_CREATED);
	}

	@Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_UPDATED)
	@Override
	public void resourceUpdated(
		IBaseResource theOldResource,
		IBaseResource theResource,
		RequestDetails theRequestDetails,
		RequestPartitionId theRequestPartitionId) {
		validateSubmittedSubscription(
			theResource, theRequestDetails, theRequestPartitionId, Pointcut.STORAGE_PRESTORAGE_RESOURCE_UPDATED);
	}

	@VisibleForTesting
	void validateSubmittedSubscription(
		IBaseResource theSubscription,
		RequestDetails theRequestDetails,
		RequestPartitionId theRequestPartitionId,
		Pointcut thePointcut) {
		if (Pointcut.STORAGE_PRESTORAGE_RESOURCE_CREATED != thePointcut
			&& Pointcut.STORAGE_PRESTORAGE_RESOURCE_UPDATED != thePointcut) {
			throw new UnprocessableEntityException(Msg.code(2267)
				+ "Expected Pointcut to be either STORAGE_PRESTORAGE_RESOURCE_CREATED or STORAGE_PRESTORAGE_RESOURCE_UPDATED but was: "
				+ thePointcut);
		}


		if (!"Subscription".equals(myFhirContext.getResourceType(theSubscription))) {
			return;
		}

		CanonicalSubscription subscription;
		try {
			subscription = mySubscriptionCanonicalizer.canonicalize(theSubscription);
		} catch (InternalErrorException e) {
			throw new UnprocessableEntityException(Msg.code(955) + e.getMessage());
		}
		boolean finished = false;
		if (subscription.getStatus() == null) {
			throw new UnprocessableEntityException(Msg.code(8)
				+ "Can not process submitted Subscription - Subscription.status must be populated on this server");
		}

		switch (subscription.getStatus()) {
			case REQUESTED:
			case ACTIVE:
				break;
			case ERROR:
			case OFF:
			case NULL:
				finished = true;
				break;
		}

		validatePermissions(theSubscription, subscription, theRequestDetails, theRequestPartitionId, thePointcut);

		mySubscriptionCanonicalizer.setMatchingStrategyTag(theSubscription, null);

		if (!finished) {

			if (subscription.isTopicSubscription()) {
				if (myFhirContext.getVersion().getVersion()
					!= FhirVersionEnum
					.R4) { // In R4 topic subscriptions exist without a corresponding SubscriptionTopic
					// resource
					Optional<IBaseResource> oTopic = findSubscriptionTopicByUrl(subscription.getTopic());
					if (!oTopic.isPresent()) {
						throw new UnprocessableEntityException(
							Msg.code(2322) + "No SubscriptionTopic exists with topic: " + subscription.getTopic());
					}
				}
			} else {
				validateQuery(subscription.getCriteriaString(), "Subscription.criteria");

				if (subscription.getPayloadSearchCriteria() != null) {
					validateQuery(
						subscription.getPayloadSearchCriteria(),
						"Subscription.extension(url='" + HapiExtensions.EXT_SUBSCRIPTION_PAYLOAD_SEARCH_CRITERIA
							+ "')");
				}
			}

			validateChannelType(subscription);

			try {
				SubscriptionMatchingStrategy strategy = mySubscriptionStrategyEvaluator.determineStrategy(subscription);
				if (!(SubscriptionMatchingStrategy.IN_MEMORY == strategy)
					&& myStorageSettings.isOnlyAllowInMemorySubscriptions()) {
					throw new InvalidRequestException(
						Msg.code(2367)
							+ "This server is configured to only allow in-memory subscriptions. This subscription's criteria cannot be evaluated in-memory.");
				}
				mySubscriptionCanonicalizer.setMatchingStrategyTag(theSubscription, strategy);
			} catch (InvalidRequestException | DataFormatException e) {
				throw new UnprocessableEntityException(Msg.code(9) + "Invalid subscription criteria submitted: "
					+ subscription.getCriteriaString() + " " + e.getMessage());
			}

			if (subscription.getChannelType() == null) {
				throw new UnprocessableEntityException(
					Msg.code(10) + "Subscription.channel.type must be populated on this server");
			} else if (subscription.getChannelType() == CanonicalSubscriptionChannelType.MESSAGE) {
				validateMessageSubscriptionEndpoint(subscription.getEndpointUrl());
			}
		}
	}

	private Optional<IBaseResource> findSubscriptionTopicByUrl(String theCriteria) {
		myDaoRegistry.getResourceDao("SubscriptionTopic");
		SearchParameterMap map = SearchParameterMap.newSynchronous();
		map.add(SubscriptionTopic.SP_URL, new UriParam(theCriteria));
		IFhirResourceDao subscriptionTopicDao = myDaoRegistry.getResourceDao("SubscriptionTopic");
		SystemRequestDetails systemRequestDetails = SystemRequestDetails.forAllPartitions();
		//systemRequestDetails.setTenantId(SUBSCRIPTION_PARTITION_NAME);
		IBundleProvider search = subscriptionTopicDao.search(map, systemRequestDetails);
		return search.getResources(0, 1).stream().findFirst();
	}


//	protected void validatePermissions(
//		IBaseResource theSubscription,
//		CanonicalSubscription theCanonicalSubscription,
//		RequestDetails theRequestDetails,
//		RequestPartitionId theRequestPartitionId,
//		Pointcut thePointcut) {
//		// If the subscription has the cross partition tag
//		if (SubscriptionUtil.isCrossPartition(theSubscription)
//			&& !(theRequestDetails instanceof SystemRequestDetails)) {
//			if (!myStorageSettings.isCrossPartitionSubscriptionEnabled()) {
//				throw new UnprocessableEntityException(
//					Msg.code(2009) + "Cross partition subscription is not enabled on this server");
//			}
//
//			if (theRequestPartitionId == null && Pointcut.STORAGE_PRESTORAGE_RESOURCE_UPDATED == thePointcut) {
//				return;
//			}
//
//			// if we have a partition id already, we'll use that
//			// otherwise we might end up with READ and CREATE pointcuts
//			// returning conflicting partitions (say, all vs default)
//			RequestPartitionId toCheckPartitionId = theRequestPartitionId != null
//				? theRequestPartitionId
//				: determinePartition(theRequestDetails, theSubscription);
//
//			if (!toCheckPartitionId.isDefaultPartition()) {
//				throw new UnprocessableEntityException(
//					Msg.code(2010) + "Cross partition subscription must be created on the default partition");
//			}
//		}
//	}

}
