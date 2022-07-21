package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.subscription.match.registry.SubscriptionCanonicalizer;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.Enumerations;
import org.hl7.fhir.r5.model.SubscriptionTopic;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SubscriptionTopicServlet extends HttpServlet {
	@Autowired
	private IFhirSystemDao fhirSystemDao;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		IParser parser = fhirSystemDao.getContext().newJsonParser().setPrettyPrint(true);
		SubscriptionTopic.SubscriptionTopicResourceTriggerComponent patientTrigger = new SubscriptionTopic.SubscriptionTopicResourceTriggerComponent()
			.setResource("Patient");
		SubscriptionTopic.SubscriptionTopicResourceTriggerComponent operationOutcomeTrigger = new SubscriptionTopic.SubscriptionTopicResourceTriggerComponent()
			.setResource("OperationOutcome");
		SubscriptionTopic.SubscriptionTopicEventTriggerComponent eventTrigger =
			new SubscriptionTopic.SubscriptionTopicEventTriggerComponent().setEvent( new CodeableConcept()
				// https://terminology.hl7.org/3.1.0/ValueSet-v2-0003.html
				.addCoding(new Coding().setSystem("http://terminology.hl7.org/ValueSet/v2-0003").setCode("A04"))
				.addCoding(new Coding().setSystem("http://terminology.hl7.org/ValueSet/v2-0003").setCode("A28"))
				.addCoding(new Coding().setSystem("http://terminology.hl7.org/ValueSet/v2-0003").setCode("A31"))
			).setResource("Patient");

		SubscriptionTopic topic  = new SubscriptionTopic()
			.setDescription("Testing communication between EHR and IIS and operation outcome")
			.setUrl("https://florence.immregistries.org/iis-sandbox/fhir/SubscriptionTopic")
			.setUrl("http://localhost:8080/SubscriptionTopic")
			.setStatus(Enumerations.PublicationStatus.DRAFT)
			.setExperimental(true).setPublisher("Aira/Nist").setTitle("Health equity data quality requests within Immunization systems");

		topic.addResourceTrigger(patientTrigger);
		topic.addResourceTrigger(operationOutcomeTrigger);
		topic.addEventTrigger(eventTrigger);
		topic.addNotificationShape().setResource("OperationOutcome");
//		topic.addCanFilterBy()
//			.setDescription("test empty filter")
//			.setResource("Immunization")
//			.setFilterParameter()
//		SubscriptionCanonicalizer
		// TODO include topic in a provider
		resp.getOutputStream().print(parser.encodeResourceToString(topic));
	}
}
