package org.immregistries.iis.kernal.fhir.mdm;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.subscription.match.registry.SubscriptionCanonicalizer;
import ca.uhn.fhir.jpa.subscription.model.CanonicalSubscription;
import ca.uhn.fhir.jpa.subscription.model.CanonicalSubscriptionChannelType;
import ca.uhn.fhir.jpa.subscription.model.CanonicalTopicSubscription;
import ca.uhn.fhir.jpa.subscription.model.CanonicalTopicSubscriptionFilter;
import ca.uhn.fhir.model.dstu2.resource.Subscription;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.subscription.SubscriptionConstants;
import ca.uhn.fhir.util.HapiExtensions;
import ca.uhn.fhir.util.SubscriptionUtil;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r5.model.Enumerations;
import org.hl7.fhir.r5.model.SubscriptionTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ca.uhn.fhir.util.HapiExtensions.EX_SEND_DELETE_MESSAGES;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

/**
 * Modified from original to support R5 Subscription (experimental), especially for MDM
 */
public class IisSubscriptionCanonicalizer extends SubscriptionCanonicalizer {
	private static final Logger ourLog = LoggerFactory.getLogger(SubscriptionCanonicalizer.class);

	private final FhirContext myFhirContext;
	@Autowired
	public IisSubscriptionCanonicalizer(FhirContext theFhirContext) {
		super(theFhirContext);
		myFhirContext = theFhirContext;
	}





	public CanonicalSubscription canonicalize(IBaseResource theSubscription) {
		switch (myFhirContext.getVersion().getVersion()) {
			case R4:
				return canonicalizeR4(theSubscription);
			case R4B:
				return canonicalizeR4B(theSubscription);
			case R5:
				return canonicalizeR5(theSubscription);
			case DSTU2_HL7ORG:
			case DSTU2_1:
			default:
				throw new ConfigurationException(Msg.code(556) + "Subscription not supported for version: "
					+ myFhirContext.getVersion().getVersion());
		}
	}

	/**
	 * Extract the meta tags from the subscription and convert them to a simple string map.
	 *
	 * @param theSubscription The subscription to extract the tags from
	 * @return A map of tags System:Code
	 */
	private Map<String, String> extractTags(IBaseResource theSubscription) {
		Map<String, String> retVal = new HashMap<>();
		theSubscription.getMeta().getTag().stream()
			.filter(t -> t.getSystem() != null && t.getCode() != null)
			.forEach(t -> retVal.put(t.getSystem(), t.getCode()));
		return retVal;
	}

	private @Nonnull Map<String, List<String>> extractExtension(IBaseResource theSubscription) {
		try {
			switch (theSubscription.getStructureFhirVersionEnum()) {
				case DSTU2: {
					Subscription subscription =
						(Subscription) theSubscription;
					return subscription.getChannel().getUndeclaredExtensions().stream()
						.collect(Collectors.groupingBy(
							t -> t.getUrl(),
							mapping(t -> t.getValueAsPrimitive().getValueAsString(), toList())));
				}
				case DSTU3: {
					org.hl7.fhir.dstu3.model.Subscription subscription =
						(org.hl7.fhir.dstu3.model.Subscription) theSubscription;
					return subscription.getChannel().getExtension().stream()
						.collect(Collectors.groupingBy(
							t -> t.getUrl(),
							mapping(t -> t.getValueAsPrimitive().getValueAsString(), toList())));
				}
				case R4: {
					org.hl7.fhir.r4.model.Subscription subscription =
						(org.hl7.fhir.r4.model.Subscription) theSubscription;
					return subscription.getChannel().getExtension().stream()
						.collect(Collectors.groupingBy(
							t -> t.getUrl(),
							mapping(
								t -> {
									return t.getValueAsPrimitive().getValueAsString();
								},
								toList())));
				}
				case R5: {
					// TODO KHS fix org.hl7.fhir.r4b.model.BaseResource.getStructureFhirVersionEnum() for R4B
					if (theSubscription instanceof org.hl7.fhir.r4b.model.Subscription) {
						org.hl7.fhir.r4b.model.Subscription subscription =
							(org.hl7.fhir.r4b.model.Subscription) theSubscription;
						return subscription.getExtension().stream()
							.collect(Collectors.groupingBy(
								t -> t.getUrl(),
								mapping(t -> t.getValueAsPrimitive().getValueAsString(), toList())));
					} else if (theSubscription instanceof org.hl7.fhir.r5.model.Subscription) {
						org.hl7.fhir.r5.model.Subscription subscription =
							(org.hl7.fhir.r5.model.Subscription) theSubscription;
						return subscription.getExtension().stream()
							.collect(Collectors.groupingBy(
								t -> t.getUrl(),
								mapping(t -> t.getValueAsPrimitive().getValueAsString(), toList())));
					}
				}
				case DSTU2_HL7ORG:
				case DSTU2_1:
				default: {
					ourLog.error(
						"Failed to extract extension from subscription {}",
						theSubscription.getIdElement().toUnqualified().getValue());
					break;
				}
			}
		} catch (FHIRException theE) {
			ourLog.error(
				"Failed to extract extension from subscription {}",
				theSubscription.getIdElement().toUnqualified().getValue(),
				theE);
		}
		return Collections.emptyMap();
	}

