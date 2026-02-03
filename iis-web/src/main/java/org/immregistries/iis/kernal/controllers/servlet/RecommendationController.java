package org.immregistries.iis.kernal.controllers.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r5.model.Identifier;
import org.immregistries.iis.kernal.controllers.IisRestParam;
import org.immregistries.iis.kernal.controllers.rest.PatientRestController;
import org.immregistries.iis.kernal.controllers.rest.RecommendationRestController;
import org.immregistries.iis.kernal.controllers.servlet.util.PatientServletUtil;
import org.immregistries.iis.kernal.controllers.servlet.util.UiUtil;
import org.immregistries.iis.kernal.mapping.IisFhirClientFactory;
import org.immregistries.iis.kernal.mapping.mappers.fields.BusinessIdentifierMapper;
import org.immregistries.iis.kernal.mapping.mappers.resources.PatientMapper;
import org.immregistries.iis.kernal.model.BusinessIdentifier;
import org.immregistries.iis.kernal.model.IisPatient;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;

import static org.immregistries.iis.kernal.controllers.servlet.PatientController.PARAM_PATIENT_REPORTED_ID;
import static org.immregistries.iis.kernal.controllers.servlet.RecommendationController.RECOMMENDATION_BASE_PATH;

@RestController
@RequestMapping({ RECOMMENDATION_BASE_PATH, TenantController.TENANT_PATH + RECOMMENDATION_BASE_PATH })
public class RecommendationController {
	public static final String RECOMMENDATION_BASE_PATH = "/recommendation";
	public static final String PARAM_RECOMMENDATION_ID = "recommendationId";
	public static final String PARAM_RECOMMENDATION_IDENTIFIER = "recommendationIdentifier";
	public static final String PARAM_RECOMMENDATION_RESOURCE = "recommendationResource";
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private IisFhirClientFactory iisFhirClientFactory;

	@Autowired
	private FhirContext fhirContext;
	@Autowired
	private PatientMapper patientMapper;
	@Autowired
	private RecommendationRestController recommendationRestController;
	@Autowired
	private PatientRestController patientRestController;
	@Autowired
	private BusinessIdentifierMapper businessIdentifierMapper;
	@Autowired
	private UiUtil uiUtil;
	@Autowired
	private PatientServletUtil patientServletUtil;

	/**
	 * Used to add a random generated component to recommendation
	 *
	 * @param req  request
	 * @param resp response
	 * @throws ServletException Servlet Exception
	 * @throws IOException      print output stream exception
	 */
	@PostMapping
	protected void doPost(HttpServletRequest req, HttpServletResponse resp
	// , @PathVariable(name = TenantController.PATH_VARIABLE_TENANT_NAME, required =
	// false) String tenantName dealt with in filter
	)
			throws ServletException, IOException {
		Tenant tenant = uiUtil.getTenantRedirectIfNone(req, resp);
		recommendationRestController.addRandomRecommendation(tenant, req);
		doGet(req, resp);
	}

