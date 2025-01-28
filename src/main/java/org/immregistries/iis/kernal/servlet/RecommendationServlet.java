package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.logic.IImmunizationRecommendationService;
import org.immregistries.iis.kernal.mapping.internalClient.FhirRequester;
import org.immregistries.iis.kernal.mapping.internalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.model.PatientIdentifier;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

@RestController
@RequestMapping({"/recommendation", "/patient/{patientId}/recommendation", "/tenant/{tenantId}/patient/{patientId}/recommendation"})
public class RecommendationServlet extends PatientServlet {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public static final String PARAM_RECOMMENDATION_ID = "recommendationId";
	public static final String PARAM_RECOMMENDATION_IDENTIFIER = "recommendationIdentifier";
	public static final String PARAM_RECOMMENDATION_RESOURCE = "recommendationResource";

	@Autowired
	private IImmunizationRecommendationService immunizationRecommendationService;

	@Autowired
	private RepositoryClientFactory repositoryClientFactory;
	@Autowired
	private FhirRequester fhirRequester;
	@Autowired
	private FhirContext fhirContext;

	public static String linkUrl(String facilityId, String patientId) {
		return "/tenant/" + facilityId + "/patient/" + patientId + "/recommendation";
	}

	/**
	 * Used to add a random generated component to recommendation
	 *
	 * @param req request
	 * @param resp response
	 * @throws ServletException Servlet Exception
	 * @throws IOException print output stream exception
	 */
	@PostMapping
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException { //TODO add support to add new Recommendation
		Tenant tenant = ServletHelper.getTenant();
		if (tenant == null) {
			throw new AuthenticationCredentialsNotFoundException("");
		}
		IGenericClient fhirClient = repositoryClientFactory.newGenericClient(req);
		IDomainResource patient = fetchPatientFromParameter(req, fhirClient);

		if (patient != null) {

			IBaseBundle baseBundle = fhirClient.search().forResource("ImmunizationRecommendation")
				.where(org.hl7.fhir.r5.model.ImmunizationRecommendation.PATIENT.hasId(new org.hl7.fhir.r5.model.IdType(patient.getId()).getIdPart())).execute();
			if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
				org.hl7.fhir.r5.model.Bundle recommendationBundle = (org.hl7.fhir.r5.model.Bundle) baseBundle;
				if (recommendationBundle.hasEntry()) {
					org.hl7.fhir.r5.model.ImmunizationRecommendation recommendation = (org.hl7.fhir.r5.model.ImmunizationRecommendation) recommendationBundle.getEntryFirstRep().getResource();
					recommendation = (org.hl7.fhir.r5.model.ImmunizationRecommendation) immunizationRecommendationService.addGeneratedRecommendation(recommendation);
					fhirClient.update().resource(recommendation).withId(recommendation.getId()).execute();
				} else {
					fhirClient.create().resource(immunizationRecommendationService.generate(tenant, new Date(), patient)).execute();
				}
			} else if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R4)) {
				org.hl7.fhir.r4.model.Bundle recommendationBundle = (org.hl7.fhir.r4.model.Bundle) baseBundle;
				if (recommendationBundle.hasEntry()) {
					org.hl7.fhir.r4.model.ImmunizationRecommendation recommendation = (org.hl7.fhir.r4.model.ImmunizationRecommendation) recommendationBundle.getEntryFirstRep().getResource();
					recommendation = (org.hl7.fhir.r4.model.ImmunizationRecommendation) immunizationRecommendationService.addGeneratedRecommendation(recommendation);
					fhirClient.update().resource(recommendation).withId(recommendation.getId()).execute();
				} else {
					fhirClient.create().resource(immunizationRecommendationService.generate(tenant, new Date(), patient)).execute();
				}
			}

		}
		doGet(req, resp);
	}

	/**
	 * Used to manually edit the Recommendation resource
	 *
	 * @param req request
	 * @param resp response
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

				if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
					org.hl7.fhir.r5.model.ImmunizationRecommendation newRecommendation = parser.parseResource(org.hl7.fhir.r5.model.ImmunizationRecommendation.class, req.getParameter(PARAM_RECOMMENDATION_RESOURCE));
					org.hl7.fhir.r5.model.ImmunizationRecommendation old = (org.hl7.fhir.r5.model.ImmunizationRecommendation) getRecommendation(req, fhirClient);
					newRecommendation.setId(old.getIdElement().getIdPart());
					fhirClient.update().resource(newRecommendation).execute();
				} else if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R4)) {
					org.hl7.fhir.r4.model.ImmunizationRecommendation newRecommendation = parser.parseResource(org.hl7.fhir.r4.model.ImmunizationRecommendation.class, req.getParameter(PARAM_RECOMMENDATION_RESOURCE));
					org.hl7.fhir.r4.model.ImmunizationRecommendation old = (org.hl7.fhir.r4.model.ImmunizationRecommendation) getRecommendation(req, fhirClient);
					newRecommendation.setId(old.getIdElement().getIdPart());
					fhirClient.update().resource(newRecommendation).execute();
				}
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

	/**
	 * UI page for recommendations
	 *
	 * @param req  request
	 * @param resp response
	 * @throws ServletException servlet exception
	 * @throws IOException      OutputStream exception
	 */
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

			IDomainResource recommendationResource = getRecommendation(req, fhirClient);
			IDomainResource patientResource = null;
			if (recommendationResource != null) {
				if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
					patientResource = (IDomainResource) fhirClient.read().resource("Patient")
						.withId(((org.hl7.fhir.r5.model.ImmunizationRecommendation) recommendationResource).getPatient().getReference()).execute();
				} else if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R4)) {
					patientResource = (IDomainResource) fhirClient.read().resource("Patient")
						.withId(((org.hl7.fhir.r4.model.ImmunizationRecommendation) recommendationResource).getPatient().getReference()).execute();
				}
			} else {
				patientResource = fetchPatientFromParameter(req, fhirClient);
			}
			PatientMaster patientMaster = patientMapper.localObject(patientResource);
			PatientIdentifier identifier = patientMaster.getMainPatientIdentifier();
			if (StringUtils.isBlank(identifier.getValue())) {
				identifier = patientMaster.getFirstPatientIdentifier();
			}

			if (patientResource != null) {
				out.println("<h2>Immunization recommendations of " + patientMaster.getLegalNameOrFirst().asSingleString() + "</h2>");
				if (recommendationResource == null) {
					IBaseBundle baseBundle = fhirClient.search()
						.forResource("ImmunizationRecommendation")
						.where(org.hl7.fhir.r5.model.ImmunizationRecommendation.PATIENT
							.hasChainedProperty(org.hl7.fhir.r5.model.Patient.IDENTIFIER.exactly()
								.systemAndCode(identifier.getSystem(), identifier.getValue()))).execute();
					if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
						org.hl7.fhir.r5.model.Bundle recommendationBundle = (org.hl7.fhir.r5.model.Bundle) baseBundle;
						if (recommendationBundle.hasEntry()) {
							recommendationResource = (IDomainResource) recommendationBundle.getEntryFirstRep().getResource();
						}
					} else if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R4)) {
						org.hl7.fhir.r4.model.Bundle recommendationBundle = (org.hl7.fhir.r4.model.Bundle) baseBundle;
						if (recommendationBundle.hasEntry()) {
							recommendationResource = (IDomainResource) recommendationBundle.getEntryFirstRep().getResource();
						}
					}
				}
				printRecommendation(out, recommendationResource, patientResource);
				if (recommendationResource != null && fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
					org.hl7.fhir.r5.model.Bundle subcriptionBundle = fhirClient.search().forResource(org.hl7.fhir.r5.model.Subscription.class).returnBundle(org.hl7.fhir.r5.model.Bundle.class).execute();
					IParser parser = repositoryClientFactory.getFhirContext()
						.newJsonParser().setPrettyPrint(true).setSummaryMode(false).setSuppressNarratives(true);

					out.println("<div class=\"w3-container\">");
					out.println("<h3>Manually edit</h3>");
					out.println("<form action=\"recommendation\" method=\"POST\">");
					out.println("  <input type=\"hidden\" name=\"_method\" value=\"put\" />");
					out.println("	<input type=\"hidden\" name=\"" + PARAM_PATIENT_REPORTED_ID + "\" value=\"" + new org.hl7.fhir.r5.model.IdType(patientResource.getId()).getIdPart() + "\"/>");
					out.println("	<input type=\"hidden\" name=\"" + PARAM_RECOMMENDATION_ID + "\" value=\"" + new org.hl7.fhir.r5.model.IdType(recommendationResource.getId()).getIdPart() + "\"/>");
					out.println("	<textarea class=\"w3-input w3-border\" name=\"" + PARAM_RECOMMENDATION_RESOURCE + "\" rows=\"11\" cols=\"160\">" +
						parser.encodeResourceToString(recommendationResource) +
						"</textarea>");
					out.println("	<input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"submit\" value=\"Edit resource\"/>");
					out.println("</form>");
					out.println("</div>");

					/*
					 * Temporary change to send through subscription
					 */
					org.hl7.fhir.r5.model.ImmunizationRecommendation immunizationRecommendation = (org.hl7.fhir.r5.model.ImmunizationRecommendation) recommendationResource;
					immunizationRecommendation.setPatient(new org.hl7.fhir.r5.model.Reference().setIdentifier(identifier.toR5())); // TODO filter to take always MRN ?
					printSubscriptions(out, parser, subcriptionBundle, immunizationRecommendation);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		HomeServlet.doFooter(out);
		out.flush();
		out.close();
	}

	/**
	 * Helping method to get Recommendation from server
	 *
	 * @param req        request
	 * @param fhirClient Fhir client
	 * @return ImmunizationRecommendation
	 */
	protected IDomainResource getRecommendation(HttpServletRequest req, IGenericClient fhirClient) {
		IDomainResource recommendation = null;
		if (req.getParameter(PARAM_RECOMMENDATION_ID) != null) {
			recommendation = (IDomainResource) fhirClient.read().resource("ImmunizationRecommendation").withId(req.getParameter(PARAM_RECOMMENDATION_ID)).execute();
		} else if (req.getParameter(PARAM_RECOMMENDATION_IDENTIFIER) != null) {
			if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
				org.hl7.fhir.r5.model.Bundle recommendationBundle = fhirClient.search().forResource("ImmunizationRecommendation").where(
					org.hl7.fhir.r5.model.Patient.IDENTIFIER.exactly().identifier(req.getParameter(PARAM_RECOMMENDATION_IDENTIFIER))).returnBundle(org.hl7.fhir.r5.model.Bundle.class).execute();
				if (recommendationBundle.hasEntry()) {
					recommendation = (org.hl7.fhir.r5.model.ImmunizationRecommendation) recommendationBundle.getEntryFirstRep().getResource();
				}
			} else if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R4)) {
				org.hl7.fhir.r4.model.Bundle recommendationBundle = fhirClient.search().forResource("ImmunizationRecommendation").where(
					org.hl7.fhir.r4.model.Patient.IDENTIFIER.exactly().identifier(req.getParameter(PARAM_RECOMMENDATION_IDENTIFIER))).returnBundle(org.hl7.fhir.r4.model.Bundle.class).execute();
				if (recommendationBundle.hasEntry()) {
					recommendation = (org.hl7.fhir.r4.model.ImmunizationRecommendation) recommendationBundle.getEntryFirstRep().getResource();
				}
			}
		}
		return recommendation;
	}

	public void printRecommendation(PrintWriter out, IDomainResource recommendation, IDomainResource patient) {
		if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
			printRecommendationR5(out, (org.hl7.fhir.r5.model.ImmunizationRecommendation) recommendation, (org.hl7.fhir.r5.model.Patient) patient);
		} else if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R4)) {
			printRecommendationR4(out, (org.hl7.fhir.r4.model.ImmunizationRecommendation) recommendation, (org.hl7.fhir.r4.model.Patient) patient);

		}

	}

	public static void printRecommendationR5(PrintWriter out, org.hl7.fhir.r5.model.ImmunizationRecommendation recommendation, org.hl7.fhir.r5.model.Patient patient) {
		out.println("<div class=\"w3-container\">");
		out.println("<h4>Recommendations</h4>");
		if (recommendation != null) {
			out.println("<table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
			out.println("  <tr class=\"w3-green\">");
			out.println("    <th>Code</th>");
			out.println("    <th>Date</th>");
			out.println("    <th>Date Criterion</th>");
			out.println("    <th></th>");
			out.println("  </tr>");
			out.println("<tbody>");
			int count = 0;
			for (org.hl7.fhir.r5.model.ImmunizationRecommendation.ImmunizationRecommendationRecommendationComponent component : recommendation.getRecommendation()) {
				count++;
				if (count > 100) {
					break;
				}
				String link = "recommendation?" + PARAM_RECOMMENDATION_ID + "="
					+ new org.hl7.fhir.r5.model.IdType(recommendation.getId()).getIdPart();
				out.println("<tr>");
				out.println("    <td><a href=\"" + link + "\">" + component.getVaccineCodeFirstRep().getCodingFirstRep().getCode() + "</a></td>");
				out.println("    <td><a href=\"" + link + "\">" + component.getDateCriterionFirstRep().getValue() + "</a></td>");
				out.println("    <td><a href=\"" + link + "\">" + component.getDateCriterionFirstRep().getCode().getCodingFirstRep().getDisplay() + "</a></td>");
				out.println("</tr>");
			}
			out.println("</tbody>");
			out.println("</table>");

			out.println("<form action=\"recommendation\" method=\"POST\">");
			out.println("	<input type=\"hidden\" name=\"" + PARAM_PATIENT_REPORTED_ID + "\" value=\"" + new org.hl7.fhir.r5.model.IdType(patient.getId()).getIdPart() + "\"/>");
			out.println("	<input type=\"hidden\" name=\"" + PARAM_RECOMMENDATION_ID + "\" value=\"" + new org.hl7.fhir.r5.model.IdType(recommendation.getId()).getIdPart() + "\"/>");
			out.println("	<input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"submit\" value=\"Add recommendation component\"/>");
			out.println("</form>");
		} else {
			out.println("<div class=\"w3-panel w3-yellow\"><p>No Recommendation Found</p></div>");
			out.println("<form action=\"recommendation\" method=\"POST\">");
			out.println("	<input type=\"hidden\" name=\"" + PARAM_PATIENT_REPORTED_ID + "\" value=\"" + new org.hl7.fhir.r5.model.IdType(patient.getId()).getIdPart() + "\"/>");
			out.println("	<input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"submit\" value=\"Generate new recommendation\"/>");
			out.println("</form>");
		}
		out.println("</div>");
	}

	// TODO Remove redundancy
	public static void printRecommendationR4(PrintWriter out, org.hl7.fhir.r4.model.ImmunizationRecommendation recommendation, org.hl7.fhir.r4.model.Patient patient) {
		out.println("<div class=\"w3-container\">");
		out.println("<h4>Recommendations</h4>");
		if (recommendation != null) {
			out.println("<table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
			out.println("  <tr class=\"w3-green\">");
			out.println("    <th>Code</th>");
			out.println("    <th>Date</th>");
			out.println("    <th>Date Criterion</th>");
			out.println("    <th></th>");
			out.println("  </tr>");
			out.println("<tbody>");
			int count = 0;
			for (org.hl7.fhir.r4.model.ImmunizationRecommendation.ImmunizationRecommendationRecommendationComponent component : recommendation.getRecommendation()) {
				count++;
				if (count > 100) {
					break;
				}
				String link = "recommendation?" + PARAM_RECOMMENDATION_ID + "="
					+ new org.hl7.fhir.r4.model.IdType(recommendation.getId()).getIdPart();
				out.println("<tr>");
				out.println("    <td><a href=\"" + link + "\">" + component.getVaccineCodeFirstRep().getCodingFirstRep().getCode() + "</a></td>");
				out.println("    <td><a href=\"" + link + "\">" + component.getDateCriterionFirstRep().getValue() + "</a></td>");
				out.println("    <td><a href=\"" + link + "\">" + component.getDateCriterionFirstRep().getCode().getCodingFirstRep().getDisplay() + "</a></td>");
				out.println("</tr>");
			}
			out.println("</tbody>");
			out.println("</table>");

			out.println("<form action=\"recommendation\" method=\"POST\">");
			out.println("	<input type=\"hidden\" name=\"" + PARAM_PATIENT_REPORTED_ID + "\" value=\"" + new org.hl7.fhir.r4.model.IdType(patient.getId()).getIdPart() + "\"/>");
			out.println("	<input type=\"hidden\" name=\"" + PARAM_RECOMMENDATION_ID + "\" value=\"" + new org.hl7.fhir.r4.model.IdType(recommendation.getId()).getIdPart() + "\"/>");
			out.println("	<input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"submit\" value=\"Add recommendation component\"/>");
			out.println("</form>");
		} else {
			out.println("<div class=\"w3-panel w3-yellow\"><p>No Recommendation Found</p></div>");
			out.println("<form action=\"recommendation\" method=\"POST\">");
			out.println("	<input type=\"hidden\" name=\"" + PARAM_PATIENT_REPORTED_ID + "\" value=\"" + new org.hl7.fhir.r4.model.IdType(patient.getId()).getIdPart() + "\"/>");
			out.println("	<input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"submit\" value=\"Generate new recommendation\"/>");
			out.println("</form>");
		}
		out.println("</div>");
	}
}
