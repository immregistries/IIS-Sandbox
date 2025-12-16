package org.immregistries.iis.kernal.controllers.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r5.model.Bundle;
import org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.fhir.shl.ShLinkPayload;
import org.immregistries.iis.kernal.logic.shlink.ShLinkUtilService;
import org.immregistries.iis.kernal.mapping.interfaces.PatientMapper;
import org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester;
import org.immregistries.iis.kernal.mapping.internalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.model.LoincIdentifier;
import org.immregistries.iis.kernal.model.ObservationReported;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.persisted.model.MessageReceived;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.persisted.repository.MessageReceivedRepository;
import org.immregistries.iis.kernal.controllers.rest.PatientRestController;
import org.immregistries.iis.kernal.controllers.rest.SubscriptionRestController;
import org.immregistries.iis.kernal.controllers.servlet.shlink.CLVRController;
import org.immregistries.iis.kernal.controllers.servlet.shlink.ShLinkController;
import org.immregistries.iis.kernal.controllers.servlet.util.UiUtil;
import org.immregistries.iis.kernal.controllers.servlet.util.UrlTenantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

import static org.immregistries.iis.kernal.controllers.servlet.PatientController.PATIENT_BASE_PATH;
import static org.immregistries.iis.kernal.controllers.servlet.util.PatientServletUtil.*;

@RestController
@RequestMapping({ PATIENT_BASE_PATH, TenantController.TENANT_PATH + PATIENT_BASE_PATH })
public class PatientController {
	public static final String PATIENT_PATH_KEY = "patient";
	public static final String PATIENT_BASE_PATH = "/" + PATIENT_PATH_KEY;

	public static final String PARAM_ACTION = "action";
	public static final String ACTION_SEARCH = "search";
	public static final String PARAM_PATIENT_NAME_LAST = "patientNameLast";
	public static final String PARAM_PATIENT_NAME_FIRST = "patientNameFirst";
	public static final String PARAM_PATIENT_REPORTED_EXTERNAL_LINK = "identifier";
	public static final String PARAM_PATIENT_REPORTED_ID = "patientId";

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private RepositoryClientFactory repositoryClientFactory;

	@Autowired
	private FhirContext fhirContext;
	@Autowired
	private PatientMapper patientMapper;

	@Autowired
	private ShLinkUtilService shLinkUtilService;

	@Autowired
	private PatientRestController patientRestController;

	@Autowired(required = false)
	private SubscriptionRestController subscriptionRestController;

	@Autowired
	private MessageReceivedRepository messageReceivedRepository;

	@PostMapping
	protected void doPost(HttpServletRequest req, HttpServletResponse resp
	// , @PathVariable(name = TenantController.PATH_VARIABLE_TENANT_NAME, required =
	// false) String tenantName
	)
			throws ServletException, IOException {
		doGet(req, resp);
	}

