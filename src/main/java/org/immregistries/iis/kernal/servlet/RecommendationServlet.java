package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.ImmunizationRecommendation;
import org.hl7.fhir.r5.model.Patient;
import org.hl7.fhir.r5.model.Subscription;
import org.immregistries.iis.kernal.logic.ImmunizationRecommendationService;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

public class RecommendationServlet extends PatientServlet {

	@Autowired
	ImmunizationRecommendationService immunizationRecommendationService;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		doGet(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		HttpSession session = req.getSession(true);
		OrgAccess orgAccess = (OrgAccess) session.getAttribute("orgAccess");
		if (orgAccess == null) {
			RequestDispatcher dispatcher = req.getRequestDispatcher("home");
			dispatcher.forward(req, resp);
			return;
		}

		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		HomeServlet.doHeader(out, session, "Recommendations");

		try {
			IGenericClient fhirClient = repositoryClientFactory.newGenericClient(session);

			Patient patient = null;
			if (req.getParameter(PARAM_PATIENT_REPORTED_ID) != null) {
				patient = fhirClient.read().resource(Patient.class).withId(req.getParameter(PARAM_PATIENT_REPORTED_ID)).execute();
			} else if (req.getParameter(PARAM_PATIENT_REPORTED_EXTERNAL_LINK) != null) {
				Bundle patientBundle = (Bundle) fhirRequester.searchRegularRecord(Patient.class,
					Patient.IDENTIFIER.exactly().identifier(req.getParameter(PARAM_PATIENT_REPORTED_EXTERNAL_LINK)));
				if (patientBundle.hasEntry()) {
					patient = (Patient) patientBundle.getEntryFirstRep().getResource();
				}
			}

			if (patient != null) {
				Bundle bundle = fhirClient.search().forResource(Subscription.class).returnBundle(Bundle.class).execute();

				IParser parser = repositoryClientFactory.getFhirContext()
					.newJsonParser().setPrettyPrint(true).setSummaryMode(true).setSuppressNarratives(true);
				ImmunizationRecommendation immunizationRecommendation = immunizationRecommendationService.generate(patient);
				printSubscriptions(out, parser, bundle, immunizationRecommendation);
			}

		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		HomeServlet.doFooter(out, session);
		out.flush();
		out.close();

	}
}
