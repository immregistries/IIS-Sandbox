package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.*;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.logic.CodeMapManager;
import org.immregistries.iis.kernal.mapping.Interfaces.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.Interfaces.PatientMapper;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.iis.kernal.InternalClient.FhirRequester;
import org.immregistries.iis.kernal.InternalClient.RepositoryClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;


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
	FhirRequester fhirRequester;
	@Autowired
	PatientMapper patientMapper;
	@Autowired
	ImmunizationMapper<Immunization> immunizationMapper;

	public static final String PARAM_ACTION = "action";
	public static final String PARAM_RESOURCE = "resource";

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
    OrgAccess orgAccess = ServletHelper.getOrgAccess();
	  IGenericClient fhirClient = repositoryClientFactory.newGenericClient(session);

	 if (orgAccess == null) {
//      RequestDispatcher dispatcher = req.getRequestDispatcher("home");
//      dispatcher.forward(req, resp);
//      return;
		 throw new AuthenticationCredentialsNotFoundException("");
    }

    resp.setContentType("text/html");
    PrintWriter out = new PrintWriter(resp.getOutputStream());
    try {
		 VaccinationReported vaccinationReported = fhirRequester.readVaccinationReported(req.getParameter(PARAM_VACCINATION_REPORTED_ID));
//			 fhirRequests.searchVaccinationReported(fhirClient,
//			 Immunization.IDENTIFIER.exactly().code(req.getParameter(PARAM_VACCINATION_REPORTED_ID)));

		 String action = req.getParameter(PARAM_ACTION);
		 if (action != null) {
		 }

		 HomeServlet.doHeader(out, "IIS Sandbox - Vaccinations");

		 out.println("<h2>Facility : " + orgAccess.getOrg().getOrganizationName() + "</h2>");
		 PatientReported patientReportedSelected = fhirRequester.readPatientReported(vaccinationReported.getPatientReportedId());
		 {
			 printPatient(out, patientReportedSelected);
			 SimpleDateFormat sdfDate = new SimpleDateFormat("MM/dd/yyyy");
			 out.println("  <div class=\"w3-container\">");
			 out.println("<h4>Vaccination</h4>");
			 {
				 CodeMap codeMap = CodeMapManager.getCodeMap();
				 out.println("<table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
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

			Bundle bundle = fhirClient.search().forResource(Subscription.class).returnBundle(Bundle.class).execute();

			Immunization immunization = immunizationMapper.getFhirResource(vaccinationReported);
			/**
			 * Setting external identifier in reference
			 */
			immunization.getPatient().setIdentifier(new Identifier()
				.setValue(vaccinationReported.getPatientReported().getPatientReportedExternalLink())
				.setSystem(vaccinationReported.getPatientReported().getPatientReportedAuthority()));
			IParser parser = repositoryClientFactory.getFhirContext().newJsonParser().setPrettyPrint(true);

			printSubscriptions(out, parser, bundle, immunization);

		}
    } catch (Exception e) {
      e.printStackTrace(System.err);
    }
    HomeServlet.doFooter(out);
    out.flush();
    out.close();
  }

  @SuppressWarnings("unchecked")
public List<ObservationReported> getObservationList(IGenericClient fhirClient,
      VaccinationReported vaccinationReported) {
    List<ObservationReported> observationReportedList;
    {
		 observationReportedList = fhirRequester.searchObservationReportedList(
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
