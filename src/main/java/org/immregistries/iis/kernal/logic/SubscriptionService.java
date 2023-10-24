package org.immregistries.iis.kernal.logic;

import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.subscription.match.deliver.resthook.SubscriptionDeliveringRestHookSubscriber;
import ca.uhn.fhir.jpa.subscription.match.registry.SubscriptionCanonicalizer;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.InternalClient.RepositoryClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.hl7.fhir.r5.model.Bundle.HTTPVerb.DELETE;


@Service
public class SubscriptionService {
	Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

	@Autowired
	IFhirSystemDao fhirSystemDao;
	@Autowired
	RepositoryClientFactory repositoryClientFactory;
//	@Autowired
//	SubscriptionDeliveringRestHookSubscriber subscriptionDeliveringRestHookSubscriber;
//	@Autowired
//	SubscriptionCanonicalizer subscriptionCanonicalizer;

//	public Subscription searchRelatedSubscription(Immunization baseResource, RequestDetails requestDetails) {
//		UserAccess userAccess = (UserAccess) requestDetails.getAttribute("userAccess");
//		/**
//		 * define materialization of subscription on immunization with
//		 * 	- TAG ?
//		 * 	- Identifier System ?
//		 * 	- $match operations ?
//		 */
//		Bundle bundle = repositoryClientFactory.newGenericClient(userAccess).search().forResource(Subscription.class)
//			.where(Subscription.STATUS.exactly().code(Enumerations.SubscriptionStatusCodes.ACTIVE.toCode()))
////			.and(Subscription.IDENTIFIER.hasSystemWithAnyCode(baseResource.getIdentifier()))  TODO change
////			.and(Subscription.)  TODO change
//			.returnBundle(Bundle.class).execute();
//		Subscription subscription = (Subscription) bundle.getEntryFirstRep().getResource();
////		subscription.gets
//		return subscription;
//	}


	public String triggerWithResource(Subscription subscription, List<Pair<String, Bundle.HTTPVerb>> requests) {
//		try {
//			UserAccess userAccess = ServletHelper.getUserAccess();

//			ResourceDeliveryMessage resourceDeliveryMessage = new ResourceDeliveryMessage();
//			resourceDeliveryMessage.setSubscription(subscriptionCanonicalizer.canonicalize(subscription));
//			resourceDeliveryMessage.setPartitionId(RequestPartitionId.fromPartitionName(""+userAccess.getAccessName()));
//			resourceDeliveryMessage.setOperationType(BaseResourceMessage.OperationTypeEnum.UPDATE);
//			resourceDeliveryMessage.setPayload(fhirSystemDao.getContext(), resource, EncodingEnum.JSON);
//			subscriptionDeliveringRestHookSubscriber.handleMessage(resourceDeliveryMessage);

			IGenericClient endpointClient = repositoryClientFactory.newGenericClient(subscription.getEndpoint());
			/**
			 * Adding headers for security requirements
			 */
			AdditionalRequestHeadersInterceptor additionalRequestHeadersInterceptor = new AdditionalRequestHeadersInterceptor();
			for (Subscription.SubscriptionParameterComponent parameterComponent : subscription.getParameter()) {
//				additionalRequestHeadersInterceptor.addHeaderValue(parameterComponent.getName(), parameterComponent.getValue());
			}
			endpointClient.registerInterceptor(additionalRequestHeadersInterceptor);
			Bundle notificationBundle = new Bundle(Bundle.BundleType.SUBSCRIPTIONNOTIFICATION);
			SubscriptionStatus status = new SubscriptionStatus()
				.setType(SubscriptionStatus.SubscriptionNotificationType.EVENTNOTIFICATION)
				.setStatus(subscription.getStatus())
//				.setSubscription(subscription.getIdentifierFirstRep().getAssigner())
				.setSubscription(new Reference().setIdentifier(subscription.getIdentifierFirstRep()))
//				.set
//				.setEventsInNotification(1)
//				.setEventsSinceSubscriptionStart(1)
				.setTopic(subscription.getTopic());
			/**
			 * First entry is SubscriptionStatus
			 */
			notificationBundle.addEntry().setResource(status);
			for (Pair<String, Bundle.HTTPVerb> pair : requests) {
				switch(pair.getValue()){
					case PUT:
					case POST: {
						Resource resource = (Resource) parseResource(pair.getKey());
						notificationBundle.addEntry()
							.setResource(resource)
							.setRequest(new Bundle.BundleEntryRequestComponent(pair.getValue(), resource.getId()));
						break;
					}
					case DELETE: {
						String url = ""; // TODO get resourceType?
						try {
							Identifier identifier = (Identifier) parseResource(pair.getKey());
							url = "?identifier=";
							if (StringUtils.isNotBlank(identifier.getSystem())) {
								url += identifier.getSystem() + "|";
							}
							url += identifier.getValue();
						} catch (ClassCastException classCastException) {}

						if (url.isBlank()) {
							url = new  UrlType(pair.getKey()).getValue();
						}

						Bundle.BundleEntryComponent entry = notificationBundle.addEntry()
							.setRequest(new Bundle.BundleEntryRequestComponent(DELETE, url));
						break;
					}
				}
			}



			MethodOutcome outcome = endpointClient.create().resource(notificationBundle).execute();
			if (outcome.getResource() != null) {
//				out.println(parser.encodeResourceToString(outcome.getResource()));
			}
			if (outcome.getOperationOutcome() != null) {
//				out.println(parser.encodeResourceToString(outcome.getOperationOutcome()));
			}
			if (outcome.getId() != null) {
//				out.println(outcome.getId());
			}

//				MethodOutcome methodOutcome = localClient.create().resource(parsedResource).execute();
//				List<IPrimitiveType<String>> ids = new ArrayList<>();
//				ids.add(methodOutcome.getId());
//				List<IPrimitiveType<String>> urls = new ArrayList<>();
//				urls.add(new StringType("OperationOutcome?"));
//				subscriptionTriggeringProvider.triggerSubscription(new IdType(subscription.getId()),ids,urls);


			return "Success : \n\n" + fhirSystemDao.getContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(notificationBundle);
//		} catch (Exception e) {
//			e.printStackTrace();
//			return e.getLocalizedMessage();
//		}

	}

	private IBaseResource parseResource(String message) {
		if (message.startsWith("<")) {
			return repositoryClientFactory.getFhirContext().newXmlParser().parseResource(message);
		} else {
			return repositoryClientFactory.getFhirContext().newJsonParser().parseResource(message);
		}
	}
}
