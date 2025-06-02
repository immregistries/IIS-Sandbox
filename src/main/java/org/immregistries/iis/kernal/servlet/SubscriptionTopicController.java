package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.searchparam.extractor.ISearchParamExtractor;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hl7.fhir.r5.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;

@RestController
@RequestMapping("/SubscriptionTopic")
public class SubscriptionTopicController {
	public static final String DATA_QUALITY_ISSUES_TOPIC_NAME = "/data-quality-issues";
	public static final String PATIENT_TOPIC_NAME = "/Patient";
	public static final String GROUP_TOPIC_NAME = "/Group";
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	FhirContext fhirContext;
	@Autowired
	ISearchParamRegistry iSearchParamRegistry;
	@Autowired
	ISearchParamExtractor iSearchParamExtractor;

	@GetMapping
	protected void doGetDefault(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		IParser parser = fhirContext.newJsonParser().setPrettyPrint(true);
		SubscriptionTopic topic = getDataQualityIssuesSubscriptionTopic();
		resp.getOutputStream().print(parser.encodeResourceToString(topic));
	}

	@GetMapping(PATIENT_TOPIC_NAME)
	protected void doGetPatientTest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		IParser parser = fhirContext.newJsonParser().setPrettyPrint(true);
		SubscriptionTopic topic = getPatientSubscriptionTopic();
		resp.getOutputStream().print(parser.encodeResourceToString(topic));
	}

	@GetMapping(GROUP_TOPIC_NAME)
	protected void doGetGroup(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		IParser parser = fhirContext.newJsonParser().setPrettyPrint(true);
		SubscriptionTopic topic = getGroupSubscriptionTopic();
		resp.getOutputStream().print(parser.encodeResourceToString(topic));
	}


	@GetMapping(DATA_QUALITY_ISSUES_TOPIC_NAME)
	protected void doGetDataQualityIssues(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		IParser parser = fhirContext.newJsonParser().setPrettyPrint(true);
		SubscriptionTopic topic = getDataQualityIssuesSubscriptionTopic();
		resp.getOutputStream().print(parser.encodeResourceToString(topic));
	}

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
			.setFilterParameter("_tag")
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
