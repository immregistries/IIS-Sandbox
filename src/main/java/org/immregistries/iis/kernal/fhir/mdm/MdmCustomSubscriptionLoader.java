package org.immregistries.iis.kernal.fhir.mdm;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.mdm.config.MdmSubscriptionLoader;
import ca.uhn.fhir.jpa.subscription.channel.api.ChannelProducerSettings;
import ca.uhn.fhir.jpa.subscription.channel.subscription.IChannelNamer;
import ca.uhn.fhir.jpa.subscription.match.registry.SubscriptionLoader;
import ca.uhn.fhir.jpa.topic.SubscriptionTopicLoader;
import ca.uhn.fhir.mdm.api.IMdmSettings;
import ca.uhn.fhir.mdm.api.MdmConstants;
import ca.uhn.fhir.mdm.log.Logs;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.exceptions.ResourceGoneException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.util.HapiExtensions;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

public class MdmCustomSubscriptionLoader extends MdmSubscriptionLoader {

	public static final String MDM_SUBSCIPRION_ID_PREFIX = "mdm-";
	private static final Logger ourLog = Logs.getMdmTroubleshootingLog();

	@Autowired
	public FhirContext myFhirContext;

	@Autowired
	public DaoRegistry myDaoRegistry;

	@Autowired
	IChannelNamer myChannelNamer;

	@Autowired(required = false)
	private SubscriptionTopicLoader mySubscriptionTopicLoader;
	@Autowired
	SubscriptionLoader mySubscriptionLoader;

	@Autowired
	IMdmSettings myMdmSettings;

	private IFhirResourceDao<IBaseResource> mySubscriptionDao;
	private IFhirResourceDao<IBaseResource> mySubscriptionTopicDao;

	private static final String TOPIC_ID = "r5-mdm-topic";
	private static final String TOPIC_URL = "test/" + TOPIC_ID; // TODO host the topic somewhere ? may be useless
	private static final String TOPIC = "{" +
		"  \"resourceType\": \"SubscriptionTopic\"," +
		"  \"id\": \"" + TOPIC_ID + "\"," +
		"  \"url\": \"" + TOPIC_URL + "\"," +
		"  \"title\": \"Health equity data quality requests within Immunization systems\"," +
		"  \"status\": \"draft\"," +
		"  \"experimental\": true," +
		"  \"description\": \"Testing communication between EHR and IIS and operation outcome\"," +
		"  \"notificationShape\": [ {" +
		"    \"resource\": \"OperationOutcome\"" +
		"  } ]" +
		"}";

	@Override
	public synchronized void daoUpdateMdmSubscriptions() {
		List<IBaseResource> subscriptions;
		List<String> mdmResourceTypes = myMdmSettings.getMdmRules().getMdmTypes();
		switch (myFhirContext.getVersion().getVersion()) {
			case R4:
				subscriptions = mdmResourceTypes.stream()
					.map(resourceType ->
						buildMdmSubscriptionR4(MDM_SUBSCIPRION_ID_PREFIX + resourceType, resourceType + "?"))
					.collect(Collectors.toList());
				break;
			case R5:
				subscriptions = mdmResourceTypes.stream()
					.map(resourceType ->
						buildMdmSubscriptionR5(MDM_SUBSCIPRION_ID_PREFIX + resourceType, resourceType + "?"))
					.collect(Collectors.toList());
				break;
			default:
				throw new ConfigurationException(Msg.code(736) + "MDM not supported for FHIR version "
					+ myFhirContext.getVersion().getVersion());
		}
		mySubscriptionDao = myDaoRegistry.getResourceDao("Subscription");
		for (IBaseResource subscription : subscriptions) {
			updateIfNotPresent(subscription);
		}
		// After loading all the subscriptions, sync the subscriptions to the registry.
		if (subscriptions != null && subscriptions.size() > 0) {
			mySubscriptionTopicLoader.syncDatabaseToCache();
			mySubscriptionLoader.syncDatabaseToCache();
			mySubscriptionTopicLoader.registerListener();
		}
	}