	private CanonicalSubscription canonicalizeR4(IBaseResource theSubscription) {
		org.hl7.fhir.r4.model.Subscription subscription = (org.hl7.fhir.r4.model.Subscription) theSubscription;
		CanonicalSubscription retVal = new CanonicalSubscription();
		retVal.setStatus(subscription.getStatus());
		org.hl7.fhir.r4.model.Subscription.SubscriptionChannelComponent channel = subscription.getChannel();
		retVal.setHeaders(channel.getHeader());
		retVal.setChannelExtensions(extractExtension(subscription));
		retVal.setIdElement(subscription.getIdElement());
		retVal.setPayloadString(channel.getPayload());
		retVal.setPayloadSearchCriteria(
			getExtensionString(subscription, HapiExtensions.EXT_SUBSCRIPTION_PAYLOAD_SEARCH_CRITERIA));
		retVal.setTags(extractTags(subscription));
		setPartitionIdOnReturnValue(theSubscription, retVal);
		retVal.setCrossPartitionEnabled(SubscriptionUtil.isCrossPartition(theSubscription));

		List<org.hl7.fhir.r4.model.CanonicalType> profiles =
			subscription.getMeta().getProfile();
		for (org.hl7.fhir.r4.model.CanonicalType next : profiles) {
			if (SubscriptionConstants.SUBSCRIPTION_TOPIC_PROFILE_URL.equals(next.getValueAsString())) {
				retVal.setTopicSubscription(true);
			}
		}

		if (retVal.isTopicSubscription()) {
			CanonicalTopicSubscription topicSubscription = retVal.getTopicSubscription();
			topicSubscription.setTopic(getCriteria(theSubscription));

			// WIP STR5 support other content types
			topicSubscription.setContent(org.hl7.fhir.r5.model.Subscription.SubscriptionPayloadContent.FULLRESOURCE);
			retVal.setEndpointUrl(channel.getEndpoint());
			retVal.setChannelType(getChannelType(subscription));

			for (Extension next :
				subscription.getCriteriaElement().getExtension()) {
				if (SubscriptionConstants.SUBSCRIPTION_TOPIC_FILTER_URL.equals(next.getUrl())) {
					List<CanonicalTopicSubscriptionFilter> filters = CanonicalTopicSubscriptionFilter.fromQueryUrl(
						next.getValue().primitiveValue());
					filters.forEach(topicSubscription::addFilter);
				}
			}

			if (channel.hasExtension(SubscriptionConstants.SUBSCRIPTION_TOPIC_CHANNEL_HEARTBEAT_PERIOD_URL)) {
				Extension timeoutExtension = channel.getExtensionByUrl(
					SubscriptionConstants.SUBSCRIPTION_TOPIC_CHANNEL_HEARTBEAT_PERIOD_URL);
				topicSubscription.setHeartbeatPeriod(
					Integer.valueOf(timeoutExtension.getValue().primitiveValue()));
			}
			if (channel.hasExtension(SubscriptionConstants.SUBSCRIPTION_TOPIC_CHANNEL_TIMEOUT_URL)) {
				Extension timeoutExtension =
					channel.getExtensionByUrl(SubscriptionConstants.SUBSCRIPTION_TOPIC_CHANNEL_TIMEOUT_URL);
				topicSubscription.setTimeout(
					Integer.valueOf(timeoutExtension.getValue().primitiveValue()));
			}
			if (channel.hasExtension(SubscriptionConstants.SUBSCRIPTION_TOPIC_CHANNEL_MAX_COUNT)) {
				Extension timeoutExtension =
					channel.getExtensionByUrl(SubscriptionConstants.SUBSCRIPTION_TOPIC_CHANNEL_MAX_COUNT);
				topicSubscription.setMaxCount(
					Integer.valueOf(timeoutExtension.getValue().primitiveValue()));
			}
			if (channel.getPayloadElement()
				.hasExtension(SubscriptionConstants.SUBSCRIPTION_TOPIC_CHANNEL_PAYLOAD_CONTENT)) {
				Extension timeoutExtension = channel.getPayloadElement()
					.getExtensionByUrl(SubscriptionConstants.SUBSCRIPTION_TOPIC_CHANNEL_PAYLOAD_CONTENT);
				topicSubscription.setContent(org.hl7.fhir.r5.model.Subscription.SubscriptionPayloadContent.fromCode(
					timeoutExtension.getValue().primitiveValue()));
			}

		} else {
			retVal.setCriteriaString(getCriteria(theSubscription));
			retVal.setEndpointUrl(channel.getEndpoint());
			retVal.setChannelType(getChannelType(subscription));
		}

		if (retVal.getChannelType() == CanonicalSubscriptionChannelType.EMAIL) {
			String from;
			String subjectTemplate;
			try {
				from = channel.getExtensionString(HapiExtensions.EXT_SUBSCRIPTION_EMAIL_FROM);
				subjectTemplate = channel.getExtensionString(HapiExtensions.EXT_SUBSCRIPTION_SUBJECT_TEMPLATE);
			} catch (FHIRException theE) {
				throw new ConfigurationException(
					Msg.code(561) + "Failed to extract subscription extension(s): " + theE.getMessage(), theE);
			}
			retVal.getEmailDetails().setFrom(from);
			retVal.getEmailDetails().setSubjectTemplate(subjectTemplate);
		}

		if (retVal.getChannelType() == CanonicalSubscriptionChannelType.RESTHOOK) {
			String stripVersionIds;
			String deliverLatestVersion;
			try {
				stripVersionIds =
					channel.getExtensionString(HapiExtensions.EXT_SUBSCRIPTION_RESTHOOK_STRIP_VERSION_IDS);
				deliverLatestVersion =
					channel.getExtensionString(HapiExtensions.EXT_SUBSCRIPTION_RESTHOOK_DELIVER_LATEST_VERSION);
			} catch (FHIRException theE) {
				throw new ConfigurationException(
					Msg.code(562) + "Failed to extract subscription extension(s): " + theE.getMessage(), theE);
			}
			retVal.getRestHookDetails().setStripVersionId(Boolean.parseBoolean(stripVersionIds));
			retVal.getRestHookDetails().setDeliverLatestVersion(Boolean.parseBoolean(deliverLatestVersion));
		}

		List<Extension> topicExts = subscription.getExtensionsByUrl("http://hl7.org/fhir/subscription/topics");
		if (topicExts.size() > 0) {
			IBaseReference ref = (IBaseReference) topicExts.get(0).getValueAsPrimitive();
			if (!"EventDefinition".equals(ref.getReferenceElement().getResourceType())) {
				throw new PreconditionFailedException(Msg.code(563) + "Topic reference must be an EventDefinition");
			}
		}

		Extension extension = channel.getExtensionByUrl(EX_SEND_DELETE_MESSAGES);
		if (extension != null && extension.hasValue() && extension.getValue() instanceof BooleanType) {
			retVal.setSendDeleteMessages(((BooleanType) extension.getValue()).booleanValue());
		}
		return retVal;
	}

