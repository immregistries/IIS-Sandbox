package org.immregistries.iis.kernal.controllers.servlet.util;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.TokenParam;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r5.model.Patient;
import org.immregistries.iis.kernal.mapping.internalClient.FhirRequesterUtil;
import org.immregistries.iis.kernal.mapping.internalClient.FhirSearchRequester;
import org.immregistries.iis.kernal.mapping.internalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.model.LoincIdentifier;
import org.immregistries.iis.kernal.model.ObservationReported;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.SnomedValue;
import org.immregistries.iis.kernal.persisted.model.MessageReceived;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.immregistries.iis.kernal.controllers.servlet.PatientController.PARAM_PATIENT_REPORTED_EXTERNAL_LINK;
import static org.immregistries.iis.kernal.controllers.servlet.PatientController.PARAM_PATIENT_REPORTED_ID;
import static org.immregistries.iis.kernal.controllers.servlet.SubscriptionController.PARAM_MESSAGE;
import static org.immregistries.iis.kernal.controllers.servlet.SubscriptionController.PARAM_SUBSCRIPTION_ID;

public final class PatientServletUtil {

	public static void printPatientList(PrintWriter out, List<? extends PatientMaster> patientMasterList, boolean showingRecent) {
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
					out.println("    <td><a href=\"" + link + "\">" + patient.getMainBusinessIdentifier().getValue()
							+ "</a></td>");
					out.println("    <td><a href=\"" + link + "\">" + patient.getNameLast() + "</a></td>");
					out.println("    <td><a href=\"" + link + "\">" + patient.getNameFirst() + "</a></td>");
					out.println(
							"    <td><a href=\"" + link + "\">" + sdf.format(patient.getUpdatedDate()) + "</a></td>");
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
					if ("LN".equals(observationReported.getIdentifierTable())
							|| "99TPG".equals(observationReported.getIdentifierTable())) {
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

	public static void printSubscriptions(PrintWriter out, IParser parser, org.hl7.fhir.r5.model.Bundle bundle,
			org.hl7.fhir.r5.model.Resource resource) {
		String resourceString = parser.encodeResourceToString(resource);
		// .replace("\"","\'")
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
				org.hl7.fhir.r5.model.Subscription subscription = (org.hl7.fhir.r5.model.Subscription) entry
						.getResource();
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
						+ PARAM_SUBSCRIPTION_ID + "\" value=\"" + subscription.getIdentifierFirstRep().getValue()
						+ "\"/>");
				out.println("			<input type=\"hidden\" name=\""
						+ PARAM_MESSAGE + "\" value='" + resourceString + "'/>");
				out.println(
						"			<input class=\"w3-button w3-teal w3-ripple\" type=\"submit\" value=\"Send\" style=\"padding-bottom: 2px;padding-top: 2px;\"/>");
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

	public static IDomainResource fetchPatientFromParameter(HttpServletRequest req, IGenericClient fhirClient,
			FhirSearchRequester fhirSearchRequester) {
		String idParam = req.getParameter(PARAM_PATIENT_REPORTED_ID);
		String identifierParam = req.getParameter(PARAM_PATIENT_REPORTED_EXTERNAL_LINK);
		return fetchPatientFromParameters(idParam, identifierParam, fhirClient, fhirSearchRequester);
	}

	public static @Nullable IDomainResource fetchPatientFromParameters(String idParam, String identifierParam,
																							 IGenericClient fhirClient, FhirSearchRequester fhirSearchRequester) {
		IDomainResource patient = null;
		if (idParam != null) {
			patient = (IDomainResource) fhirClient.read().resource("Patient").withId(idParam).execute();
		} else {
			if (identifierParam != null) {
				SearchParameterMap searchParameterMap = new SearchParameterMap(Patient.SP_IDENTIFIER,
						new TokenParam().setValue(identifierParam));
				IBundleProvider bundleProvider = fhirSearchRequester.searchGoldenRecord(Patient.class, searchParameterMap);
				if (!bundleProvider.isEmpty()) {
					patient = (IDomainResource) bundleProvider.getAllResources().get(0);
				}
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

	public static void printFhirShortcuts(PrintWriter out, IBaseResource patientSelected,
			PatientMaster patientMasterSelected, Tenant tenant) {
		out.println("<h4>FHIR Api Shortcuts</h4>");
		String apiBaseUrl = RepositoryClientFactory.fhirServerBasePath(tenant);
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
			if (FhirRequesterUtil.isGoldenRecord(patientSelected)) {
				link = apiBaseUrl + "/$mdm-query-links?goldenResourceId=" + patientMasterSelected.getPatientId();
			} else {
				link = apiBaseUrl + "/$mdm-query-links?resourceId=" + patientMasterSelected.getPatientId();
			}
			out.println("<div>Related Patient Records: <a href=\"" + link + "\">" + link + "</a></div>");
		}
	}

}
