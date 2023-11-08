package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r5.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/SubscriptionTopic")
public class SubscriptionTopicController {
	@Autowired
	FhirContext fhirContext;


	Logger logger = LoggerFactory.getLogger(SubscriptionTopicController.class);

	@GetMapping
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		IParser parser = fhirContext.newJsonParser().setPrettyPrint(true);
		SubscriptionTopic topic = getDataQualityIssuesSubscriptionTopic();
		resp.getOutputStream().print(parser.encodeResourceToString(topic));
	}

	@GetMapping("/Group")
	protected void doGet2(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		IParser parser = fhirContext.newJsonParser().setPrettyPrint(true);
		SubscriptionTopic topic = getGroupSubscriptionTopic();
		resp.getOutputStream().print(parser.encodeResourceToString(topic));
	}

	@GetMapping("/data-quality-issues")
	protected void doGetDataQualityIssues(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		IParser parser = fhirContext.newJsonParser().setPrettyPrint(true);
		SubscriptionTopic topic = getDataQualityIssuesSubscriptionTopic();
		resp.getOutputStream().print(parser.encodeResourceToString(topic));
	}

	static public SubscriptionTopic getDataQualityIssuesSubscriptionTopic() {
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


		SubscriptionTopic topic  = new SubscriptionTopic()
			.setDescription("Testing communication between EHR and IIS and operation outcome")
			.setUrl(ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString() +"/SubscriptionTopic/data-quality-issues")
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
		SubscriptionTopic topic  = new SubscriptionTopic()
			.setDescription("Testing communication between EHR and IIS and operation outcome")
			.setUrl(ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString() +"/SubscriptionTopic/Group")
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
}
