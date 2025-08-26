package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.fhir.shl.ShLinkPayload;
import org.immregistries.iis.kernal.logic.shlink.ShLinkUtilService;
import org.immregistries.iis.kernal.mapping.interfaces.PatientMapper;
import org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester;
import org.immregistries.iis.kernal.mapping.internalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.model.LoincIdentifier;
import org.immregistries.iis.kernal.model.ObservationReported;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.model.persisted.MessageReceived;
import org.immregistries.iis.kernal.model.persisted.Tenant;
import org.immregistries.iis.kernal.servlet.shlink.PatientShLinkController;
import org.immregistries.iis.kernal.servlet.shlink.ShLinkController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import static org.immregistries.iis.kernal.logic.shlink.ShLinkUtilService.SHLINK_PREFIX;
import static org.immregistries.iis.kernal.servlet.PatientController.PATIENT_BASE_PATH;
import static org.immregistries.iis.kernal.servlet.PatientServletUtil.*;

@RestController
@RequestMapping({PATIENT_BASE_PATH, TenantController.TENANT_PATH + PATIENT_BASE_PATH})
public class PatientController {
	public static final String PATIENT_PATH_KEY = "patient";
	public static final String PATIENT_BASE_PATH = "/" + PATIENT_PATH_KEY;

	public static final String PARAM_ACTION = "action";
	public static final String ACTION_SEARCH = "search";
	public static final String PARAM_PATIENT_NAME_LAST = "patientNameLast";
	public static final String PARAM_PATIENT_NAME_FIRST = "patientNameFirst";
	public static final String PARAM_PATIENT_REPORTED_EXTERNAL_LINK = "identifier";
	public static final String PARAM_PATIENT_REPORTED_ID = "id";


	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private RepositoryClientFactory repositoryClientFactory;
	@Autowired
	private AbstractFhirRequester fhirRequester;
	@Autowired
	private FhirContext fhirContext;
	@Autowired
	private PatientMapper patientMapper;

	@Autowired
	private ShLinkUtilService shLinkUtilService;

	@PostMapping
	protected void doPost(HttpServletRequest req, HttpServletResponse resp
//		, @PathVariable(name = TenantController.PATH_VARIABLE_TENANT_NAME, required = false) String tenantName
	)
		throws ServletException, IOException {
		doGet(req, resp);
	}