	@GetMapping
	protected void doGet(HttpServletRequest req, HttpServletResponse resp
	// , @PathVariable(name = TenantController.PATH_VARIABLE_TENANT_NAME, required =
	// false) String tenantName
	)
			throws ServletException, IOException {
		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		Tenant tenant = CurrentTenantUtil.getTenantRedirectIfNone(req, resp);
		try {
			UiUtil.doHeader(out, "IIS Sandbox - Patients", tenant);
			String patientId = req.getParameter(PARAM_PATIENT_REPORTED_ID);
			IBaseResource patientSelected = null;
			if (StringUtils.isNotBlank(patientId)) {
				patientSelected = patientRestController.getPatientFhir(patientId, tenant, req);
			}

			if (patientSelected == null) {
				searchOrPrintAll(req, out, tenant);
			} else {
				singlePatientInformationPrintAll(out, patientSelected, tenant, req);
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		UiUtil.doFooter(out);
		out.flush();
		out.close();
	}

	private void singlePatientInformationPrintAll(PrintWriter out, IBaseResource patientSelected,
			Tenant tenant, HttpServletRequest req) {
		PatientMaster patientMasterSelected = patientMapper.localObject(patientSelected);
		boolean isGolden = AbstractFhirRequester.isGoldenRecord(patientSelected);

		out.println("<h2>Patient : " + patientMasterSelected.getNameFirst() + " "
				+ patientMasterSelected.getNameMiddle() + " " + patientMasterSelected.getNameLast() + "</h2>");

		printPatient(out, patientMasterSelected);

		out.println("  <div class=\"w3-container\">");

		printPatientVaccinations(out, patientMasterSelected, isGolden, tenant, req);
		printPatientObservations(out, patientMasterSelected, isGolden, tenant, req);
		printRelatedPatients(out, patientMasterSelected, isGolden, tenant, req);

		out.println("  </div>");

		printPatientRecommendationsAndSubscriptions(out, patientSelected, patientMasterSelected, req, tenant);

		out.println("<div class=\"w3-container\">");
		printFhirShortcuts(out, patientSelected, patientMasterSelected, tenant);
		out.println("</div>");

		ShLinkPayload shLinkPayload = patientRestController.getShLinkPayload(patientMasterSelected.getPatientId(),
				tenant,
				req);

		out.println("<div class=\"w3-container\">");
		out.println("<h4>Smart Health link</h4>");
		printQrCodeAndDetails(out, tenant, patientMasterSelected, shLinkPayload);

		// out.println("<button onclick=\"copyHtmlToClipboard()\">Copy HTML</button>");
		// out.println("<script>");
		// out.println("function copyHtmlToClipboard() {");
		// out.println(" const content = ");
		// out.println(" navigator.clipboard.writeText(content)");
		// out.println(" .then(() => { console.log('HTML copied to clipboard'); })");
		// out.println(" .catch(err => { console.error('Failed to copy HTML: ', err);
		// });");
		// out.println("}");
		// out.println("</script>");

		out.println("</div>");

		out.println("<div class=\"w3-container\">");
		out.println("<h4>Messages Received</h4>");
		List<MessageReceived> messageReceivedList = messageReceivedRepository
				.findByPatientReportedId(patientMasterSelected.getPatientId());
		if (messageReceivedList.isEmpty()) {
			out.println("<div class=\"w3-panel w3-yellow\"><p>No Messages Received</p></div>");
		} else {
			for (MessageReceived messageReceived : messageReceivedList) {
				printMessageReceived(out, messageReceived);
			}
		}
		out.println("</div>");

		out.println("</div>");
	}

	private void printQrCodeAndDetails(PrintWriter out, Tenant tenant, PatientMaster patientMasterSelected,
			ShLinkPayload shLinkPayload) {
		out.println("<div class=\"w3-container\">");
		out.println("<img src=\""
			+ UrlTenantUtil.tenantifyPathWithContextPath(tenant,
						"/patient/qr?id=" + patientMasterSelected.getPatientId())
				+ "\"  alt=\"shlink\" width=\"200\">");
		out.println("<div><a href= \"" + shLinkPayload.getUrl() + "\">Manifest URL</a></div>");
		out.println("<h5>Qr Code Text Value</h5>");
		out.println("<textarea id =\"qrCode\" cols=\"30\" rows=\"2\" style=\"white-space: nowrap;  overflow: auto;\">");
		String qrCode = shLinkUtilService.qrCode(shLinkPayload);
		out.print(qrCode);
		out.println("</textarea>");
		out.println("</div>");
		out.println("</div>");
		out.println("<div><a href= \"" +
			UrlTenantUtil.tenantifyPathWithContextPath(tenant,
						ShLinkController.SHLINK_CONTROLLER_BASE_PATH + "?" + ShLinkController.PARAM_PATIENT_ID + "="
								+ patientMasterSelected.getPatientId())
				+
				"\">Generate a new Smart Health Link with IPS</a></div>");
		out.println("<div><a href= \"" +
			UrlTenantUtil.tenantifyPathWithContextPath(tenant,
						CLVRController.CLVR_PATH_SUFFIX + "/" + patientMasterSelected.getPatientId())
				+
				"?pdf=true\">Generate a EVC with IPS</a></div>");
	}

	private void printPatientRecommendationsAndSubscriptions(PrintWriter out, IBaseResource patientSelected,
			PatientMaster patientMasterSelected, HttpServletRequest req,
			Tenant tenant) {
		IParser parser = repositoryClientFactory.getFhirContext()
				.newJsonParser().setPrettyPrint(true).setSuppressNarratives(true);
		IBaseBundle recommendationBaseBundle = patientRestController
				.getPatientRecommendation(patientMasterSelected.getPatientId(), tenant, req);

		if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R4)) {
			org.hl7.fhir.r4.model.Bundle recommendationBundle = (org.hl7.fhir.r4.model.Bundle) recommendationBaseBundle;
			if (recommendationBundle.hasEntry()) {
				RecommendationController
						.printRecommendation(out,
								(org.hl7.fhir.r4.model.ImmunizationRecommendation) recommendationBundle
										.getEntryFirstRep().getResource(),
								(org.hl7.fhir.r4.model.Patient) patientSelected, fhirContext);
			} else {
				RecommendationController.printRecommendation(out, null, (org.hl7.fhir.r4.model.Patient) patientSelected,
						fhirContext);
			}
			// org.hl7.fhir.r4.model.Bundle subcriptionBundle =
			// fhirClient.search().forResource(org.hl7.fhir.r4.model.Subscription.class).returnBundle(org.hl7.fhir.r4.model.Bundle.class).execute();
			// printSubscriptions(out, parser, subcriptionBundle,
			// (org.hl7.fhir.r4.model.Resource) patientSelected);
			// TODO support for R4

		} else if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
			org.hl7.fhir.r5.model.Bundle recommendationBundle = (org.hl7.fhir.r5.model.Bundle) recommendationBaseBundle;
			if (recommendationBundle.hasEntry()) {
				RecommendationController.printRecommendation(out,
						(IDomainResource) recommendationBundle.getEntryFirstRep().getResource(),
						(IDomainResource) patientSelected, fhirContext);
			} else {
				RecommendationController.printRecommendation(out, null, (IDomainResource) patientSelected, fhirContext);
			}
			org.hl7.fhir.r5.model.Bundle subcriptionBundle = (Bundle) subscriptionRestController
					.getAllSubscriptions(req);
			printSubscriptions(out, parser, subcriptionBundle, (org.hl7.fhir.r5.model.Resource) patientSelected);
		}
	}

	private void printRelatedPatients(PrintWriter out, PatientMaster patientMasterSelected, boolean isGolden,
			Tenant tenant, HttpServletRequest req) {
		List<PatientMaster> relatedPatients = patientRestController
				.getPatientRelatedPatients(patientMasterSelected.getPatientId(), tenant, isGolden, req);
		out.println("<h4>Related Patient records</h4>");
		printPatientList(out, relatedPatients, false);
		UiUtil.printGoldenRecordExplanation(out, isGolden);
	}

	private void printPatientVaccinations(PrintWriter out, PatientMaster patientMasterSelected, boolean isGolden,
			Tenant tenant, HttpServletRequest req) {
		List<VaccinationMaster> vaccinationList = patientRestController
				.getPatientVaccination(patientMasterSelected.getPatientId(), tenant, isGolden, req);
		out.println("<h4>Vaccinations</h4>");
		VaccinationController.printVaccinationList(out, vaccinationList, null); // TODO test and change
	}

	private void printPatientObservations(PrintWriter out, PatientMaster patientMasterSelected, boolean isGolden,
			Tenant tenant, HttpServletRequest req) {
		List<ObservationReported> observationReportedList = patientRestController
				.getPatientObservation(patientMasterSelected.getPatientId(), tenant, isGolden, req);
		Set<String> suppressSet = LoincIdentifier.getSuppressIdentifierCodeSet();
		observationReportedList
				.removeIf(observationReported -> suppressSet.contains(observationReported.getIdentifierCode()));

		out.println("<h4>Patient Observations</h4>");
		printObservationList(out, observationReportedList);
	}

	private void searchOrPrintAll(HttpServletRequest req, PrintWriter out, Tenant tenant) {
		/**
		 * Extracting Parameters from Request in case of research
		 */
		String patientNameLast = StringUtils.defaultString(req.getParameter(PARAM_PATIENT_NAME_LAST));
		String patientNameFirst = StringUtils.defaultString(req.getParameter(PARAM_PATIENT_NAME_FIRST));
		String externalLink = StringUtils.defaultString(req.getParameter(PARAM_PATIENT_REPORTED_EXTERNAL_LINK));
		List<PatientMaster> patientMasterList = null;
		String action = req.getParameter(PARAM_ACTION);
		if (action != null) {
			if (action.equals(ACTION_SEARCH)) {
				patientMasterList = patientRestController.basicSearch(patientNameLast, patientNameFirst, externalLink,
						tenant, req);
			}
		}
		List<PatientMaster> patientMasterList1 = patientMasterList;
		out.println("<h2>Patients</h2>");
		out.println("<div class=\"w3-container w3-half w3-margin-top\">");
		out.println("    <h3>Search Patient Registry</h3>");
		out.println("    <form method=\"GET\" action=\"patient\" class=\"w3-container w3-card-4\">");
		out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_PATIENT_NAME_LAST + "\" value=\""
				+ patientNameLast + "\"/>");
		out.println("      <label>Last Name</label>");
		out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_PATIENT_NAME_FIRST + "\" value=\""
				+ patientNameFirst + "\"/>");
		out.println("      <label>First Name</label>");
		out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_PATIENT_REPORTED_EXTERNAL_LINK
				+ "\" value=\"" + externalLink + "\"/>");
		out.println("      <label>Medical Record Number</label><br/>");
		out.println("      <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\""
				+ PARAM_ACTION + "\" value=\"" + ACTION_SEARCH + "\"/>");
		out.println("    </form>");
		out.println("</div>");

		out.println("<div class=\"w3-container\">");

		boolean showingRecent = false;
		if (patientMasterList1 == null) {
			showingRecent = true;
			patientMasterList1 = patientRestController.getAllPatients(tenant, req);
		}

		printPatientList(out, patientMasterList1, showingRecent);
		out.println("  </div>");

		{
			out.println("<div class=\"w3-container\">");
			out.println("<h4>FHIR Api Shortcuts</h4>");
			String apiBaseUrl = RepositoryClientFactory.fhirServerBasePath(tenant);
			{
				String link = apiBaseUrl + "/Patient";
				out.println("<div>All FHIR Patient records: <a href=\"" + link + "\">" + link + "</a></div>");
			}
			{
				String link = apiBaseUrl + "/Patient" + "?_tag=GOLDEN_RECORD";
				out.println("<div>Patient golden records (records referenced by duplicates): <a href=\"" + link + "\">"
						+ link + "</a></div>");
			}
			out.println("</div>");
		}
	}

}
