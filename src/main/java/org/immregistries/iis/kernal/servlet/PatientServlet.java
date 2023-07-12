package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hl7.fhir.r5.model.*;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.logic.CodeMapManager;
import org.immregistries.iis.kernal.mapping.Interfaces.PatientMapper;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.iis.kernal.InternalClient.FhirRequester;
import org.immregistries.iis.kernal.InternalClient.RepositoryClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.immregistries.iis.kernal.servlet.RecommendationServlet.PARAM_RECOMMENDATION_ID;
import static org.immregistries.iis.kernal.servlet.SubscriptionServlet.PARAM_MESSAGE;
import static org.immregistries.iis.kernal.servlet.SubscriptionServlet.PARAM_SUBSCRIPTION_ID;

public class PatientServlet extends HttpServlet {
	Logger logger = LoggerFactory.getLogger(PatientServlet.class);
	@Autowired
	RepositoryClientFactory repositoryClientFactory;
	@Autowired
	FhirRequester fhirRequester;
	@Autowired
	PatientMapper patientMapper;

	public static final String PARAM_ACTION = "action";
	public static final String ACTION_SEARCH = "search";

	public static final String PARAM_PATIENT_NAME_LAST = "patientNameLast";
	public static final String PARAM_PATIENT_NAME_FIRST = "patientNameFirst";
	public static final String PARAM_PATIENT_REPORTED_EXTERNAL_LINK = "patientIdentifier";
	public static final String PARAM_PATIENT_REPORTED_ID = "patientId";


	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		doGet(req, resp);
	}

  @SuppressWarnings("unchecked")
	@Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    HttpSession session = req.getSession(true);
    OrgAccess orgAccess = ServletHelper.getOrgAccess();
    if (orgAccess == null) {
//      RequestDispatcher dispatcher = req.getRequestDispatcher("home");
//      dispatcher.forward(req, resp);
//      return;
		 throw new AuthenticationCredentialsNotFoundException("");
    }

    resp.setContentType("text/html");
    PrintWriter out = new PrintWriter(resp.getOutputStream());
	 Session dataSession = PopServlet.getDataSession();
	 IGenericClient fhirClient = repositoryClientFactory.newGenericClient(session);
    try {
      String patientNameLast = req.getParameter(PARAM_PATIENT_NAME_LAST);
      String patientNameFirst = req.getParameter(PARAM_PATIENT_NAME_FIRST);
      String patientReportedExternalLink = req.getParameter(PARAM_PATIENT_REPORTED_EXTERNAL_LINK);

      List<PatientReported> patientReportedList = null;
      String action = req.getParameter(PARAM_ACTION);
      if (action != null) {
        if (action.equals(ACTION_SEARCH)) {
			  patientReportedList = fhirRequester.searchPatientReportedGoldenList(
				  Patient.FAMILY.matches().value(patientNameLast),
				  Patient.NAME.matches().value(patientNameFirst),
				  Patient.IDENTIFIER.exactly().code(patientReportedExternalLink)
			  );
        }
      }

      if (patientNameLast == null) {
        patientNameLast = "";
      }
      if (patientNameFirst == null) {
        patientNameFirst = "";
      }
      if (patientReportedExternalLink == null) {
        patientReportedExternalLink = "";
      }

		 HomeServlet.doHeader(out, "IIS Sandbox - Patients");

      PatientReported patientReportedSelected = getPatientReportedFromParameter(req, fhirClient);

      if (patientReportedSelected == null) {
		  out.println("<h2>Patients from Facility : " + orgAccess.getOrg().getOrganizationName() + "</h2>");
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
            + PARAM_PATIENT_REPORTED_EXTERNAL_LINK + "\" value=\"" + patientReportedExternalLink
            + "\"/>");
        out.println("      <label>Medical Record Number</label><br/>");
        out.println("      <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\""
                + PARAM_ACTION + "\" value=\"" + ACTION_SEARCH + "\"/>");
        out.println("    </form>");
        out.println("</div>");

        out.println("<div class=\"w3-container\">");

        boolean showingRecent = false;
        if (patientReportedList == null) {
			  showingRecent = true;
			  patientReportedList = fhirRequester.searchPatientReportedList(); // TODO Paging ?
		  }

        if (patientReportedList != null) {
          if (patientReportedList.size() == 0) {
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
            for (PatientReported patientReported : patientReportedList) {
              count++;
              if (count > 100) {
                break;
              }
				  String link = "patient?" + PARAM_PATIENT_REPORTED_EXTERNAL_LINK + "="
						+ patientReported.getPatientReportedExternalLink();
              out.println("  <tr>");
              out.println("    <td><a href=\"" + link + "\">"
                  + patientReported.getPatientReportedExternalLink() + "</a></td>");
					out.println("    <td><a href=\"" + link + "\">" + patientReported.getNameLast()
						+ "</a></td>");
					out.println("    <td><a href=\"" + link + "\">"
						+ patientReported.getNameFirst() + "</a></td>");
					out.println("    <td><a href=\"" + link + "\">"
						+ sdf.format(patientReported.getUpdatedDate()) + "</a></td>");
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
      } else {

			IParser parser = repositoryClientFactory.getFhirContext()
				.newJsonParser().setPrettyPrint(true).setSuppressNarratives(true);
			Patient patientSelected = (Patient) patientMapper.getFhirResource(patientReportedSelected);
			out.println("<h2>Patient : " + patientSelected.getNameFirstRep().getNameAsSingleString() + "</h2>");
			{
				printPatient(out, patientReportedSelected);
			}
			{
				printVaccinationList(req, resp, out, fhirClient, patientReportedSelected.getId());
			}
			{
				List<ObservationReported> observationReportedList =
					getObservationList(fhirClient, patientReportedSelected);
				if (observationReportedList.size() != 0) {
					out.println("<h4>Patient Observations</h4>");
					printObservations(out, observationReportedList);
				}
				out.println("  </div>");
			}
			{
				Bundle recommendationBundle = fhirClient.search()
					.forResource(ImmunizationRecommendation.class)
					.where(ImmunizationRecommendation.PATIENT
							.hasId(patientSelected.getId())
//						.hasChainedProperty(Patient.IDENTIFIER.exactly()
//							.systemAndCode(patientReportedSelected.getPatientReportedAuthority(),patientReportedSelected.getPatientReportedExternalLink()))
					).returnBundle(Bundle.class).execute();
				if (recommendationBundle.hasEntry()) {
					printRecommendation(out, (ImmunizationRecommendation) recommendationBundle.getEntryFirstRep().getResource(), patientSelected);
				} else  {
					printRecommendation(out, null, patientSelected);
				}
//				out.println("<form method=\"GET\" action=\"recommendation\">");
//				out.println("	<input type=\"hidden\" name=\""
//					+ PARAM_PATIENT_REPORTED_EXTERNAL_LINK + "\" value=\"" + patientReportedExternalLink
//					+ "\"/>");
//				out.println("	<input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"\" value=\"Vaccination Recommendations\"/>");
//				out.println("</form>");
			}

			{
				Bundle subcriptionBundle = fhirClient.search().forResource(Subscription.class).returnBundle(Bundle.class).execute();
				printSubscriptions(out, parser, subcriptionBundle, patientSelected);
			}
			{
				out.println("<div class=\"w3-container\">");
				out.println("<h3>Messages Received</h3>");
				Query query = dataSession.createQuery(
					"from MessageReceived where patientReportedId = :patientReportedId order by reportedDate asc");
				query.setParameter("patientReportedId", patientReportedSelected.getId());
				List<MessageReceived> messageReceivedList = query.list();
				if (messageReceivedList.size() == 0) {
					out.println("<div class=\"w3-panel w3-yellow\"><p>No Messages Received</p></div>");
				} else {
					for (MessageReceived messageReceived : messageReceivedList) {
						printMessageReceived(out, messageReceived);
					}
				}
			}

			String apiBaseUrl = "/iis/fhir/" + orgAccess.getAccessName();
        {
			  String link = apiBaseUrl + "/Patient/"
				  + patientReportedSelected.getId();
			  out.println("<a href=\"" + link + "\">FHIR Resource</a>");
        }
//		  {
//          String link = apiBaseUrl + "/Patient?identifier="
//	              + patientReportedSelected.getPatientReportedExternalLink();
//          out.println("<a href=\"" + link + "\">all FHIR Resources</a>");
//        }
		  {
          String link = apiBaseUrl + "/Patient/" + patientReportedSelected.getId() +
				 "/$everything?_mdm=true";
          out.println("<a href=\"" + link + "\">Everything related to Patient</a>");
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
		List<VaccinationReported> vaccinationReportedList = null;
		{
			vaccinationReportedList = fhirRequester.searchVaccinationReportedList(
				Immunization.PATIENT.hasId(patientId)
			);
		}
		if (vaccinationReportedList.size() == 0) {
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
			for (VaccinationReported vaccinationReported : vaccinationReportedList) {
				out.println("  <tr>");
				out.println("    <td>");
				String link = "vaccination?" + VaccinationServlet.PARAM_VACCINATION_REPORTED_ID + "="
					+ vaccinationReported.getVaccinationReportedId();
				out.println("      <a href=\"" + link + "\">");
				if (!StringUtils.isEmpty(vaccinationReported.getVaccineCvxCode())) {
					Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE,
						vaccinationReported.getVaccineCvxCode());
					if (cvxCode == null) {
						out.println("Unknown CVX (" + vaccinationReported.getVaccineCvxCode() + ")");
					} else {
						out.println(
							cvxCode.getLabel() + " (" + vaccinationReported.getVaccineCvxCode() + ")");
					}
				}
				out.println("      </a>");
				out.println("    </td>");
				out.println("    <td>");
				if (vaccinationReported.getAdministeredDate() == null) {
					out.println("null");
				} else {
					out.println(sdfDate.format(vaccinationReported.getAdministeredDate()));
				}
				out.println("    </td>");
				out.println("    <td>");
				if (!StringUtils.isEmpty(vaccinationReported.getVaccineMvxCode())) {
					Code mvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_MANUFACTURER_CODE,
						vaccinationReported.getVaccineMvxCode());
					if (mvxCode == null) {
						out.print("Unknown MVX");
					} else {
						out.print(mvxCode.getLabel());
					}
					out.println(" (" + vaccinationReported.getVaccineMvxCode() + ")");
				}
				out.println("    </td>");
				out.println("    <td>" + vaccinationReported.getLotnumber() + "</td>");
				out.println("    <td>");
				if (!StringUtils.isEmpty(vaccinationReported.getInformationSource())) {
					Code informationCode =
						codeMap.getCodeForCodeset(CodesetType.VACCINATION_INFORMATION_SOURCE,
							vaccinationReported.getInformationSource());
					if (informationCode != null) {
						out.print(informationCode.getLabel());
						out.println(" (" + vaccinationReported.getInformationSource() + ")");
					}
				}
				out.println("    </td>");
				out.println("    <td>");
				if (!StringUtils.isEmpty(vaccinationReported.getCompletionStatus())) {
					Code completionCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_COMPLETION,
						vaccinationReported.getCompletionStatus());
					if (completionCode != null) {
						out.print(completionCode.getLabel());
						out.println(" (" + vaccinationReported.getCompletionStatus() + ")");
					}
				}
				out.println("    <td>");
				if (!StringUtils.isEmpty(vaccinationReported.getActionCode())) {
					Code actionCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_ACTION_CODE,
						vaccinationReported.getActionCode());
					if (actionCode != null) {
						out.print(actionCode.getLabel());
						out.println(" (" + vaccinationReported.getActionCode() + ")");
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

  public void printPatient(PrintWriter out, PatientReported patientReportedSelected) {
	  SimpleDateFormat sdfDate = new SimpleDateFormat("MM/dd/yyyy");
	  out.println("    <div class=\"w3-container w3-half w3-margin-top\">");
	  out.println("<table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
	  out.println("  <tbody>");
	  out.println("  <tr>");
	  out.println("    <th class=\"w3-green\">External Id (MRN)</th>");
	  out.println("    <td>" + patientReportedSelected.getPatientReportedExternalLink() + "</td>");
	  out.println("  </tr>");
	  out.println("  <tr>");
	  out.println("    <th class=\"w3-green\">Patient Name</th>");
	  out.println("    <td>" + patientReportedSelected.getNameLast() + ", "
		  + patientReportedSelected.getNameFirst() + " "
		  + patientReportedSelected.getNameMiddle() + "</td>");
	  out.println("  </tr>");
	  {
		  out.println("  <tr>");
		  out.println("    <th class=\"w3-green\">Birth Date</th>");
		  out.println(
			  "    <td>" + sdfDate.format(patientReportedSelected.getBirthDate()) + "</td>");
		  out.println("  </tr>");
	  }
	  out.println("  </tbody>");
	  out.println("</table>");
    out.println("</div>");
  }

	public List<ObservationReported> getObservationList(IGenericClient fhirClient, PatientReported patientReportedSelected) {
    List<ObservationReported> observationReportedList = new ArrayList<>();
    {
		 observationReportedList = fhirRequester.searchObservationReportedList(
			 Observation.SUBJECT.hasId(patientReportedSelected.getId()));
//		 observationReportedList = observationReportedList.stream().filter(observationReported -> observationReported.getVaccinationReported() == null).collect(Collectors.toList());
      Set<String> suppressSet = LoincIdentifier.getSuppressIdentifierCodeSet();
      for (Iterator<ObservationReported> it = observationReportedList.iterator(); it.hasNext();) {
        ObservationReported observationReported = it.next();
        if (suppressSet.contains(observationReported.getIdentifierCode())) {
          it.remove();
        }
      }
    }
    return observationReportedList;
}

	public static void printMessageReceived(PrintWriter out, MessageReceived messageReceived) {
		SimpleDateFormat sdfTime = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		out.println("     <h3>" + messageReceived.getCategoryRequest() + " - "
			+ messageReceived.getCategoryResponse() + " "
			+ sdfTime.format(messageReceived.getReportedDate()) + "</h3>");
		out.println("     <pre>" + messageReceived.getMessageRequest() + "</pre>");
		out.println("     <pre>" + messageReceived.getMessageResponse() + "</pre>");
	}

	public void printSubscriptions(PrintWriter out, IParser parser, Bundle bundle, Resource resource) {
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
			for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
				Subscription subscription = (Subscription) entry.getResource();
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

	public void printRecommendation(PrintWriter out, ImmunizationRecommendation recommendation, Patient patient) {
		out.println("<div class=\"w3-container\">");
		out.println("<h3>Recommendations</h3>");
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
			for (ImmunizationRecommendation.ImmunizationRecommendationRecommendationComponent component: recommendation.getRecommendation()) {
				count++;
				if (count > 100) {
					break;
				}
				String link = "recommendation?" + PARAM_RECOMMENDATION_ID + "="
					+ new IdType(recommendation.getId()).getIdPart();
				out.println("<tr>");
				out.println("    <td><a href=\"" + link + "\">" + component.getVaccineCodeFirstRep().getCodingFirstRep().getCode() + "</a></td>");
				out.println("    <td><a href=\"" + link + "\">" + component.getDateCriterionFirstRep().getValue() + "</a></td>");
				out.println("    <td><a href=\"" + link + "\">" + component.getDateCriterionFirstRep().getCode().getCodingFirstRep().getDisplay() + "</a></td>");
				out.println("</tr>");
			}
			out.println("</tbody>");
			out.println("</table>");

			out.println("<form action=\"recommendation\" method=\"POST\">");
			out.println("	<input type=\"hidden\" name=\"" + PARAM_PATIENT_REPORTED_ID + "\" value=\"" + new IdType(patient.getId()).getIdPart() + "\"/>");
			out.println("	<input type=\"hidden\" name=\"" + PARAM_RECOMMENDATION_ID + "\" value=\"" + new IdType(recommendation.getId()).getIdPart() + "\"/>");
			out.println("	<input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"submit\" value=\"Add recommendation component\"/>");
			out.println("</form>");
		} else {
			out.println("<div class=\"w3-panel w3-yellow\"><p>No Recommendation Found</p></div>");
			out.println("<form action=\"recommendation\" method=\"POST\">");
			out.println("	<input type=\"hidden\" name=\"" + PARAM_PATIENT_REPORTED_ID + "\" value=\"" + new IdType(patient.getId()).getIdPart() + "\"/>");
			out.println("	<input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"submit\" value=\"Generate new recommendation\"/>");
			out.println("</form>");
		}
		out.println("</div>");
	}

	protected Patient getPatientFromParameter(HttpServletRequest req, IGenericClient fhirClient) {
		Patient patient = null;
		if (req.getParameter(PARAM_PATIENT_REPORTED_ID) != null) {
			patient = fhirClient.read().resource(Patient.class).withId(req.getParameter(PARAM_PATIENT_REPORTED_ID)).execute();
		} else if (req.getParameter(PARAM_PATIENT_REPORTED_EXTERNAL_LINK) != null) {
			Bundle patientBundle = (Bundle) fhirRequester.searchGoldenRecord(Patient.class, //TODO choose priority golden or regular
				Patient.IDENTIFIER.exactly().identifier(req.getParameter(PARAM_PATIENT_REPORTED_EXTERNAL_LINK)));
			if (patientBundle.hasEntry()) {
				patient = (Patient) patientBundle.getEntryFirstRep().getResource();
			} else {
				patientBundle = (Bundle) fhirRequester.searchRegularRecord(Patient.class,
					Patient.IDENTIFIER.exactly().identifier(req.getParameter(PARAM_PATIENT_REPORTED_EXTERNAL_LINK)));
				if (patientBundle.hasEntry()) {
					patient = (Patient) patientBundle.getEntryFirstRep().getResource();
				}
			}
		}
		return patient;
	}

	protected PatientReported getPatientReportedFromParameter(HttpServletRequest req, IGenericClient fhirClient) {
		Patient patient = getPatientFromParameter(req, fhirClient);
		if (patient != null) {
			return patientMapper.getReportedWithMaster(patient);
		} else {
			return null;
		}
	}

}