	private CanonicalSubscription canonicalizeR4B(IBaseResource theSubscription) {
		org.hl7.fhir.r4b.model.Subscription subscription = (org.hl7.fhir.r4b.model.Subscription) theSubscription;

		CanonicalSubscription retVal = new CanonicalSubscription();
		org.hl7.fhir.r4b.model.Enumerations.SubscriptionStatus status = subscription.getStatus();
		if (status != null) {
			retVal.setStatus(org.hl7.fhir.r4.model.Subscription.SubscriptionStatus.fromCode(status.toCode()));
		}
		setPartitionIdOnReturnValue(theSubscription, retVal);
		org.hl7.fhir.r4b.model.Subscription.SubscriptionChannelComponent channel = subscription.getChannel();
		retVal.setHeaders(channel.getHeader());
		retVal.setChannelExtensions(extractExtension(subscription));
		retVal.setIdElement(subscription.getIdElement());
		retVal.setPayloadString(channel.getPayload());
		retVal.setPayloadSearchCriteria(
			getExtensionString(subscription, HapiExtensions.EXT_SUBSCRIPTION_PAYLOAD_SEARCH_CRITERIA));
		retVal.setTags(extractTags(subscription));

		List<org.hl7.fhir.r4b.model.CanonicalType> profiles =
			subscription.getMeta().getProfile();
		for (org.hl7.fhir.r4b.model.CanonicalType next : profiles) {
			if (SubscriptionConstants.SUBSCRIPTION_TOPIC_PROFILE_URL.equals(next.getValueAsString())) {
				retVal.setTopicSubscription(true);
			}
		}

		if (retVal.isTopicSubscription()) {
			retVal.getTopicSubscription().setTopic(getCriteria(theSubscription));

			// WIP STR5 support other content types
			retVal.getTopicSubscription()
				.setContent(org.hl7.fhir.r5.model.Subscription.SubscriptionPayloadContent.FULLRESOURCE);
			retVal.setEndpointUrl(channel.getEndpoint());
			retVal.setChannelType(getChannelType(subscription));
		} else {
			retVal.setCriteriaString(getCriteria(theSubscription));
			retVal.setEndpointUrl(channel.getEndpoint());
			retVal.setChannelType(getChannelType(subscription));
		}

		if (retVal.getChannelType() == CanonicalSubscriptionChannelType.EMAIL) {
			String from;
			String subjectTemplate;
			try {
				from = getExtensionString(subscription, HapiExtensions.EXT_SUBSCRIPTION_EMAIL_FROM);
				subjectTemplate = getExtensionString(subscription, HapiExtensions.EXT_SUBSCRIPTION_SUBJECT_TEMPLATE);
			} catch (FHIRException theE) {
				throw new ConfigurationException(
					Msg.code(564) + "Failed to extract subscription extension(s): " + theE.getMessage(), theE);
			}
			retVal.getEmailDetails().setFrom(from);
			retVal.getEmailDetails().setSubjectTemplate(subjectTemplate);
		}

		if (retVal.getChannelType() == CanonicalSubscriptionChannelType.RESTHOOK) {
			String stripVersionIds;
			String deliverLatestVersion;
			try {
				stripVersionIds =
					getExtensionString(channel, HapiExtensions.EXT_SUBSCRIPTION_RESTHOOK_STRIP_VERSION_IDS);
				deliverLatestVersion =
					getExtensionString(channel, HapiExtensions.EXT_SUBSCRIPTION_RESTHOOK_DELIVER_LATEST_VERSION);
			} catch (FHIRException theE) {
				throw new ConfigurationException(
					Msg.code(565) + "Failed to extract subscription extension(s): " + theE.getMessage(), theE);
			}
			retVal.getRestHookDetails().setStripVersionId(Boolean.parseBoolean(stripVersionIds));
			retVal.getRestHookDetails().setDeliverLatestVersion(Boolean.parseBoolean(deliverLatestVersion));
		}

		List<org.hl7.fhir.r4b.model.Extension> topicExts =
			subscription.getExtensionsByUrl("http://hl7.org/fhir/subscription/topics");
		if (topicExts.size() > 0) {
			IBaseReference ref = (IBaseReference) topicExts.get(0).getValueAsPrimitive();
			if (!"EventDefinition".equals(ref.getReferenceElement().getResourceType())) {
				throw new PreconditionFailedException(Msg.code(566) + "Topic reference must be an EventDefinition");
			}
		}

		org.hl7.fhir.r4b.model.Extension extension = channel.getExtensionByUrl(EX_SEND_DELETE_MESSAGES);
		if (extension != null && extension.hasValue() && extension.hasValueBooleanType()) {
			retVal.setSendDeleteMessages(extension.getValueBooleanType().booleanValue());
		}

		return retVal;
	}