	@GetMapping
	protected void doGet(HttpServletRequest req, HttpServletResponse resp
//		, @PathVariable(name = TenantController.PATH_VARIABLE_TENANT_NAME, required = false) String tenantName
	)
		throws ServletException, IOException {
		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		try (Session dataSession = ServletHelper.getDataSession()) {
			Tenant tenant = ServletHelper.getTenantRedirectIfNone(req, resp, dataSession);
			IGenericClient fhirClient = repositoryClientFactory.newGenericClient(req);
			try {
				HomeController.doHeader(out, "IIS Sandbox - Patients", tenant);
				IBaseResource patientSelected = fetchPatientFromParameter(req, fhirClient, fhirRequester);

				if (patientSelected == null) {
					searchOrPrintAll(req, out, tenant);
				} else {
					singlePatientInformationPrintAll(out, patientSelected, fhirClient, tenant, dataSession, req);
				}
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
		HomeController.doFooter(out);
		out.flush();
		out.close();
	}


	private void singlePatientInformationPrintAll(PrintWriter out, IBaseResource patientSelected, IGenericClient fhirClient, Tenant tenant, Session dataSession, HttpServletRequest req) {
		PatientMaster patientMasterSelected = patientMapper.localObject(patientSelected);
		boolean isGolden = AbstractFhirRequester.isGoldenRecord(patientSelected);

		out.println("<h2>Patient : " + patientMasterSelected.getNameFirst() + " " + patientMasterSelected.getNameMiddle() + " " + patientMasterSelected.getNameLast() + "</h2>");

		printPatient(out, patientMasterSelected);

		out.println("  <div class=\"w3-container\">");

		printPatientVaccinations(out, patientMasterSelected, isGolden);
		printPatientObservations(out, patientMasterSelected, isGolden);
		printRelatedPatients(out, patientMasterSelected, isGolden);

		out.println("  </div>");

		printPatientRecommendationsAndSubscriptions(out, patientSelected, patientMasterSelected, fhirClient);

		out.println("<div class=\"w3-container\">");
		printFhirShortcuts(out, patientSelected, patientMasterSelected, tenant);
		out.println("</div>");
		out.println("<div class=\"w3-container\">");

//		UriComponentsBuilder uriComponentsBuilder = ServletUriComponentsBuilder.fromRequest(req);
//		uriComponentsBuilder
		out.println("<img src=\"" + ServletHelper.tenantifyUrlWithBasePath(tenant, "/patient/qr?id=" + patientMasterSelected.getPatientId()) + "\"  alt=\"shlink\" width=\"200\">");

		String manifestUrl = PatientShLinkController.getManifestUrl(req, patientSelected, tenant);

		out.println("<a href= \"" + manifestUrl + "\">" + manifestUrl + "</a>");
		out.println("<a href= \"" +
			ServletHelper.tenantifyUrlWithBasePath(tenant,
				ShLinkController.SHLINK_CONTROLLER_BASE_PATH + "?" + ShLinkController.PARAM_PATIENT_ID + "=" + patientMasterSelected.getPatientId()) +
			"\">Generate a new Smart Health Link with IPS</a>");

		out.println("<p id =\"qrCode\">");
		ShLinkPayload shLinkPayload = PatientShLinkController.getPatientShLinkPayload(manifestUrl);
		String qrCode = shLinkUtilService.qrCode(shLinkPayload);
		String decodedFrom64 = new String(Base64.getUrlDecoder().decode(qrCode.substring(SHLINK_PREFIX.length()).getBytes()));
		logger.info(decodedFrom64);
		out.println(qrCode);
		out.println("</p>");

//		out.println("<button onclick=\"copyHtmlToClipboard()\">Copy HTML</button>");
//		out.println("<script>");
//		out.println("function copyHtmlToClipboard() {");
//		out.println("    const content = ");
//		out.println("    navigator.clipboard.writeText(content)");
//		out.println("        .then(() => { console.log('HTML copied to clipboard'); })");
//		out.println("        .catch(err => { console.error('Failed to copy HTML: ', err); });");
//		out.println("}");
//		out.println("</script>");


		out.println("</div>");

		out.println("<div class=\"w3-container\">");
		out.println("<h4>Messages Received</h4>");
		Query<MessageReceived> query = dataSession.createQuery(
			"from MessageReceived where patientReportedId = :patientReportedId order by reportedDate asc", MessageReceived.class);
		query.setParameter("patientReportedId", patientMasterSelected.getPatientId());
		List<MessageReceived> messageReceivedList = query.list();
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

	private void printPatientRecommendationsAndSubscriptions(PrintWriter out, IBaseResource patientSelected, PatientMaster patientMasterSelected, IGenericClient fhirClient) {
		IParser parser = repositoryClientFactory.getFhirContext()
			.newJsonParser().setPrettyPrint(true).setSuppressNarratives(true);
		if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
			org.hl7.fhir.r5.model.Bundle recommendationBundle = fhirClient.search()
				.forResource(org.hl7.fhir.r5.model.ImmunizationRecommendation.class)
				.where(org.hl7.fhir.r5.model.ImmunizationRecommendation.PATIENT
					.hasId(new org.hl7.fhir.r5.model.IdType(patientMasterSelected.getPatientId()))
				).returnBundle(org.hl7.fhir.r5.model.Bundle.class).execute();
			if (recommendationBundle.hasEntry()) {
				RecommendationController.printRecommendation(out, (IDomainResource) recommendationBundle.getEntryFirstRep().getResource(), (IDomainResource) patientSelected, fhirContext);
			} else {
				RecommendationController.printRecommendation(out, null, (IDomainResource) patientSelected, fhirContext);
			}
			org.hl7.fhir.r5.model.Bundle subcriptionBundle = fhirClient.search().forResource(org.hl7.fhir.r5.model.Subscription.class).returnBundle(org.hl7.fhir.r5.model.Bundle.class).execute();
			printSubscriptions(out, parser, subcriptionBundle, (org.hl7.fhir.r5.model.Resource) patientSelected);
		}

		if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R4)) {
			org.hl7.fhir.r4.model.Bundle recommendationBundle = fhirClient.search()
				.forResource(org.hl7.fhir.r4.model.ImmunizationRecommendation.class)
				.where(org.hl7.fhir.r4.model.ImmunizationRecommendation.PATIENT
					.hasId(new org.hl7.fhir.r4.model.IdType(patientMasterSelected.getPatientId()))
				).returnBundle(org.hl7.fhir.r4.model.Bundle.class).execute();
			if (recommendationBundle.hasEntry()) {
				RecommendationController.printRecommendation(out, (org.hl7.fhir.r4.model.ImmunizationRecommendation) recommendationBundle.getEntryFirstRep().getResource(), (org.hl7.fhir.r4.model.Patient) patientSelected, fhirContext);
			} else {
				RecommendationController.printRecommendation(out, null, (org.hl7.fhir.r4.model.Patient) patientSelected, fhirContext);
			}
//					org.hl7.fhir.r4.model.Bundle subcriptionBundle = fhirClient.search().forResource(org.hl7.fhir.r4.model.Subscription.class).returnBundle(org.hl7.fhir.r4.model.Bundle.class).execute();
//					printSubscriptions(out, parser, subcriptionBundle, (org.hl7.fhir.r4.model.Resource) patientSelected);
			// TODO support for R4

		}
	}

