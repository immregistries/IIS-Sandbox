package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.Immunization;
import org.hl7.fhir.r5.model.Observation;
import org.hl7.fhir.r5.model.Patient;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.logic.CodeMapManager;
import org.immregistries.iis.kernal.mapping.forR5.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.forR5.PatientMapper;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.iis.kernal.repository.FhirRequests;
import org.immregistries.iis.kernal.repository.RepositoryClientFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@SuppressWarnings("serial")
public class VaccinationServlet extends PatientServlet {
	@Autowired
	RepositoryClientFactory repositoryClientFactory;
	@Autowired
	FhirRequests fhirRequests;
	@Autowired
	PatientMapper patientMapper;
	@Autowired
	ImmunizationMapper immunizationMapper;

  public static final String PARAM_ACTION = "action";

  public static final String PARAM_VACCINATION_REPORTED_ID = "vaccinationReportedId";


  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    HttpSession session = req.getSession(true);
    OrgAccess orgAccess = (OrgAccess) session.getAttribute("orgAccess");
	  IGenericClient fhirClient = ServletHelper.getFhirClient(session, repositoryClientFactory);

	  if (orgAccess == null) {
      RequestDispatcher dispatcher = req.getRequestDispatcher("home");
      dispatcher.forward(req, resp);
      return;
    }

    resp.setContentType("text/html");
    PrintWriter out = new PrintWriter(resp.getOutputStream());
    try {
		 VaccinationReported vaccinationReported =
			 immunizationMapper.getReported(fhirClient.read().resource(Immunization.class)
				 .withId(req.getParameter(PARAM_VACCINATION_REPORTED_ID)).execute());

//			 fhirRequests.searchVaccinationReported(fhirClient,
//			 Immunization.IDENTIFIER.exactly().code(req.getParameter(PARAM_VACCINATION_REPORTED_ID)));

      String action = req.getParameter(PARAM_ACTION);
      if (action != null) {
      }

      HomeServlet.doHeader(out, session);

      out.println("    <h2>" + orgAccess.getOrg().getOrganizationName() + "</h2>");
      PatientReported patientReportedSelected = patientMapper.getReported(fhirClient.read().resource(Patient.class).withId(vaccinationReported.getPatientReportedId()).execute());

      {
        printPatient(out, patientReportedSelected);
        SimpleDateFormat sdfDate = new SimpleDateFormat("MM/dd/yyyy");
        out.println("  <div class=\"w3-container\">");
        out.println("<h4>Vaccination</h4>");
        {
          CodeMap codeMap = CodeMapManager.getCodeMap();
          out.println(
              "<table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
          out.println("  <tr class=\"w3-green\">");
          out.println("    <th>Vaccine</th>");
          out.println("    <th>Admin Date</th>");
          out.println("    <th>Manufacturer</th>");
          out.println("    <th>Lot Number</th>");
          out.println("    <th>Information</th>");
          out.println("    <th>Action</th>");
          out.println("  </tr>");
          out.println("  <tbody>");
          {
            out.println("  <tr>");
            out.println("    <td>");
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

        List<ObservationReported> observationReportedList =
            getObservationList(fhirClient, vaccinationReported);

        if (observationReportedList.size() != 0) {
          out.println("<h4>Observations</h4>");
          printObservations(out, observationReportedList);
        }
        out.println("  </div>");

      }
    } catch (Exception e) {
      e.printStackTrace(System.err);
    }
    HomeServlet.doFooter(out, session);
    out.flush();
    out.close();
  }

  @SuppressWarnings("unchecked")
public List<ObservationReported> getObservationList(IGenericClient fhirClient,
      VaccinationReported vaccinationReported) {
    List<ObservationReported> observationReportedList;
    {
		 observationReportedList = fhirRequests.searchObservationReportedList(
                 Observation.PATIENT.hasId(vaccinationReported.getVaccinationReportedId())
		 );
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
