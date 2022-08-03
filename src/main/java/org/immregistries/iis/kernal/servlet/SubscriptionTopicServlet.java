package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r5.hapi.fhirpath.FhirPathR5;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.Enumerations;
import org.hl7.fhir.r5.model.SubscriptionTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SubscriptionTopicServlet extends HttpServlet {
	@Autowired
	private IFhirSystemDao fhirSystemDao;

	Logger logger = LoggerFactory.getLogger(SubscriptionTopicServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		IParser parser = fhirSystemDao.getContext().newJsonParser().setPrettyPrint(true);
		SubscriptionTopic.SubscriptionTopicResourceTriggerComponent patientTrigger = new SubscriptionTopic.SubscriptionTopicResourceTriggerComponent()
			.setResource("Patient")
			.addSupportedInteraction(SubscriptionTopic.InteractionTrigger.CREATE)
			.addSupportedInteraction(SubscriptionTopic.InteractionTrigger.UPDATE)
			.setQueryCriteria(new SubscriptionTopic.SubscriptionTopicResourceTriggerQueryCriteriaComponent()
				.setResultForCreate(SubscriptionTopic.CriteriaNotExistsBehavior.TESTPASSES)
				.setResultForDelete(SubscriptionTopic.CriteriaNotExistsBehavior.TESTPASSES)
				.setCurrent("Patient?_id=1")
			).setFhirPathCriteria("Patient?_id=1");
		SubscriptionTopic.SubscriptionTopicResourceTriggerComponent operationOutcomeTrigger =
			new SubscriptionTopic.SubscriptionTopicResourceTriggerComponent()
				.setResource("OperationOutcome")
				.addSupportedInteraction(SubscriptionTopic.InteractionTrigger.CREATE)
				.addSupportedInteraction(SubscriptionTopic.InteractionTrigger.DELETE)

				.setQueryCriteria(new SubscriptionTopic.SubscriptionTopicResourceTriggerQueryCriteriaComponent()
					.setResultForCreate(SubscriptionTopic.CriteriaNotExistsBehavior.TESTPASSES)
					.setResultForDelete(SubscriptionTopic.CriteriaNotExistsBehavior.TESTPASSES)
					.setCurrent("Operation?issue.severity=error")
				);
		SubscriptionTopic.SubscriptionTopicEventTriggerComponent eventTrigger =
			new SubscriptionTopic.SubscriptionTopicEventTriggerComponent().setEvent( new CodeableConcept()
				// https://terminology.hl7.org/3.1.0/ValueSet-v2-0003.html
				// Codes for CRUD on patients with HL7v2
				.addCoding(new Coding().setSystem("http://terminology.hl7.org/ValueSet/v2-0003").setCode("A04"))
				.addCoding(new Coding().setSystem("http://terminology.hl7.org/ValueSet/v2-0003").setCode("A28"))
				.addCoding(new Coding().setSystem("http://terminology.hl7.org/ValueSet/v2-0003").setCode("A31"))
				.addCoding(new Coding().setSystem("IIS-Sandbox").setCode("Manual Trigger"))
				// TODO add MQE codes ?
			).setResource("Patient");

		SubscriptionTopic topic  = new SubscriptionTopic()
			.setDescription("Testing communication between EHR and IIS and operation outcome")
//			.setUrl("https://florence.immregistries.org/iis-sandbox/fhir/SubscriptionTopic")
//			.setUrl("http://localhost:8080/SubscriptionTopic")
			.setUrl(req.getRequestURI())
			.setStatus(Enumerations.PublicationStatus.DRAFT)
			.setExperimental(true).setPublisher("Aira/Nist")
			.setTitle("Health equity data quality requests within Immunization systems");

		topic.setId("sandbox");
		topic.addResourceTrigger(patientTrigger);
		topic.addResourceTrigger(operationOutcomeTrigger);
//		topic.addEventTrigger(eventTrigger);
		topic.addNotificationShape().setResource("OperationOutcome");
		// TODO include topic in a provider or in fhir server
		resp.getOutputStream().print(parser.encodeResourceToString(topic));
	}
}