	private CanonicalSubscription canonicalizeR5(IBaseResource theSubscription) {
		org.hl7.fhir.r5.model.Subscription subscription = (org.hl7.fhir.r5.model.Subscription) theSubscription;

		CanonicalSubscription retVal = new CanonicalSubscription();

		setPartitionIdOnReturnValue(theSubscription, retVal);
		retVal.setChannelExtensions(extractExtension(subscription));
		retVal.setIdElement(subscription.getIdElement());
		retVal.setPayloadString(subscription.getContentType());
		retVal.setPayloadSearchCriteria(
			getExtensionString(subscription, HapiExtensions.EXT_SUBSCRIPTION_PAYLOAD_SEARCH_CRITERIA));
		retVal.setTags(extractTags(subscription));

		List<org.hl7.fhir.r5.model.Extension> topicExts =
			subscription.getExtensionsByUrl("http://hl7.org/fhir/subscription/topics");
		if (topicExts.size() > 0) {
			IBaseReference ref = (IBaseReference) topicExts.get(0).getValueAsPrimitive();
			if (!"EventDefinition".equals(ref.getReferenceElement().getResourceType())) {
				throw new PreconditionFailedException(Msg.code(2325) + "Topic reference must be an EventDefinition");
			}
		}

		// All R5 subscriptions are topic subscriptions
		retVal.setTopicSubscription(true);

		Enumerations.SubscriptionStatusCodes status = subscription.getStatus();
		if (status != null) {
			switch (status) {
				case REQUESTED:
					retVal.setStatus(org.hl7.fhir.r4.model.Subscription.SubscriptionStatus.REQUESTED);
					break;
				case ACTIVE:
					retVal.setStatus(org.hl7.fhir.r4.model.Subscription.SubscriptionStatus.ACTIVE);
					break;
				case ERROR:
					retVal.setStatus(org.hl7.fhir.r4.model.Subscription.SubscriptionStatus.ERROR);
					break;
				case OFF:
					retVal.setStatus(org.hl7.fhir.r4.model.Subscription.SubscriptionStatus.OFF);
					break;
				case NULL:
				case ENTEREDINERROR:
				default:
					ourLog.warn("Converting R5 Subscription status from {} to ERROR", status);
					retVal.setStatus(org.hl7.fhir.r4.model.Subscription.SubscriptionStatus.ERROR);
			}
		}
		retVal.getTopicSubscription().setContent(subscription.getContent());

		/**
		 * Temp fix for mdm triggering until topic subscription triggering is perfected in HAPIFHIR
		 * TODO prevent security issues as it runs depending on the resource contained
		 */
		if (!subscription.getContained().isEmpty()) {
			SubscriptionTopic topic = (SubscriptionTopic) subscription.getContained().get(0);
			retVal.setCrossPartitionEnabled(SubscriptionUtil.isCrossPartition(theSubscription));
			if (topic.hasResourceTrigger() && !topic.hasEventTrigger() && topic.getResourceTrigger().size() == 1) {
				retVal.setTopicSubscription(false);
				retVal.setCriteriaString(topic.getResourceTrigger().get(0).getQueryCriteria().getCurrent());
			}
		}

		retVal.setEndpointUrl(subscription.getEndpoint());
		retVal.getTopicSubscription().setTopic(subscription.getTopic());
		retVal.setChannelType(getChannelType(subscription));

		subscription.getFilterBy().forEach(filter -> {
			retVal.getTopicSubscription().addFilter(convertFilter(filter));
		});

		retVal.getTopicSubscription().setHeartbeatPeriod(subscription.getHeartbeatPeriod());
		retVal.getTopicSubscription().setMaxCount(subscription.getMaxCount());

		setR5FlagsBasedOnChannelType(subscription, retVal);

		return retVal;
	}

