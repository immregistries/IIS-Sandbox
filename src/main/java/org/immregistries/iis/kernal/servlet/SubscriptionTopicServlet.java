package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.exceptions.ResourceGoneException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.hapi.fhirpath.FhirPathR5;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

//@Component
public class SubscriptionTopicServlet extends HttpServlet {
	@Autowired
	FhirContext fhirContext;


	Logger logger = LoggerFactory.getLogger(SubscriptionTopicServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		IParser parser = fhirContext.newJsonParser().setPrettyPrint(true);
		SubscriptionTopic topic = getSubscriptionTopic();

		resp.getOutputStream().print(parser.encodeResourceToString(topic));
	}

	static public SubscriptionTopic getSubscriptionTopic() {
//		SubscriptionTopic.SubscriptionTopicEventTriggerComponent eventTrigger =
//			new SubscriptionTopic.SubscriptionTopicEventTriggerComponent().setEvent( new CodeableConcept()
//				// https://terminology.hl7.org/3.1.0/ValueSet-v2-0003.html
//				// Codes for CRUD on patients with HL7v2
//				.addCoding(new Coding().setSystem("http://terminology.hl7.org/ValueSet/v2-0003").setCode("A04"))
//				.addCoding(new Coding().setSystem("http://terminology.hl7.org/ValueSet/v2-0003").setCode("A28"))
//				.addCoding(new Coding().setSystem("http://terminology.hl7.org/ValueSet/v2-0003").setCode("A31"))
//				.addCoding(new Coding().setSystem("IIS-Sandbox").setCode("Manual Trigger"))
//				// TODO add MQE codes ?
//			).setResource("OperationOutcome?");


		SubscriptionTopic topic  = new SubscriptionTopic()
			.setDescription("Testing communication between EHR and IIS and operation outcome")
			.setUrl(ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString() +"/SubscriptionTopic") //TODO change if relocated
			.setStatus(Enumerations.PublicationStatus.DRAFT)
			.setExperimental(true).setPublisher("Aira/Nist")
			.setTitle("Health equity data quality requests within Immunization systems");
		topic.setId("sandbox");
		topic.addResourceTrigger(new SubscriptionTopic.SubscriptionTopicResourceTriggerComponent()
			.setResource("OperationOutcome")
			.setQueryCriteria(new SubscriptionTopic.SubscriptionTopicResourceTriggerQueryCriteriaComponent()
				.setCurrent("OperationOutcome?")
			)
		);
		topic.addResourceTrigger(new SubscriptionTopic.SubscriptionTopicResourceTriggerComponent()
			.setResource("Patient")
			.setQueryCriteria(new SubscriptionTopic.SubscriptionTopicResourceTriggerQueryCriteriaComponent()
				.setCurrent("Patient?")
			)
		);
		topic.addResourceTrigger(new SubscriptionTopic.SubscriptionTopicResourceTriggerComponent()
			.setResource("Group")
			.setQueryCriteria(new SubscriptionTopic.SubscriptionTopicResourceTriggerQueryCriteriaComponent()
				.setCurrent("Group?")
			)
		);
//		topic.addResourceTrigger(new SubscriptionTopic.SubscriptionTopicResourceTriggerComponent()
//			.setResource("Immunization")
//			.setQueryCriteria(new SubscriptionTopic.SubscriptionTopicResourceTriggerQueryCriteriaComponent()
//				.setCurrent("Immunization?")
//			)
//		);
		topic.addCanFilterBy(new SubscriptionTopic.SubscriptionTopicCanFilterByComponent()
			.setDescription("test")
			.setResource("OperationOutcome")
			.setFilterParameter("_tag")
			.addModifier(Enumerations.SearchModifierCode.EXACT)
		);
		topic.addNotificationShape().setResource("OperationOutcome");
		return topic;
	}
}
