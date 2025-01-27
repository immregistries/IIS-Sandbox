package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.InternalClient.FhirRequester;
import org.immregistries.iis.kernal.InternalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.logic.CodeMapManager;
import org.immregistries.iis.kernal.mapping.Interfaces.PatientMapper;
import org.immregistries.iis.kernal.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.immregistries.iis.kernal.servlet.RecommendationServlet.PARAM_RECOMMENDATION_ID;
import static org.immregistries.iis.kernal.servlet.SubscriptionServlet.PARAM_MESSAGE;
import static org.immregistries.iis.kernal.servlet.SubscriptionServlet.PARAM_SUBSCRIPTION_ID;

@RestController
@RequestMapping({"/patient", "/tenant/{tenantId}/patient"})
public class PatientServlet  {
	public static final String PARAM_ACTION = "action";
	public static final String ACTION_SEARCH = "search";
	public static final String PARAM_PATIENT_NAME_LAST = "patientNameLast";
	public static final String PARAM_PATIENT_NAME_FIRST = "patientNameFirst";
	public static final String PARAM_PATIENT_REPORTED_EXTERNAL_LINK = "identifier";
	public static final String PARAM_PATIENT_REPORTED_ID = "id";
	Logger logger = LoggerFactory.getLogger(PatientServlet.class);
	@Autowired
	RepositoryClientFactory repositoryClientFactory;
	@Autowired
	FhirRequester fhirRequester;
	@Autowired
	FhirContext fhirContext;
	@Autowired
	PatientMapper patientMapper;

	public static String linkUrl(String facilityId) {
		return "/tenant/" + facilityId + "/patient";
	}

	public static void printMessageReceived(PrintWriter out, MessageReceived messageReceived) {
		SimpleDateFormat sdfTime = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		out.println("     <h3>" + messageReceived.getCategoryRequest() + " - "
			+ messageReceived.getCategoryResponse() + " "
			+ sdfTime.format(messageReceived.getReportedDate()) + "</h3>");
		out.println("     <pre>" + messageReceived.getMessageRequest() + "</pre>");
		out.println("     <pre>" + messageReceived.getMessageResponse() + "</pre>");
	}