	private void printRelatedPatients(PrintWriter out, PatientMaster patientMasterSelected, boolean isGolden) {
		List<PatientMaster> relatedPatients = List.of();
		if (isGolden) {
			relatedPatients = fhirRequester.searchPatientReportedFromGoldenIdWithMdmLinks(patientMasterSelected.getPatientId());
		} else {
			PatientMaster goldenRecord = fhirRequester.readPatientMasterWithMdmLink(patientMasterSelected.getPatientId());
			if (goldenRecord != null) {
				relatedPatients = List.of(goldenRecord);
			}
		}
		out.println("<h4>Related Patient records</h4>");
		printPatientList(out, relatedPatients, false);
		HomeController.printGoldenRecordExplanation(out, isGolden);
	}

	private void printPatientVaccinations(PrintWriter out, PatientMaster patientMasterSelected, boolean isGolden) {
		ReferenceParam referenceParam = new ReferenceParam().setValue(patientMasterSelected.getPatientId());
		referenceParam.setMdmExpand(isGolden);
		List<VaccinationMaster> vaccinationList = fhirRequester.searchVaccinationMasterGoldenList(
			new SearchParameterMap().add("patient", referenceParam)
		);
		out.println("<h4>Vaccinations</h4>");
		VaccinationController.printVaccinationList(out, vaccinationList, null); // TODO test and change
	}

	private void printPatientObservations(PrintWriter out, PatientMaster patientMasterSelected, boolean isGolden) {
		ReferenceParam referenceParam = new ReferenceParam().setValue(patientMasterSelected.getPatientId());
		referenceParam.setMdmExpand(isGolden);
		List<ObservationReported> observationReportedList = fhirRequester.searchObservationReportedList(
			new SearchParameterMap("subject", referenceParam));
		Set<String> suppressSet = LoincIdentifier.getSuppressIdentifierCodeSet();
		observationReportedList.removeIf(observationReported -> suppressSet.contains(observationReported.getIdentifierCode()));

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
				patientMasterList = fhirRequester.searchPatientMasterGoldenList(
					new SearchParameterMap("family", new StringParam(patientNameLast))
						.add("name", new StringParam(patientNameFirst))
						.add("identifier", new TokenParam().setValue(externalLink))
				);
			}
		}
		List<PatientMaster> patientMasterList1 = patientMasterList;
		out.println("<h2>Patients</h2>");
		out.println("<div class=\"w3-container w3-half w3-margin-top\">");
		out.println("    <h3>Search Patient Registry</h3>");
		out.println("    <form method=\"GET\" action=\"patient\" class=\"w3-container w3-card-4\">");
		out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_PATIENT_NAME_LAST + "\" value=\"" + patientNameLast + "\"/>");
		out.println("      <label>Last Name</label>");
		out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_PATIENT_NAME_FIRST + "\" value=\"" + patientNameFirst + "\"/>");
		out.println("      <label>First Name</label>");
		out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_PATIENT_REPORTED_EXTERNAL_LINK + "\" value=\"" + externalLink + "\"/>");
		out.println("      <label>Medical Record Number</label><br/>");
		out.println("      <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\""
			+ PARAM_ACTION + "\" value=\"" + ACTION_SEARCH + "\"/>");
		out.println("    </form>");
		out.println("</div>");

		out.println("<div class=\"w3-container\">");

		boolean showingRecent = false;
		if (patientMasterList1 == null) {
			showingRecent = true;
			patientMasterList1 = fhirRequester.searchPatientMasterGoldenList(new SearchParameterMap()); // TODO Paging ?
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
				out.println("<div>Patient golden records (records referenced by duplicates): <a href=\"" + link + "\">" + link + "</a></div>");
			}
			out.println("</div>");
		}
	}

}
