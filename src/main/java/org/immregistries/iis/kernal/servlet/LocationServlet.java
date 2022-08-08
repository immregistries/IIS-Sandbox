package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hl7.fhir.r5.model.Location;
import org.immregistries.iis.kernal.mapping.LocationMapper;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.iis.kernal.repository.FhirRequests;
import org.immregistries.iis.kernal.repository.RepositoryClientFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@SuppressWarnings("serial")
public class LocationServlet extends HttpServlet {
	@Autowired
	RepositoryClientFactory repositoryClientFactory;
	@Autowired
	FhirRequests fhirRequests;

  public static final String PARAM_ACTION = "action";
  public static final String ACTION_ADD = "Add";
  public static final String ACTION_SAVE = "Save";

  public static final String PARAM_ORG_LOCATION_ID = "orgLocationId";

  public static final String PARAM_ORG_FACILITY_CODE = "orgFacilityCode";
  public static final String PARAM_ORG_FACILITY_NAME = "orgFacilityName";
  public static final String PARAM_LOCATION_TYPE = "locationType";
  public static final String PARAM_ADDRESS_LINE1 = "addressLine1";
  public static final String PARAM_ADDRESS_LINE2 = "addressLine2";
  public static final String PARAM_ADDRESS_CITY = "addressCity";
  public static final String PARAM_ADDRESS_STATE = "addressState";
  public static final String PARAM_ADDRESS_ZIP = "addressZip";
  public static final String PARAM_ADDRESS_COUNTRY = "addressCountry";
  public static final String PARAM_ADDRESS_COUNTY_PARISH = "addressCountyParish";
  public static final String PARAM_VFC_PROVIDER_PIN = "vfcProviderPin";



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
    OrgAccess orgAccess = (OrgAccess) session.getAttribute("orgAccess");
    if (orgAccess == null) {
      RequestDispatcher dispatcher = req.getRequestDispatcher("home");
      dispatcher.forward(req, resp);
      return;
    }
	  IGenericClient fhirClient = (IGenericClient) session.getAttribute("fhirClient");