	synchronized void updateIfNotPresent(IBaseResource theSubscription) {
		try {
			mySubscriptionDao.read(theSubscription.getIdElement(), SystemRequestDetails.forAllPartitions());
		} catch (ResourceNotFoundException | ResourceGoneException e) {
			ourLog.info("Creating subscription " + theSubscription.getIdElement());
			mySubscriptionDao.update(theSubscription, SystemRequestDetails.forAllPartitions());
		}
	}

	synchronized void updateTopicIfNotPresent(IBaseResource theSubscriptionTopic) {
		mySubscriptionTopicDao = myDaoRegistry.getResourceDao("SubscriptionTopic");
		try {
			mySubscriptionTopicDao.read(theSubscriptionTopic.getIdElement(), SystemRequestDetails.forAllPartitions());
		} catch (ResourceNotFoundException | ResourceGoneException e) {
			ourLog.info("Creating topic " + theSubscriptionTopic.getIdElement());
			mySubscriptionTopicDao.update(theSubscriptionTopic, SystemRequestDetails.forAllPartitions());
		}
	}

	private Subscription buildMdmSubscriptionR5(String theId, String theCriteria) { //TODO test and improve

		//Setting up the topic
		IParser parser = myFhirContext.newJsonParser();
		SubscriptionTopic topic = parser.parseResource(SubscriptionTopic.class, TOPIC);
		topic.setId(theId + "-topic");
		topic.setUrl(TOPIC_URL + "-" + theId);
		topic.addResourceTrigger().setResource(theCriteria)
			.addSupportedInteraction(SubscriptionTopic.InteractionTrigger.CREATE)
			.addSupportedInteraction(SubscriptionTopic.InteractionTrigger.UPDATE)
			.setQueryCriteria(new SubscriptionTopic.SubscriptionTopicResourceTriggerQueryCriteriaComponent()
				.setResultForCreate(SubscriptionTopic.CriteriaNotExistsBehavior.TESTPASSES)
				.setCurrent(theCriteria)
			);
		updateTopicIfNotPresent(topic);

		Subscription retval = new Subscription();
		retval.setId(theId);
		retval.setReason("MDM");
		retval.setStatus(Enumerations.SubscriptionStatusCodes.REQUESTED);

		retval.getMeta().addTag().setSystem(MdmConstants.SYSTEM_MDM_MANAGED).setCode(MdmConstants.CODE_HAPI_MDM_MANAGED);
		retval.addExtension().setUrl(HapiExtensions.EXTENSION_SUBSCRIPTION_CROSS_PARTITION).setValue(new BooleanType().setValue(true));
		retval.setChannelType(new Coding("http://terminology.hl7.org/CodeSystem/subscription-channel-type", "message", "message"));
		IChannelNamer var10001 = this.myChannelNamer;
		ChannelProducerSettings var10003 = new ChannelProducerSettings();
		retval.setEndpoint("channel:" + var10001.getChannelName("empi", var10003));
		retval.setContentType("application/json");
		retval.setTopicElement(new CanonicalType(topic.getUrl()));
		retval.addContained(topic);
		return retval;
	}


	private org.hl7.fhir.r4.model.Subscription buildMdmSubscriptionR4(String theId, String theCriteria) {
		org.hl7.fhir.r4.model.Subscription retval = new org.hl7.fhir.r4.model.Subscription();
		retval.setId(theId);
		retval.setReason("MDM");
		retval.setStatus(org.hl7.fhir.r4.model.Subscription.SubscriptionStatus.REQUESTED);
		retval.setCriteria(theCriteria);
		retval.getMeta()
			.addTag()
			.setSystem(MdmConstants.SYSTEM_MDM_MANAGED)
			.setCode(MdmConstants.CODE_HAPI_MDM_MANAGED);
		retval.addExtension()
			.setUrl(HapiExtensions.EXTENSION_SUBSCRIPTION_CROSS_PARTITION)
			.setValue(new org.hl7.fhir.r4.model.BooleanType().setValue(true));
		org.hl7.fhir.r4.model.Subscription.SubscriptionChannelComponent channel = retval.getChannel();
		channel.setType(org.hl7.fhir.r4.model.Subscription.SubscriptionChannelType.MESSAGE);
		channel.setEndpoint("channel:"
			+ myChannelNamer.getChannelName(IMdmSettings.EMPI_CHANNEL_NAME, new ChannelProducerSettings()));
		channel.setPayload("application/json");
		return retval;
	}

}

