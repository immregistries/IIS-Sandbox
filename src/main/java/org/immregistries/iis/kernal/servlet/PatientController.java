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
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.mapping.interfaces.PatientMapper;
import org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester;
import org.immregistries.iis.kernal.mapping.internalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.immregistries.iis.kernal.servlet.PatientController.PATIENT_BASE_PATH;
import static org.immregistries.iis.kernal.servlet.SubscriptionServlet.PARAM_MESSAGE;
import static org.immregistries.iis.kernal.servlet.SubscriptionServlet.PARAM_SUBSCRIPTION_ID;

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

	public static void printPatientList(PrintWriter out, List<PatientMaster> patientMasterList, boolean showingRecent) {
		if (patientMasterList != null) {
			if (patientMasterList.isEmpty()) {
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
					String link = "patient?" + PARAM_PATIENT_REPORTED_ID + "=" + patient.getPatientId();
					out.println("  <tr>");
					out.println("    <td><a href=\"" + link + "\">" + patient.getMainBusinessIdentifier().getValue() + "</a></td>");
					out.println("    <td><a href=\"" + link + "\">" + patient.getNameLast() + "</a></td>");
					out.println("    <td><a href=\"" + link + "\">" + patient.getNameFirst() + "</a></td>");
					out.println("    <td><a href=\"" + link + "\">" + sdf.format(patient.getUpdatedDate()) + "</a></td>");
					out.println("  </tr>");
				}
				out.println("  </tbody>");
				out.println("</table>");

				if (count > 99) {
					out.println("<em>Only the first 100 are shown</em>");
				}
			}
		}
	}

	public static void printObservationList(PrintWriter out, List<ObservationReported> observationReportedList) {
		SimpleDateFormat sdfDate = new SimpleDateFormat("MM/dd/yyyy");
		if (observationReportedList.isEmpty()) {
			out.println("<div class=\"w3-panel w3-yellow\"><p>No Observations found</p></div>");
		} else {
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
					if (StringUtils.isBlank(observationReported.getIdentifierLabel())) {
						out.println("      " + code);
					} else {
						String table = observationReported.getIdentifierTable();
						if (StringUtils.isBlank(table)) {
							out.println("      " + observationReported.getIdentifierLabel() + " (" + code + ")");
						} else {
							switch (table) {
								case "LN":
									table = "Loinc";
									break;
								case "99TPG":
									table = "Priority";
									break;
								case "SCT":
									table = "Snomed";
									break;
							}
							out.println("      " + observationReported.getIdentifierLabel() + " (" + table + " "
								+ code + ")");
						}
					}
					if ("LN".equals(observationReported.getIdentifierTable()) || "99TPG".equals(observationReported.getIdentifierTable())) {
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
					String value = StringUtils.defaultString(observationReported.getValueCode());
					Date valueDate = null;
					if (value.length() > 8) {
						value = value.substring(8);
					}
					if (value.length() == 8) {
						SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
						try {
							valueDate = sdf.parse(value);
						} catch (ParseException ignored) {
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
					if (StringUtils.isBlank(observationReported.getValueLabel())) {
						out.println("      " + code);
					} else {
						String table = observationReported.getValueTable();
						if (StringUtils.isBlank(table)) {
							out.println("      " + observationReported.getValueLabel() + " (" + code + ")");
						} else {
							switch (table) {
								case "LN":
									table = "Loinc";
									break;
								case "99TPG":
									table = "Priority";
									break;
								case "SCT":
									table = "Snomed";
									break;
							}
							out.println(
								"      " + observationReported.getValueLabel() + " (" + table + " " + code + ")");
						}
					}
					if ("SCT".equals(observationReported.getValueTable())
						|| "CDCPHINVS".equals(observationReported.getValueTable())
						|| "99TPG".equals(observationReported.getValueTable())) {
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
	}

	public static void printPatient(PrintWriter out, PatientMaster patientSelected) {
		SimpleDateFormat sdfDate = new SimpleDateFormat("MM/dd/yyyy");
		out.println("    <div class=\"w3-container w3-half w3-margin-top\">");
		out.println("<table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
		out.println("  <tbody>");
		out.println("  <tr>");
		out.println("    <th class=\"w3-green\">External Id (MRN)</th>");
		out.println("    <td>" + patientSelected.getMainBusinessIdentifier().getValue() + "</td>");
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

	public static void printSubscriptions(PrintWriter out, IParser parser, org.hl7.fhir.r5.model.Bundle bundle, org.hl7.fhir.r5.model.Resource resource) {
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

	public static IDomainResource fetchPatientFromParameter(HttpServletRequest req, IGenericClient fhirClient, AbstractFhirRequester fhirRequester) {
		IDomainResource patient = null;
		if (req.getParameter(PARAM_PATIENT_REPORTED_ID) != null) {
			patient = (IDomainResource) fhirClient.read().resource("Patient").withId(req.getParameter(PARAM_PATIENT_REPORTED_ID)).execute();
		} else if (req.getParameter(PARAM_PATIENT_REPORTED_EXTERNAL_LINK) != null) {
			IBundleProvider bundleProvider = fhirRequester.searchGoldenRecord(org.hl7.fhir.r5.model.Patient.class,
				new SearchParameterMap(org.hl7.fhir.r5.model.Patient.SP_IDENTIFIER, new TokenParam().setValue(req.getParameter(PARAM_PATIENT_REPORTED_EXTERNAL_LINK))));
			if (!bundleProvider.isEmpty()) {
				patient = (IDomainResource) bundleProvider.getAllResources().get(0);
			}
		}
		return patient;
	}

	public static void printMessageReceived(PrintWriter out, MessageReceived messageReceived) {
		SimpleDateFormat sdfTime = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		out.println("     <h3>" + messageReceived.getCategoryRequest() + " - "
			+ messageReceived.getCategoryResponse() + " "
			+ sdfTime.format(messageReceived.getReportedDate()) + "</h3>");
		out.println("     <pre>" + messageReceived.getMessageRequest() + "</pre>");
		out.println("     <pre>" + messageReceived.getMessageResponse() + "</pre>");
	}

	private static void printFhirShortcuts(PrintWriter out, IBaseResource patientSelected, PatientMaster patientMasterSelected, Tenant tenant) {
		out.println("<h4>FHIR Api Shortcuts</h4>");
		String apiBaseUrl = "/iis/fhir/" + tenant.getOrganizationName();
		{
			String link = apiBaseUrl + "/Patient/" + patientMasterSelected.getPatientId();
			out.println("<div>FHIR Resource: <a href=\"" + link + "\">" + link + "</a></div>");
		}
		{
			String link = apiBaseUrl + "/Patient/" + patientMasterSelected.getPatientId() + "/$everything?_mdm=true";
			out.println("<div>Everything related to this Patient: <a href=\"" + link + "\">" + link + "</a></div>");
		}
		{
			String link = apiBaseUrl + "/Patient/" + patientMasterSelected.getPatientId() + "/$summary";
			out.println("<div>International Patient Summary: <a href=\"" + link + "\">" + link + "</a></div>");
		}
		{
			String link = apiBaseUrl + "/Immunization?patient:mdm=Patient/" + patientMasterSelected.getPatientId();
			out.println("<div>All Immunizations related<a href=\"" + link + "\">" + link + "</a></div>");
		}
		{
			String link = apiBaseUrl + "/Observation?patient:mdm=Patient/" + patientMasterSelected.getPatientId();
			out.println("<div>All Observations related<a href=\"" + link + "\">" + link + "</a></div>");
		}
		{
			String link;
			if (AbstractFhirRequester.isGoldenRecord(patientSelected)) {
				link = apiBaseUrl + "/$mdm-query-links?goldenResourceId=" + patientMasterSelected.getPatientId();
			} else {
				link = apiBaseUrl + "/$mdm-query-links?resourceId=" + patientMasterSelected.getPatientId();
			}
			out.println("<div>Related Patient Records: <a href=\"" + link + "\">" + link + "</a></div>");
		}
	}

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
			Tenant tenant = ServletHelper.getTenant(req, dataSession);
			if (tenant == null) {
				if (ServletHelper.getUserAccess() != null) {
					resp.sendRedirect("/iis/tenant");
				}
				throw new AuthenticationCredentialsNotFoundException("");
			}
			IGenericClient fhirClient = repositoryClientFactory.newGenericClient(req);
			try {
				HomeServlet.doHeader(out, "IIS Sandbox - Patients", tenant);
				IBaseResource patientSelected = fetchPatientFromParameter(req, fhirClient, fhirRequester);

				if (patientSelected == null) {
					searchOrPrintAll(req, out, tenant);
				} else {
					singlePatientInformationPrintAll(out, patientSelected, fhirClient, tenant, dataSession);
				}
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
		HomeServlet.doFooter(out);
		out.flush();
		out.close();
	}

	private void singlePatientInformationPrintAll(PrintWriter out, IBaseResource patientSelected, IGenericClient fhirClient, Tenant tenant, Session dataSession) {
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
		HomeServlet.printGoldenRecordExplanation(out, isGolden);
	}

	private void printPatientVaccinations(PrintWriter out, PatientMaster patientMasterSelected, boolean isGolden) {
		ReferenceParam referenceParam = new ReferenceParam().setValue(patientMasterSelected.getPatientId());
		referenceParam.setMdmExpand(isGolden);
		List<VaccinationMaster> vaccinationList = fhirRequester.searchVaccinationMasterGoldenList(
			new SearchParameterMap().add("patient", referenceParam)
		);
		out.println("<h4>Vaccinations</h4>");
		VaccinationController.printVaccinationList(out, vaccinationList);
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
			String apiBaseUrl = "/iis/fhir/" + tenant.getOrganizationName();
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