	  resp.setContentType("text/html");
    PrintWriter out = new PrintWriter(resp.getOutputStream());
//    Session dataSession = PopServlet.getDataSession();
    try {
		 OrgLocation orgLocationSelected = null;
		 if (req.getParameter(PARAM_ORG_LOCATION_ID) != null) {
			orgLocationSelected = fhirRequests.searchOrgLocation(fhirClient,
				Location.IDENTIFIER.exactly().identifier(req.getParameter(PARAM_ORG_LOCATION_ID)));
		 }

      String action = req.getParameter(PARAM_ACTION);
      if (action != null) {
        if (action.equals(ACTION_ADD)) {
          String orgFacilityCode = req.getParameter(PARAM_ORG_FACILITY_CODE);
          if (StringUtils.isNotEmpty(orgFacilityCode)) {
            orgLocationSelected = new OrgLocation();
            orgLocationSelected.setOrgFacilityCode(orgFacilityCode);
            orgLocationSelected.setOrgMaster(orgAccess.getOrg());
				Location location = LocationMapper.fhirLocation(orgLocationSelected);
				 try {
					 MethodOutcome outcome = fhirClient.update().resource(location).conditional()
						 .where(Location.IDENTIFIER.exactly().identifier(location.getIdentifierFirstRep().getValue()))
						 .execute();
				 } catch (ResourceNotFoundException e ){
					 MethodOutcome outcome = fhirClient.create().resource(location).execute();
				 }
          }
        } else if (action.equals(ACTION_SAVE)) {
          orgLocationSelected.setOrgFacilityCode(req.getParameter(PARAM_ORG_FACILITY_CODE));
          orgLocationSelected.setOrgFacilityName(req.getParameter(PARAM_ORG_FACILITY_NAME));
          orgLocationSelected.setLocationType(req.getParameter(PARAM_LOCATION_TYPE));
          orgLocationSelected.setAddressLine1(req.getParameter(PARAM_ADDRESS_LINE1));
          orgLocationSelected.setAddressLine2(req.getParameter(PARAM_ADDRESS_LINE2));
          orgLocationSelected.setAddressCity(req.getParameter(PARAM_ADDRESS_CITY));
          orgLocationSelected.setAddressState(req.getParameter(PARAM_ADDRESS_STATE));
          orgLocationSelected.setAddressZip(req.getParameter(PARAM_ADDRESS_ZIP));
          orgLocationSelected.setAddressCountry(req.getParameter(PARAM_ADDRESS_COUNTRY));
          orgLocationSelected.setAddressCountyParish(req.getParameter(PARAM_ADDRESS_COUNTY_PARISH));
          orgLocationSelected.setVfcProviderPin(req.getParameter(PARAM_VFC_PROVIDER_PIN));
			  Location location = LocationMapper.fhirLocation(orgLocationSelected);
			  try {
				  MethodOutcome outcome = fhirClient.update().resource(location).conditional()
					  .where(Location.IDENTIFIER.exactly().identifier(location.getIdentifierFirstRep().getValue()))
					  .execute();
			  } catch (ResourceNotFoundException e ){
				  MethodOutcome outcome = fhirClient.create().resource(location).execute();
			  }
        }
      }

      List<OrgLocation> orgLocationList = null;
      orgLocationList = fhirRequests.searchOrgLocationList(fhirClient);

      HomeServlet.doHeader(out, session);

      out.println("    <h2>" + orgAccess.getOrg().getOrganizationName() + "</h2>");
      if (orgLocationSelected == null) {
        out.println("  <div class=\"w3-container\">");
        {
          if (orgLocationList.size() == 0) {
            out.println("<div class=\"w3-panel w3-yellow\"><p>No Locations Found</p></div>");
          } else {
            out.println(
                "<table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
            out.println("  <tr class=\"w3-green\">");
            out.println("    <th>Code</th>");
            out.println("    <th>Name</th>");
            out.println("    <th>Type</th>");
            out.println("  </tr>");
            out.println("  <tbody>");
            for (OrgLocation orgLocation : orgLocationList) {
              String link =
                  "location?" + PARAM_ORG_LOCATION_ID + "=" + orgLocation.getOrgLocationId();
              out.println("  <tr>");
              out.println("    <td><a href=\"" + link + "\">" + orgLocation.getOrgFacilityCode()
                  + "</a></td>");
              out.println("    <td><a href=\"" + link + "\">" + orgLocation.getOrgFacilityName()
                  + "</a></td>");
              out.println("    <td><a href=\"" + link + "\">" + orgLocation.getLocationType()
                  + "</a></td>");
              out.println("  </tr>");
            }
            out.println("  </tbody>");
            out.println("</table>");

          }
        }
        out.println("  </div>");

        out.println("    <div class=\"w3-container w3-half w3-margin-top\">");
        out.println("    <h3>Add Location</h3>");
        out.println(
            "    <form method=\"POST\" action=\"location\" class=\"w3-container w3-card-4\">");
        out.println("      <label>Facility Code</label>");
        out.println("      <input class=\"w3-input\" type=\"text\" name=\""
            + PARAM_ORG_FACILITY_CODE + "\" value=\"\"/>");
        out.println(
            "          <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\""
                + PARAM_ACTION + "\" value=\"" + ACTION_ADD + "\"/>");
        out.println("    </form>");
        out.println("    </div>");

        out.println("  <div class=\"w3-container\">");


      } else {
        out.println("    <div class=\"w3-container w3-half w3-margin-top\">");
        out.println("    <h3>Add Location</h3>");
        out.println(
            "    <form method=\"POST\" action=\"location\" class=\"w3-container w3-card-4\">");
        out.println("      <label>Facility Code</label>");
        out.println(
            "      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_ORG_FACILITY_CODE
                + "\" value=\"" + orgLocationSelected.getOrgFacilityCode() + "\"/>");
        out.println("      <label>Facility Name</label>");
        out.println(
            "      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_ORG_FACILITY_NAME
                + "\" value=\"" + orgLocationSelected.getOrgFacilityName() + "\"/>");
        out.println("      <label>Location Type</label>");
        out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_LOCATION_TYPE
            + "\" value=\"" + orgLocationSelected.getLocationType() + "\"/>");
        out.println("      <label>Address Line 1</label>");
        out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_ADDRESS_LINE1
            + "\" value=\"" + orgLocationSelected.getAddressLine1() + "\"/>");
        out.println("      <label>Address Line 2</label>");
        out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_ADDRESS_LINE2
            + "\" value=\"" + orgLocationSelected.getAddressLine2() + "\"/>");
        out.println("      <label>Address City</label>");
        out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_ADDRESS_CITY
            + "\" value=\"" + orgLocationSelected.getAddressCity() + "\"/>");
        out.println("      <label>Address State</label>");
        out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_ADDRESS_STATE
            + "\" value=\"" + orgLocationSelected.getAddressState() + "\"/>");
        out.println("      <label>Address Zip</label>");
        out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_ADDRESS_ZIP
            + "\" value=\"" + orgLocationSelected.getAddressZip() + "\"/>");
        out.println("      <label>Address Country</label>");
        out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_ADDRESS_COUNTRY
            + "\" value=\"" + orgLocationSelected.getAddressCountry() + "\"/>");
        out.println("      <label>Address County/Parish</label>");
        out.println(
            "      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_ADDRESS_COUNTY_PARISH
                + "\" value=\"" + orgLocationSelected.getAddressCountyParish() + "\"/>");
        out.println("      <label>VFC Provider PIN</label>");
        out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_VFC_PROVIDER_PIN
            + "\" value=\"" + orgLocationSelected.getVfcProviderPin() + "\"/>");
        out.println("          <input type=\"hidden\" name=\"" + PARAM_ORG_LOCATION_ID
            + "\" value=\"" + orgLocationSelected.getOrgLocationId() + "\"/>");
        out.println(
            "          <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\""
                + PARAM_ACTION + "\" value=\"" + ACTION_SAVE + "\"/>");
        out.println("    </form>");
        out.println("    </div>");

        out.println("  <div class=\"w3-container\">");
      }
    } catch (Exception e) {
      e.printStackTrace(System.err);
    }
    HomeServlet.doFooter(out, session);
    out.flush();
    out.close();
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
      out.println("  <tr>");
      String valueType = observationReported.getValueType();
      if (valueType == null) {
        valueType = "CE";
      }
      out.println("    <td>");
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
      out.println("    </td>");


      out.println("    <td>");
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
        if (observationReported.getValueLabel().equals("")) {
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
        if (observationReported.getValueTable().equals("SCT")
            || observationReported.getValueTable().equals("CDCPHINVS")
            || observationReported.getValueTable().equals("99TPG")) {
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
        out.println("    <td></td>");
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
    out.println("    <td>" + patientReportedSelected.getPatientNameLast() + ", "
        + patientReportedSelected.getPatientNameFirst() + " "
        + patientReportedSelected.getPatientNameMiddle() + "</td>");
    out.println("  </tr>");
    {
      out.println("  <tr>");
      out.println("    <th class=\"w3-green\">Birth Date</th>");
      out.println(
          "    <td>" + sdfDate.format(patientReportedSelected.getPatientBirthDate()) + "</td>");
      out.println("  </tr>");
    }
    out.println("  </tbody>");
    out.println("</table>");
    out.println("</div>");
  }

  @SuppressWarnings("unchecked")
public List<ObservationReported> getObservationList(Session dataSession,
      PatientReported patientReportedSelected) {
    List<ObservationReported> observationReportedList;
    {
      Query query = dataSession.createQuery(
          "from ObservationReported where patientReported = :patientReported and vaccinationReported is null");
      query.setParameter("patientReported", patientReportedSelected);
      observationReportedList = query.list();
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

}