	private void setR5FlagsBasedOnChannelType(
		org.hl7.fhir.r5.model.Subscription subscription, CanonicalSubscription retVal) {
		if (retVal.getChannelType() == CanonicalSubscriptionChannelType.EMAIL) {
			String from;
			String subjectTemplate;
			try {
				from = getExtensionString(subscription, HapiExtensions.EXT_SUBSCRIPTION_EMAIL_FROM);
				subjectTemplate = getExtensionString(subscription, HapiExtensions.EXT_SUBSCRIPTION_SUBJECT_TEMPLATE);
			} catch (FHIRException theE) {
				throw new ConfigurationException(
					Msg.code(2323) + "Failed to extract subscription extension(s): " + theE.getMessage(), theE);
			}
			retVal.getEmailDetails().setFrom(from);
			retVal.getEmailDetails().setSubjectTemplate(subjectTemplate);
		}

		if (retVal.getChannelType() == CanonicalSubscriptionChannelType.RESTHOOK) {
			String stripVersionIds;
			String deliverLatestVersion;
			try {
				stripVersionIds =
					getExtensionString(subscription, HapiExtensions.EXT_SUBSCRIPTION_RESTHOOK_STRIP_VERSION_IDS);
				deliverLatestVersion = getExtensionString(
					subscription, HapiExtensions.EXT_SUBSCRIPTION_RESTHOOK_DELIVER_LATEST_VERSION);
			} catch (FHIRException theE) {
				throw new ConfigurationException(
					Msg.code(2324) + "Failed to extract subscription extension(s): " + theE.getMessage(), theE);
			}
			retVal.getRestHookDetails().setStripVersionId(Boolean.parseBoolean(stripVersionIds));
			retVal.getRestHookDetails().setDeliverLatestVersion(Boolean.parseBoolean(deliverLatestVersion));
		}
	}

