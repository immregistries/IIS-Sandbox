package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.fhir.annotations.OnR5Condition;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.logic.IImmunizationRecommendationService;
import org.immregistries.iis.kernal.model.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import static org.immregistries.iis.kernal.mapping.interfaces.PatientMapper.MRN_SYSTEM;

@RestController
@RequestMapping({"/recommendation", "/patient/{patientId}/recommendation", "/tenant/{tenantId}/patient/{patientId}/recommendation"})
@Conditional(OnR5Condition.class)
public class RecommendationServlet extends PatientServlet {

	public static final String PARAM_RECOMMENDATION_ID = "recommendationId";
	public static final String PARAM_RECOMMENDATION_IDENTIFIER = "recommendationIdentifier";
	public static final String PARAM_RECOMMENDATION_RESOURCE = "recommendationResource";

	@Autowired
	IImmunizationRecommendationService immunizationRecommendationService;

	public static String linkUrl(String facilityId, String patientId) {
		return "/tenant/" + facilityId + "/patient/" + patientId + "/recommendation";
	}

	/**
	 * USed to add a random generated component to recommendation
	 *
	 * @param req
	 * @param resp
	 * @throws ServletException
	 * @throws IOException
	 */
	@PostMapping
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException { //TODO add support to add new Reco
		Tenant tenant = ServletHelper.getTenant();
		if (tenant == null) {
			throw new AuthenticationCredentialsNotFoundException("");
		}
		IGenericClient fhirClient = repositoryClientFactory.newGenericClient(req);
		Patient patient = (Patient) getPatientFromParameter(req, fhirClient);

		if (patient != null) {
			Bundle recommendationBundle = fhirClient.search().forResource(ImmunizationRecommendation.class)
				.where(ImmunizationRecommendation.PATIENT.hasId(new IdType(patient.getId()).getIdPart())).returnBundle(Bundle.class).execute();
			if (recommendationBundle.hasEntry()) {
				ImmunizationRecommendation recommendation = (ImmunizationRecommendation) recommendationBundle.getEntryFirstRep().getResource();
				recommendation = (ImmunizationRecommendation) immunizationRecommendationService.addGeneratedRecommendation(recommendation);
				fhirClient.update().resource(recommendation).withId(recommendation.getId()).execute();
			} else {
				fhirClient.create().resource(immunizationRecommendationService.generate(tenant, new Date(), patient)).execute();
			}
		}
		doGet(req, resp);

	}