	/**
	 * Used to manually edit the Recommendation resource
	 *
	 * @param req  request
	 * @param resp response
	 */
	@PutMapping
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		uiUtil.getTenantRedirectIfNone(req, resp);
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		try {

			if (req.getParameter(PARAM_RECOMMENDATION_RESOURCE) != null) {
				Tenant tenant = uiUtil.getTenantRedirectIfNone(req, resp);
				recommendationRestController.updateRecommendation(tenant,
						req.getParameter(PARAM_RECOMMENDATION_RESOURCE),
						req.getParameter(IisRestParam.RECOMMENDATION_ID),
						req.getParameter(IisRestParam.RECOMMENDATION_IDENTIFIER), req);
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
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Tenant tenant = uiUtil.getTenantRedirectIfNone(req, resp);
		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		uiUtil.doHeader(out, "Recommendations", tenant);

		try {
			IGenericClient fhirClient = iisFhirClientFactory.getOrCreateGenericClient(req);

			IAnyResource recommendationResource = recommendationRestController.getRecommendation(
					req.getParameter(PARAM_RECOMMENDATION_ID), req.getParameter(PARAM_RECOMMENDATION_IDENTIFIER),
					tenant, req);
			IDomainResource patientResource = null;
			if (recommendationResource != null) {
				String patientReference = getPatientIdFromRecommendation(recommendationResource,
						fhirClient.getFhirContext());
				patientResource = (IDomainResource) fhirClient.read().resource("Patient")
						.withId(patientReference)
						.execute();
			} else {
				String patientId = req.getParameter(PARAM_PATIENT_REPORTED_ID);
				patientResource = (IDomainResource) patientRestController.getPatientFhir(patientId, tenant, req);
			}
			if (patientResource == null) {
				out.println("No patient or recommendation found with request parameters.");
			}
			IisPatient patientMaster = patientMapper.localObject(patientResource);
			BusinessIdentifier identifier = patientMaster.getMainBusinessIdentifier();
			if (StringUtils.isBlank(identifier.getValue())) {
				identifier = patientMaster.getFirstBusinessIdentifier();
			}

			if (patientResource != null) {
				out.println("<h2>Immunization recommendations of "
						+ patientMaster.getLegalNameOrFirst().asSingleString() + "</h2>");
				if (recommendationResource == null) {
					IBaseBundle baseBundle = patientRestController
							.getPatientRecommendationBundle(patientResource.getIdElement().getIdPart(), tenant, req);
					if (fhirContext.getVersion().equals(FhirVersionEnum.R5)) {
						org.hl7.fhir.r5.model.Bundle recommendationBundle = (org.hl7.fhir.r5.model.Bundle) baseBundle;
						if (recommendationBundle.getEntry().size() > 0) {
							recommendationResource = (IDomainResource) recommendationBundle
									.getEntryFirstRep()
									.getResource();
						}
					} else if (fhirContext.getVersion().equals(FhirVersionEnum.R4)) {
						org.hl7.fhir.r4.model.Bundle recommendationBundle = (org.hl7.fhir.r4.model.Bundle) baseBundle;
						if (recommendationBundle.getEntry().size() > 0) {
							recommendationResource = (IDomainResource) recommendationBundle
									.getEntryFirstRep()
									.getResource();
						}
					}
				}

				printRecommendation(out, recommendationResource, patientResource);
				if (recommendationResource != null
						&& fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
					org.hl7.fhir.r5.model.Bundle subcriptionBundle = fhirClient.search()
							.forResource(org.hl7.fhir.r5.model.Subscription.class)
							.returnBundle(org.hl7.fhir.r5.model.Bundle.class).execute();
					IParser parser = iisFhirClientFactory.getFhirContext()
							.newJsonParser().setPrettyPrint(true).setSummaryMode(false).setSuppressNarratives(true);

					out.println("<div class=\"w3-container\">");
					out.println("<h3>Manually edit</h3>");
					out.println("<form action=\"recommendation\" method=\"POST\">");
					out.println("  <input type=\"hidden\" name=\"_method\" value=\"put\" />");
					out.println("	<input type=\"hidden\" name=\"" + PARAM_PATIENT_REPORTED_ID + "\" value=\""
							+ new org.hl7.fhir.r5.model.IdType(patientResource.getId()).getIdPart() + "\"/>");
					out.println("	<input type=\"hidden\" name=\"" + PARAM_RECOMMENDATION_ID + "\" value=\""
							+ new org.hl7.fhir.r5.model.IdType(recommendationResource.getId()).getIdPart() + "\"/>");
					out.println("	<textarea class=\"w3-input w3-border\" name=\"" + PARAM_RECOMMENDATION_RESOURCE
							+ "\" rows=\"11\" cols=\"160\">" +
							parser.encodeResourceToString(recommendationResource) +
							"</textarea>");
					out.println(
							"	<input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"submit\" value=\"Edit resource\"/>");
					out.println("</form>");
					out.println("</div>");

					/*
					 * Temporary change to send through subscription
					 */
					org.hl7.fhir.r5.model.ImmunizationRecommendation immunizationRecommendation = (org.hl7.fhir.r5.model.ImmunizationRecommendation) recommendationResource;
					immunizationRecommendation
							.setPatient(new org.hl7.fhir.r5.model.Reference()
									.setIdentifier((Identifier) businessIdentifierMapper.fhirObject(identifier)));
					patientServletUtil.printSubscriptions(out, parser, subcriptionBundle, immunizationRecommendation);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		uiUtil.doFooter(out);
		out.flush();
		out.close();
	}

	private String getPatientIdFromRecommendation(IAnyResource recommendationResource, FhirContext fhirContext) {
		String patientReference = "";
		if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
			patientReference = ((org.hl7.fhir.r5.model.ImmunizationRecommendation) recommendationResource)
					.getPatient().getReference();
		} else if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R4)) {
			patientReference = ((org.hl7.fhir.r4.model.ImmunizationRecommendation) recommendationResource)
					.getPatient().getReference();
		}
		return patientReference;
	}

	public void printRecommendation(PrintWriter out, IAnyResource recommendation, IDomainResource patient) {
		printRecommendation(out, recommendation, patient, fhirContext);
	}

	public void printRecommendation(PrintWriter out, IAnyResource recommendation, IDomainResource patient,
			FhirContext fhirContext) {
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

		} else {
			out.println("<div class=\"w3-panel w3-yellow\"><p>No Recommendation Found</p></div>");
			out.println("<form action=\"recommendation\" method=\"POST\">");
			out.println("	<input type=\"hidden\" name=\"" + PARAM_PATIENT_REPORTED_ID + "\" value=\""
					+ new org.hl7.fhir.r5.model.IdType(patient.getId()).getIdPart() + "\"/>");
			out.println(
					"	<input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"submit\" value=\"Generate new recommendation\"/>");
			out.println("</form>");
		}
		if (recommendation != null) {
			if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
				printRecommendationLineR5(out, (org.hl7.fhir.r5.model.ImmunizationRecommendation) recommendation);
			} else if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R4)) {
				printRecommendationLineR4(out, (org.hl7.fhir.r4.model.ImmunizationRecommendation) recommendation);
			}
		}

		out.println("</tbody>");
		out.println("</table>");

		out.println("<form action=\"recommendation\" method=\"POST\">");
		out.println("	<input type=\"hidden\" name=\"" + PARAM_PATIENT_REPORTED_ID + "\" value=\""
				+ new org.hl7.fhir.r5.model.IdType(patient.getId()).getIdPart() + "\"/>");
		if (recommendation != null) {
			out.println("	<input type=\"hidden\" name=\"" + PARAM_RECOMMENDATION_ID + "\" value=\""
					+ new org.hl7.fhir.r5.model.IdType(recommendation.getId()).getIdPart() + "\"/>");
		}
		out.println(
				"	<input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"submit\" value=\"Add recommendation component\"/>");
		out.println("</form>");
		out.println("</div>");
	}

	public void printRecommendationLineR5(PrintWriter out,
			org.hl7.fhir.r5.model.ImmunizationRecommendation recommendation) {
		int count = 0;
		for (org.hl7.fhir.r5.model.ImmunizationRecommendation.ImmunizationRecommendationRecommendationComponent component : recommendation
				.getRecommendation()) {
			count++;
			if (count > 100) {
				break;
			}
			String link = "recommendation?" + PARAM_RECOMMENDATION_ID + "="
					+ new org.hl7.fhir.r5.model.IdType(recommendation.getId()).getIdPart();
			out.println("<tr>");
			out.println("    <td><a href=\"" + link + "\">"
					+ component.getVaccineCodeFirstRep().getCodingFirstRep().getCode() + "</a></td>");
			out.println("    <td><a href=\"" + link + "\">" + component.getDateCriterionFirstRep().getValue()
					+ "</a></td>");
			out.println("    <td><a href=\"" + link + "\">"
					+ component.getDateCriterionFirstRep().getCode().getCodingFirstRep().getDisplay() + "</a></td>");
			out.println("</tr>");
		}
	}

	public void printRecommendationLineR4(PrintWriter out,
			org.hl7.fhir.r4.model.ImmunizationRecommendation recommendation) {
		int count = 0;

		for (org.hl7.fhir.r4.model.ImmunizationRecommendation.ImmunizationRecommendationRecommendationComponent component : recommendation
				.getRecommendation()) {
			count++;
			if (count > 100) {
				break;
			}
			String link = "recommendation?" + PARAM_RECOMMENDATION_ID + "="
					+ new org.hl7.fhir.r4.model.IdType(recommendation.getId()).getIdPart();
			out.println("<tr>");
			out.println("    <td><a href=\"" + link + "\">"
					+ component.getVaccineCodeFirstRep().getCodingFirstRep().getCode() + "</a></td>");
			out.println("    <td><a href=\"" + link + "\">" + component.getDateCriterionFirstRep().getValue()
					+ "</a></td>");
			out.println("    <td><a href=\"" + link + "\">"
					+ component.getDateCriterionFirstRep().getCode().getCodingFirstRep().getDisplay() + "</a></td>");
			out.println("</tr>");
		}
	}

}
