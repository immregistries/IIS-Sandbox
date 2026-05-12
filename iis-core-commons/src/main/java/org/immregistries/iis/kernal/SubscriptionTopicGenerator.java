package org.immregistries.iis.kernal;

import org.hl7.fhir.r5.model.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import static org.immregistries.iis.kernal.GlobalConstants.TAG_SEARCH_PARAM;

public final class SubscriptionTopicGenerator {
	public static final String DATA_QUALITY_ISSUES_TOPIC_NAME = "/data-quality-issues";
	public static final String PATIENT_TOPIC_NAME = "/Patient";
	public static final String GROUP_TOPIC_NAME = "/Group";

	public static SubscriptionTopic getDataQualityIssuesSubscriptionTopic() {
		SubscriptionTopic.SubscriptionTopicEventTriggerComponent eventTrigger =
			new SubscriptionTopic.SubscriptionTopicEventTriggerComponent().setEvent( new CodeableConcept()
					// https://terminology.hl7.org/3.1.0/ValueSet-v2-0003.html
					// Codes for CRUD on patients with HL7v2
					.addCoding(new Coding().setSystem("http://terminology.hl7.org/ValueSet/v2-0003").setCode("A04"))
					.addCoding(new Coding().setSystem("http://terminology.hl7.org/ValueSet/v2-0003").setCode("A28"))
					.addCoding(new Coding().setSystem("http://terminology.hl7.org/ValueSet/v2-0003").setCode("A31"))
					.addCoding(new Coding().setSystem("IIS-Sandbox").setCode("Manual Trigger"))
				// TODO add MQE codes ?
			).setResource("OperationOutcome?");


		String baseUrl = "";
		try {
			baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
		} catch (IllegalStateException ignored) {
		}

		SubscriptionTopic topic  = new SubscriptionTopic()
			.setDescription("Testing communication between EHR and IIS and operation outcome")
			.setUrl(baseUrl + "/SubscriptionTopic" + DATA_QUALITY_ISSUES_TOPIC_NAME)
			.setStatus(Enumerations.PublicationStatus.DRAFT)
			.setExperimental(true).setPublisher("Aira/Nist")
			.setTitle("Health equity data quality requests within Immunization systems");
		topic.setId("sandboxDataQualityIssues");
		topic.addResourceTrigger(new SubscriptionTopic.SubscriptionTopicResourceTriggerComponent()
			.setResource("OperationOutcome")
			.setQueryCriteria(new SubscriptionTopic.SubscriptionTopicResourceTriggerQueryCriteriaComponent()
				.setCurrent("OperationOutcome?")
			)
		);
		topic.addEventTrigger(eventTrigger);
		topic.addCanFilterBy(new SubscriptionTopic.SubscriptionTopicCanFilterByComponent()
			.setDescription("test")
			.setResource("OperationOutcome")
			.setFilterParameter(TAG_SEARCH_PARAM)
			.addModifier(Enumerations.SearchModifierCode.EXACT)
		);
		topic.addNotificationShape().setResource("OperationOutcome");
		return topic;
	}


	static public SubscriptionTopic getGroupSubscriptionTopic() {
		String baseUrl = "";
		try {
			baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
		} catch (IllegalStateException ignored) {
		}
		SubscriptionTopic topic  = new SubscriptionTopic()
			.setDescription("Testing communication between EHR and IIS and operation outcome")
			.setUrl(baseUrl + "/SubscriptionTopic" + GROUP_TOPIC_NAME)
			.setStatus(Enumerations.PublicationStatus.DRAFT)
			.setExperimental(true).setPublisher("Aira/Nist")
			.setTitle("Health equity data quality requests within Immunization systems");
		topic.setId("sandboxGroup");
		topic.addResourceTrigger(new SubscriptionTopic.SubscriptionTopicResourceTriggerComponent()
			.setResource("Group")
			.setQueryCriteria(new SubscriptionTopic.SubscriptionTopicResourceTriggerQueryCriteriaComponent()
				.setCurrent("Group?")
			)
		);
		topic.addCanFilterBy(new SubscriptionTopic.SubscriptionTopicCanFilterByComponent()
			.setDescription("test name filter")
			.setResource("Group")
			.setFilterParameter(Group.SP_NAME)
			.addModifier(Enumerations.SearchModifierCode.EXACT)
		);
		topic.addCanFilterBy(new SubscriptionTopic.SubscriptionTopicCanFilterByComponent()
			.setDescription("test Identifier filter")
			.setResource("Group")
			.setFilterParameter(Group.SP_IDENTIFIER)
			.addModifier(Enumerations.SearchModifierCode.IDENTIFIER)
		);
		topic.addNotificationShape().setResource("Group");
		return topic;
	}


	public static SubscriptionTopic getPatientSubscriptionTopic() {
		String baseUrl = "";
		try {
			baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
		} catch (IllegalStateException ignored) {
		}
		SubscriptionTopic topic = new SubscriptionTopic()
			.setDescription("Testing communication between EHR and IIS")
			.setUrl(baseUrl + "/SubscriptionTopic" + PATIENT_TOPIC_NAME)
			.setStatus(Enumerations.PublicationStatus.DRAFT)
			.setExperimental(true).setPublisher("Aira/Nist")
			.setTitle("Health equity data quality requests within Immunization systems");
		topic.setId("sandboxPatient");
		topic.addResourceTrigger(new SubscriptionTopic.SubscriptionTopicResourceTriggerComponent()
			.setResource("Patient")
			.setQueryCriteria(new SubscriptionTopic.SubscriptionTopicResourceTriggerQueryCriteriaComponent()
				.setCurrent("Patient?")
			)
		);
		topic.addCanFilterBy(new SubscriptionTopic.SubscriptionTopicCanFilterByComponent()
			.setDescription("test name filter")
			.setResource("Patient")
			.setFilterParameter(Patient.SP_NAME)
			.addModifier(Enumerations.SearchModifierCode.EXACT)
		);
		topic.addNotificationShape().setResource("Patient");
		return topic;
	}
}