	/**
	 * Used to manually edit the Recommendation resource
	 *
	 * @param req
	 * @param resp
	 */
	@PutMapping
	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		Tenant tenant = ServletHelper.getTenant();
		if (tenant == null) {
			throw new AuthenticationCredentialsNotFoundException("");
		}
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		try {
			IParser parser = repositoryClientFactory.getFhirContext()
				.newJsonParser().setPrettyPrint(true).setSummaryMode(false).setSuppressNarratives(true);
			if (req.getParameter(PARAM_RECOMMENDATION_RESOURCE) != null) {
				IGenericClient fhirClient = repositoryClientFactory.newGenericClient(req);

				ImmunizationRecommendation newReco = parser.parseResource(ImmunizationRecommendation.class, req.getParameter(PARAM_RECOMMENDATION_RESOURCE));
				ImmunizationRecommendation old = getRecommendation(req, fhirClient);
				newReco.setId(old.getIdElement().getIdPart());
				fhirClient.update().resource(newReco).execute();
			}
		} catch (Exception exception) {
			exception.printStackTrace(out);
			throw exception;
		} finally {
			out.flush();
			out.close();
		}
		doGet(req, resp);
	}

	@GetMapping
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		Tenant tenant = ServletHelper.getTenant();
		if (tenant == null) {
			throw new AuthenticationCredentialsNotFoundException("");
		}

		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		HomeServlet.doHeader(out, "Recommendations");

		try {
			IGenericClient fhirClient = repositoryClientFactory.newGenericClient(req);

			ImmunizationRecommendation recommendation = getRecommendation(req, fhirClient);
			Patient patient = null;
			if (recommendation != null) {
				patient = fhirClient.read().resource(Patient.class).withId(recommendation.getPatient().getReference()).execute();
			} else {
				patient = (Patient) getPatientFromParameter(req, fhirClient);
			}

			if (patient != null) {
				out.println("<h2>Immunization recommendations of " + patient.getNameFirstRep().getNameAsSingleString() + "</h2>");
				if (recommendation == null) {
					Bundle recommendationBundle = fhirClient.search()
						.forResource(ImmunizationRecommendation.class)
						.where(ImmunizationRecommendation.PATIENT
							.hasChainedProperty(Patient.IDENTIFIER.exactly()
								.systemAndCode(patient.getIdentifierFirstRep().getSystem(), patient.getIdentifierFirstRep().getValue())))
						.returnBundle(Bundle.class).execute();
					if (recommendationBundle.hasEntry()) {
						recommendation = (ImmunizationRecommendation) recommendationBundle.getEntryFirstRep().getResource();
					}
				}
				printRecommendation(out, recommendation, patient);
				if (recommendation != null) {
					Bundle subcriptionBundle = fhirClient.search().forResource(Subscription.class).returnBundle(Bundle.class).execute();
					IParser parser = repositoryClientFactory.getFhirContext()
						.newJsonParser().setPrettyPrint(true).setSummaryMode(false).setSuppressNarratives(true);

					out.println("<div class=\"w3-container\">");
					out.println("<h3>Manually edit</h3>");
					out.println("<form action=\"recommendation\" method=\"POST\">");
					out.println("  <input type=\"hidden\" name=\"_method\" value=\"put\" />");
					out.println("	<input type=\"hidden\" name=\"" + PARAM_PATIENT_REPORTED_ID + "\" value=\"" + new IdType(patient.getId()).getIdPart() + "\"/>");
					out.println("	<input type=\"hidden\" name=\"" + PARAM_RECOMMENDATION_ID + "\" value=\"" + new IdType(recommendation.getId()).getIdPart() + "\"/>");
					out.println("	<textarea class=\"w3-input w3-border\" name=\"" + PARAM_RECOMMENDATION_RESOURCE + "\" rows=\"11\" cols=\"160\">" +
						parser.encodeResourceToString(recommendation) +
						"</textarea>");
					out.println("	<input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"submit\" value=\"Edit resource\"/>");
					out.println("</form>");
					out.println("</div>");


					/**
					 * Temporary change to send through subscription
					 */
					Identifier identifier = patient.getIdentifier().stream().filter((identifier1 -> identifier1.getSystem().equals(MRN_SYSTEM)))
						.findFirst().orElse(patient.getIdentifierFirstRep());
					recommendation.setPatient(new Reference().setIdentifier(identifier)); // TODO filter to take always MRN ?
					printSubscriptions(out, parser, subcriptionBundle, recommendation);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		HomeServlet.doFooter(out);
		out.flush();
		out.close();
	}

	protected ImmunizationRecommendation getRecommendation(HttpServletRequest req, IGenericClient fhirClient) {
		ImmunizationRecommendation recommendation = null;
		if (req.getParameter(PARAM_RECOMMENDATION_ID) != null) {
			recommendation = fhirClient.read().resource(ImmunizationRecommendation.class).withId(req.getParameter(PARAM_RECOMMENDATION_ID)).execute();
		} else if (req.getParameter(PARAM_RECOMMENDATION_IDENTIFIER) != null) {
			Bundle recommendationBundle = fhirClient.search().forResource(ImmunizationRecommendation.class).where(
				Patient.IDENTIFIER.exactly().identifier(req.getParameter(PARAM_RECOMMENDATION_IDENTIFIER))).returnBundle(Bundle.class).execute();
			if (recommendationBundle.hasEntry()) {
				recommendation = (ImmunizationRecommendation) recommendationBundle.getEntryFirstRep().getResource();
			}
		}
		return recommendation;
	}
}