	@PostMapping
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		doGet(req, resp);
	}

	@SuppressWarnings("unchecked")
	@GetMapping
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		Tenant tenant = ServletHelper.getTenant();

		if (tenant == null) {
			throw new AuthenticationCredentialsNotFoundException("");
		}

		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		Session dataSession = PopServlet.getDataSession();
		IGenericClient fhirClient = repositoryClientFactory.newGenericClient(req);
		try {
			String patientNameLast = req.getParameter(PARAM_PATIENT_NAME_LAST);
			String patientNameFirst = req.getParameter(PARAM_PATIENT_NAME_FIRST);
			String externalLink = req.getParameter(PARAM_PATIENT_REPORTED_EXTERNAL_LINK);

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

			if (patientNameLast == null) {
				patientNameLast = "";
			}
			if (patientNameFirst == null) {
				patientNameFirst = "";
			}
			if (externalLink == null) {
				externalLink = "";
			}

			HomeServlet.doHeader(out, "IIS Sandbox - Patients");

			PatientMaster patientMasterSelected = null;
			IBaseResource patientSelected  = getPatientFromParameter(req,fhirClient);

			if (patientSelected == null) {
				out.println("<h2>Patients</h2>");
				out.println("<div class=\"w3-container w3-half w3-margin-top\">");
				out.println("    <h3>Search Patient Registry</h3>");
				out.println("    <form method=\"GET\" action=\"patient\" class=\"w3-container w3-card-4\">");
				out.println("      <input class=\"w3-input\" type=\"text\" name=\""
					+ PARAM_PATIENT_NAME_LAST + "\" value=\"" + patientNameLast + "\"/>");
				out.println("      <label>Last Name</label>");
				out.println("      <input class=\"w3-input\" type=\"text\" name=\""
					+ PARAM_PATIENT_NAME_FIRST + "\" value=\"" + patientNameFirst + "\"/>");
				out.println("      <label>First Name</label>");
				out.println("      <input class=\"w3-input\" type=\"text\" name=\""
					+ PARAM_PATIENT_REPORTED_EXTERNAL_LINK + "\" value=\"" + externalLink
					+ "\"/>");
				out.println("      <label>Medical Record Number</label><br/>");
				out.println("      <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\""
					+ PARAM_ACTION + "\" value=\"" + ACTION_SEARCH + "\"/>");
				out.println("    </form>");
				out.println("</div>");

				out.println("<div class=\"w3-container\">");

				boolean showingRecent = false;
				if (patientMasterList == null) {
					showingRecent = true;
					patientMasterList = fhirRequester.searchPatientMasterGoldenList(new SearchParameterMap()); // TODO Paging ?
				}

				if (patientMasterList != null) {
					if (patientMasterList.size() == 0) {
						out.println("<div class=\"w3-panel w3-yellow\"><p>No Records Found</p></div>");
					} else {
						if (showingRecent) {
							out.println("<h4>Recent Updates</h4>");
						}
						out.println("<table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
						out.println("  <tr class=\"w3-green\">");
						out.println("    <th>MRN</th>");
						out.println("    <th>Last Name</th>");
						out.println("    <th>First Name</th>");
						out.println("    <th>Last Updated</th>");
						out.println("  </tr>");
						out.println("  <tbody>");
						SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
						int count = 0;
						for (PatientMaster patient : patientMasterList) {
							count++;
							if (count > 100) {
								break;
							}
							String link = "patient?" + PARAM_PATIENT_REPORTED_ID+ "="
								+ patient.getPatientId();
							out.println("  <tr>");
							out.println("    <td><a href=\"" + link + "\">"
								+ patient.getMainPatientIdentifier().getValue() + "</a></td>");
							out.println("    <td><a href=\"" + link + "\">" + patient.getNameLast()
								+ "</a></td>");
							out.println("    <td><a href=\"" + link + "\">"
								+ patient.getNameFirst() + "</a></td>");
							out.println("    <td><a href=\"" + link + "\">"
								+ sdf.format(patient.getUpdatedDate()) + "</a></td>");
							out.println("  </tr>");
						}
						out.println("  </tbody>");
						out.println("</table>");

						if (count > 99) {
							out.println("<em>Only the first 100 are shown</em>");
						}
					}
				}
				out.println("  </div>");

				{
					out.println("<div class=\"w3-container\">");
					out.println("<h4>FHIR Api Shortcuts</h4>");
					String apiBaseUrl = "/iis/fhir/" + tenant.getOrganizationName();
					{
						String link = apiBaseUrl + "/Patient";
						out.println("<div>All FHIR Patient records: <a href=\"" + link + "\">" + link + "</a></div>");
					}
					{
						String link = apiBaseUrl + "/Patient"+ "?_tag=GOLDEN_RECORD";
						out.println("<div>Patient golden records (records referenced by duplicates): <a href=\"" + link + "\">" + link  +"</a></div>");
					}
					out.println("</div>");
				}
			} else {
				patientMasterSelected = patientMapper.localObject(patientSelected);
				IParser parser = repositoryClientFactory.getFhirContext()
					.newJsonParser().setPrettyPrint(true).setSuppressNarratives(true);
				out.println("<h2>Patient : " + patientMasterSelected.getNameFirst() + " " + patientMasterSelected.getNameMiddle() + " " + patientMasterSelected.getNameLast() + "</h2>");
				{
					printPatient(out, patientMasterSelected);
				}
				{
					printVaccinationList(req, resp, out, fhirClient, patientMasterSelected.getPatientId());
				}
				{
					List<ObservationReported> observationReportedList =
						getObservationList(fhirClient, patientMasterSelected);
					if (observationReportedList.size() != 0) {
						out.println("<h4>Patient Observations</h4>");
						printObservations(out, observationReportedList);
					}
					out.println("  </div>");
				}

				if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) { // TODO support for R4
					org.hl7.fhir.r5.model.Bundle recommendationBundle = fhirClient.search()
						.forResource(org.hl7.fhir.r5.model.ImmunizationRecommendation.class)
						.where(org.hl7.fhir.r5.model.ImmunizationRecommendation.PATIENT
							.hasId(new org.hl7.fhir.r5.model.IdType(patientMasterSelected.getPatientId()))
						).returnBundle(org.hl7.fhir.r5.model.Bundle.class).execute();
					if (recommendationBundle.hasEntry()) {
						printRecommendation(out, (org.hl7.fhir.r5.model.ImmunizationRecommendation) recommendationBundle.getEntryFirstRep().getResource(), (org.hl7.fhir.r5.model.Patient) patientSelected);
					} else {
						printRecommendation(out, null, (org.hl7.fhir.r5.model.Patient) patientSelected);
					}
					org.hl7.fhir.r5.model.Bundle subcriptionBundle = fhirClient.search().forResource(org.hl7.fhir.r5.model.Subscription.class).returnBundle(org.hl7.fhir.r5.model.Bundle.class).execute();
					printSubscriptions(out, parser, subcriptionBundle, (org.hl7.fhir.r5.model.Resource) patientSelected);
				}

				if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R4)) {

				}

				{
					out.println("<div class=\"w3-container\">");
					out.println("<h4>FHIR Api Shortcuts</h4>");
					String apiBaseUrl = "/iis/fhir/" + tenant.getOrganizationName();
					{
						String link = apiBaseUrl + "/Patient/" + patientMasterSelected.getPatientId();
						out.println("<div>FHIR Resource: <a href=\"" + link + "\">" + link + "</a></div>");
					}
					{
						String link = apiBaseUrl + "/Patient/" + patientMasterSelected.getPatientId() + "/$everything?_mdm=true";
						out.println("<div>Everything related to this Patient: <a href=\"" + link + "\">" + link  +"</a></div>");
					}
					{
						String link = apiBaseUrl + "/Patient/" + patientMasterSelected.getPatientId() + "/$summary";
						out.println("<div>International Patient Summary: <a href=\"" + link + "\">" + link  +"</a></div>");
					}
					out.println("</div>");
				}

				{
					out.println("<div class=\"w3-container\">");
					out.println("<h4>Messages Received</h4>");
					Query query = dataSession.createQuery(
						"from MessageReceived where patientReportedId = :patientReportedId order by reportedDate asc");
					query.setParameter("patientReportedId", patientMasterSelected.getPatientId());
					List<MessageReceived> messageReceivedList = query.list();
					if (messageReceivedList.size() == 0) {
						out.println("<div class=\"w3-panel w3-yellow\"><p>No Messages Received</p></div>");
					} else {
						for (MessageReceived messageReceived : messageReceivedList) {
							printMessageReceived(out, messageReceived);
						}
					}
					out.println("</div>");
				}

				out.println("</div>");
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		} finally {
			dataSession.close();
		}
		HomeServlet.doFooter(out);
		out.flush();
		out.close();
	}

	public void printVaccinationList(HttpServletRequest req, HttpServletResponse resp, PrintWriter out, IGenericClient fhirClient, String patientId) {
		SimpleDateFormat sdfDate = new SimpleDateFormat("MM/dd/yyyy");
		out.println("  <div class=\"w3-container\">");
		out.println("<h4>Vaccinations</h4>");
		List<VaccinationMaster> vaccinationList = null;
		{
			vaccinationList = fhirRequester.searchVaccinationListOperationEverything(patientId);
		}
		if (vaccinationList.size() == 0) {
			out.println("<div class=\"w3-panel w3-yellow\"><p>No Vaccinations</p></div>");
		} else {
			CodeMap codeMap = CodeMapManager.getCodeMap();
			out.println(
				"<table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
			out.println("  <tr class=\"w3-green\">");
			out.println("    <th>Vaccine</th>");
			out.println("    <th>Admin Date</th>");
			out.println("    <th>Manufacturer</th>");
			out.println("    <th>Lot Number</th>");
			out.println("    <th>Information</th>");
			out.println("    <th>Completion</th>");
			out.println("    <th>Action</th>");
			out.println("  </tr>");
			out.println("  <tbody>");
			for (VaccinationMaster vaccination : vaccinationList) {
				out.println("  <tr>");
				out.println("    <td>");
				String link = "vaccination?" + VaccinationServlet.PARAM_VACCINATION_REPORTED_ID + "="
					+ vaccination.getVaccinationId();
				out.println("      <a href=\"" + link + "\">");
				if (!StringUtils.isEmpty(vaccination.getVaccineCvxCode())) {
					Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE,
						vaccination.getVaccineCvxCode());
					if (cvxCode == null) {
						out.println("Unknown CVX (" + vaccination.getVaccineCvxCode() + ")");
					} else {
						out.println(
							cvxCode.getLabel() + " (" + vaccination.getVaccineCvxCode() + ")");
					}
				}
				out.println("      </a>");
				out.println("    </td>");
				out.println("    <td>");
				if (vaccination.getAdministeredDate() == null) {
					out.println("null");
				} else {
					out.println(sdfDate.format(vaccination.getAdministeredDate()));
				}
				out.println("    </td>");
				out.println("    <td>");
				if (!StringUtils.isEmpty(vaccination.getVaccineMvxCode())) {
					Code mvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_MANUFACTURER_CODE,
						vaccination.getVaccineMvxCode());
					if (mvxCode == null) {
						out.print("Unknown MVX");
					} else {
						out.print(mvxCode.getLabel());
					}
					out.println(" (" + vaccination.getVaccineMvxCode() + ")");
				}
				out.println("    </td>");
				out.println("    <td>" + vaccination.getLotnumber() + "</td>");
				out.println("    <td>");
				if (!StringUtils.isEmpty(vaccination.getInformationSource())) {
					Code informationCode =
						codeMap.getCodeForCodeset(CodesetType.VACCINATION_INFORMATION_SOURCE,
							vaccination.getInformationSource());
					if (informationCode != null) {
						out.print(informationCode.getLabel());
						out.println(" (" + vaccination.getInformationSource() + ")");
					}
				}
				out.println("    </td>");
				out.println("    <td>");
				if (!StringUtils.isEmpty(vaccination.getCompletionStatus())) {
					Code completionCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_COMPLETION,
						vaccination.getCompletionStatus());
					if (completionCode != null) {
						out.print(completionCode.getLabel());
						out.println(" (" + vaccination.getCompletionStatus() + ")");
					}
				}
				out.println("    <td>");
				if (!StringUtils.isEmpty(vaccination.getActionCode())) {
					Code actionCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_ACTION_CODE,
						vaccination.getActionCode());
					if (actionCode != null) {
						out.print(actionCode.getLabel());
						out.println(" (" + vaccination.getActionCode() + ")");
					}
				}
				out.println("    </td>");
				out.println("  </tr>");
			}
			out.println("  </tbody>");
			out.println("</table>");
		}
	}

	public void printObservations(PrintWriter out,
											List<ObservationReported> observationReportedList) {
		SimpleDateFormat sdfDate = new SimpleDateFormat("MM/dd/yyyy");
		out.println("<table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
		out.println("  <tr class=\"w3-green\">");
		out.println("    <th>Identifier</th>");
		out.println("    <th>Value</th>");
		out.println("    <th>Date</th>");
		out.println("  </tr>");
		out.println("  <tbody>");
		for (ObservationReported observationReported : observationReportedList) {
			out.println("<tr>");
			String valueType = observationReported.getValueType();
			if (valueType == null) {
				valueType = "CE";
			}
			out.println("<td>");
			{
				String code = observationReported.getIdentifierCode();
				if (observationReported.getIdentifierLabel().equals("")) {
					out.println("      " + code);
				} else {
					String table = observationReported.getIdentifierTable();
					if (table.equals("")) {
						out.println("      " + observationReported.getIdentifierLabel() + " (" + code + ")");
					} else {
						if (table.equals("LN")) {
							table = "Loinc";
						} else if (table.equals("99TPG")) {
							table = "Priority";
						} else if (table.equals("SCT")) {
							table = "Snomed";
						}
						out.println("      " + observationReported.getIdentifierLabel() + " (" + table + " "
							+ code + ")");
					}
				}
				if (observationReported.getIdentifierTable().equals("LN")
					|| observationReported.getIdentifierTable().equals("99TPG")) {
					LoincIdentifier loincIdentifier = null;
					for (LoincIdentifier oi : LoincIdentifier.values()) {
						if (oi.getIdentifierCode().equalsIgnoreCase(code)) {
							loincIdentifier = oi;
							break;
						}
					}
					if (loincIdentifier == null) {
						out.println("<div class=\"w3-panel w3-yellow\">Not Recognized</div>");
					} else {
						out.println("&#10004;");
						if (!loincIdentifier.getIdentifierLabel()
							.equalsIgnoreCase(observationReported.getIdentifierLabel())) {
							out.println("Matches: " + loincIdentifier.getIdentifierLabel());
						}
					}
				}
			}
			out.println("</td>");
			out.println("<td>");
			if (valueType.equals("DT")) {
				String value = observationReported.getValueCode();
				Date valueDate = null;
				if (value == null) {
					value = "";
				}
				if (value.length() > 8) {
					value = value.substring(8);
				}
				if (value.length() == 8) {
					SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
					try {
						valueDate = sdf.parse(value);
					} catch (ParseException pe) {
						//ignore
					}
				}
				if (valueDate == null) {
					out.println("      " + value);
				} else {
					out.println("      " + sdfDate.format(valueDate));
				}
			} else if (valueType.equals("SN")) {
				out.println("      " + observationReported.getValueLabel() + " "
					+ observationReported.getValueTable() + " " + observationReported.getValueCode());
			} else {
				String code = observationReported.getValueCode();
				if (observationReported.getValueLabel() == null || observationReported.getValueLabel().equals("")) {
					out.println("      " + code);
				} else {
					String table = observationReported.getValueTable();
					if (table.equals("")) {
						out.println("      " + observationReported.getValueLabel() + " (" + code + ")");
					} else {
						if (table.equals("LN")) {
							table = "Loinc";
						} else if (table.equals("99TPG")) {
							table = "Priority";
						} else if (table.equals("SCT")) {
							table = "Snomed";
						}
						out.println(
							"      " + observationReported.getValueLabel() + " (" + table + " " + code + ")");
					}
				}
				if (observationReported.getValueTable() != null && (observationReported.getValueTable().equals("SCT")
					|| observationReported.getValueTable().equals("CDCPHINVS")
					|| observationReported.getValueTable().equals("99TPG"))) {
					SnomedValue snomedValue = null;
					for (SnomedValue sv : SnomedValue.values()) {
						if (sv.getIdentifierCode().equalsIgnoreCase(code)) {
							snomedValue = sv;
							break;
						}
					}
					if (snomedValue == null) {
						out.println("<div class=\"w3-panel w3-yellow\">Not Recognized</div>");
					} else {
						out.println("&#10004;");
						if (!snomedValue.getIdentifierLabel()
							.equalsIgnoreCase(observationReported.getValueLabel())) {
							out.println("Matches: " + snomedValue.getIdentifierLabel());
						}
					}
				}
			}
			out.println("    </td>");

			if (observationReported.getObservationDate() == null) {
				out.println("<td></td>");
			} else {
				out.println(
					"    <td>" + sdfDate.format(observationReported.getObservationDate()) + "</td>");
			}
			out.println("  </tr>");
		}
		out.println("  </tbody>");
		out.println("</table>");
	}

	public void printPatient(PrintWriter out, PatientMaster patientSelected) {
		SimpleDateFormat sdfDate = new SimpleDateFormat("MM/dd/yyyy");
		out.println("    <div class=\"w3-container w3-half w3-margin-top\">");
		out.println("<table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
		out.println("  <tbody>");
		out.println("  <tr>");
		out.println("    <th class=\"w3-green\">External Id (MRN)</th>");
		out.println("    <td>" + patientSelected.getMainPatientIdentifier().getValue() + "</td>");
		out.println("  </tr>");
		out.println("  <tr>");
		out.println("    <th class=\"w3-green\">Patient Name</th>");
		out.println("    <td>" + patientSelected.getNameLast() + ", "
			+ patientSelected.getNameFirst() + " "
			+ patientSelected.getNameMiddle() + "</td>");
		out.println("  </tr>");
		{
			out.println("  <tr>");
			out.println("    <th class=\"w3-green\">Birth Date</th>");
			out.println(
				"    <td>" + sdfDate.format(patientSelected.getBirthDate()) + "</td>");
			out.println("  </tr>");
		}
		out.println("  </tbody>");
		out.println("</table>");
		out.println("</div>");
	}

	public List<ObservationReported> getObservationList(IGenericClient fhirClient, PatientMaster patientSelected) {
		List<ObservationReported> observationReportedList = new ArrayList<>();
		{
			observationReportedList = fhirRequester.searchObservationReportedList(
				new SearchParameterMap("subject", new ReferenceParam().setValue(patientSelected.getPatientId())));
//				Observation.SUBJECT.hasId(patientSelected.getPatientId()));
//		 observationReportedList = observationReportedList.stream().filter(observationReported -> observationReported.getVaccinationReported() == null).collect(Collectors.toList());
			Set<String> suppressSet = LoincIdentifier.getSuppressIdentifierCodeSet();
			for (Iterator<ObservationReported> it = observationReportedList.iterator(); it.hasNext(); ) {
				ObservationReported observationReported = it.next();
				if (suppressSet.contains(observationReported.getIdentifierCode())) {
					it.remove();
				}
			}
		}
		return observationReportedList;
	}

	public void printSubscriptions(PrintWriter out, IParser parser, org.hl7.fhir.r5.model.Bundle bundle, org.hl7.fhir.r5.model.Resource resource) {
		String resourceString = parser.encodeResourceToString(resource);
//		  .replace("\"","\'")
		out.println("<div class=\"w3-container\">");
		out.println("<h4>Send through subscriptions</h4>");
		if (bundle.hasEntry()) {
			out.println("<table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
			out.println("  <tr class=\"w3-green\">");
			out.println("    <th>Name</th>");
			out.println("    <th>Endpoint</th>");
			out.println("    <th>Status</th>");
			out.println("    <th></th>");
			out.println("  </tr>");
			out.println("<tbody>");
			int count = 0;
			for (org.hl7.fhir.r5.model.Bundle.BundleEntryComponent entry : bundle.getEntry()) {
				org.hl7.fhir.r5.model.Subscription subscription = (org.hl7.fhir.r5.model.Subscription) entry.getResource();
				count++;
				if (count > 100) {
					break;
				}
				out.println("<tr>");
				out.println("     <td><a>" + subscription.getName() + "</a></td>");
				out.println("     <td><a>" + subscription.getEndpoint() + "</a></td>");
				out.println("     <td><a>" + subscription.getStatus() + "</a></td>");
				out.println("		<td>" +
					"<form method=\"GET\" action=\"subscription\" target=\"_blank\" style=\"margin: 0;\">");
				out.println("			<input type=\"hidden\" name=\""
					+ PARAM_SUBSCRIPTION_ID + "\" value=\"" + subscription.getIdentifierFirstRep().getValue() + "\"/>");
				out.println("			<input type=\"hidden\" name=\""
					+ PARAM_MESSAGE + "\" value='" + resourceString + "'/>");
				out.println("			<input class=\"w3-button w3-teal w3-ripple\" type=\"submit\" value=\"Send\" style=\"padding-bottom: 2px;padding-top: 2px;\"/>");
				out.println("</form></td>");
				out.println("</tr>");
			}
			out.println("</tbody>");
			out.println("</table>");
		} else {
			out.println("<div class=\"w3-panel w3-yellow\"><p>No Subscription Found</p></div>");
		}
		out.println("</div>");
	}

	public void printRecommendation(PrintWriter out, org.hl7.fhir.r5.model.ImmunizationRecommendation recommendation, org.hl7.fhir.r5.model.Patient patient) {
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

	protected IBaseResource getPatientFromParameter(HttpServletRequest req, IGenericClient fhirClient) {
		IBaseResource patient = null;
		if (req.getParameter(PARAM_PATIENT_REPORTED_ID) != null) {
			patient = fhirClient.read().resource("Patient").withId(req.getParameter(PARAM_PATIENT_REPORTED_ID)).execute();
		} else if (req.getParameter(PARAM_PATIENT_REPORTED_EXTERNAL_LINK) != null) {
			IBundleProvider bundleProvider = fhirRequester.searchGoldenRecord(org.hl7.fhir.r5.model.Patient.class, //TODO choose priority golden or regular
				new SearchParameterMap(org.hl7.fhir.r5.model.Patient.SP_IDENTIFIER, new TokenParam().setValue(req.getParameter(PARAM_PATIENT_REPORTED_EXTERNAL_LINK))));
//				Patient.IDENTIFIER.exactly().identifier(req.getParameter(PARAM_PATIENT_REPORTED_EXTERNAL_LINK)));
			if (!bundleProvider.isEmpty()) {
				patient = bundleProvider.getAllResources().get(0);

			}
//			if (patientBundle.hasEntry()) {
//				patient = (Patient) patientBundle.getEntryFirstRep().getResource();
//			}
//			else {
//				patientBundle = (Bundle) fhirRequester.searchRegularRecord(Patient.class,
//					Patient.IDENTIFIER.exactly().identifier(req.getParameter(PARAM_PATIENT_REPORTED_EXTERNAL_LINK)));
//				if (patientBundle.hasEntry()) {
//					patient = (Patient) patientBundle.getEntryFirstRep().getResource();
//				}
//			}
		}
		return patient;
	}

}