	private CanonicalTopicSubscriptionFilter convertFilter(
		org.hl7.fhir.r5.model.Subscription.SubscriptionFilterByComponent theFilter) {
		CanonicalTopicSubscriptionFilter retVal = new CanonicalTopicSubscriptionFilter();
		retVal.setResourceType(theFilter.getResourceType());
		retVal.setFilterParameter(theFilter.getFilterParameter());
		retVal.setModifier(theFilter.getModifier());
		retVal.setComparator(theFilter.getComparator());
		retVal.setValue(theFilter.getValue());
		return retVal;
	}

	private void setPartitionIdOnReturnValue(IBaseResource theSubscription, CanonicalSubscription retVal) {
		RequestPartitionId requestPartitionId =
			(RequestPartitionId) theSubscription.getUserData(Constants.RESOURCE_PARTITION_ID);
		if (requestPartitionId != null) {
			retVal.setPartitionId(requestPartitionId.getFirstPartitionIdOrNull());
		}
	}

	private String getExtensionString(IBaseHasExtensions theBase, String theUrl) {
		return theBase.getExtension().stream()
			.filter(t -> theUrl.equals(t.getUrl()))
			.filter(t -> t.getValue() instanceof IPrimitiveType)
			.map(t -> (IPrimitiveType<?>) t.getValue())
			.map(t -> t.getValueAsString())
			.findFirst()
			.orElse(null);
	}

}
