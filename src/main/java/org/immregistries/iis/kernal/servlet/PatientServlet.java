package org.immregistries.iis.kernal.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.logic.CodeMapManager;
import org.immregistries.iis.kernal.model.MessageReceived;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.VaccinationReported;

@SuppressWarnings("serial")
public class PatientServlet extends HttpServlet {

  public static final String PARAM_ACTION = "action";
  public static final String ACTION_SEARCH = "search";

  public static final String PARAM_PATIENT_NAME_LAST = "patientNameLast";
  public static final String PARAM_PATIENT_NAME_FIRST = "patientNameFirst";
  public static final String PARAM_PATIENT_REPORTED_EXTERNAL_LINK = "patientReportedExternalLink";

  public static final String PARAM_PATIENT_REPORTED_ID = "patientReportedId";


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
    if (orgAccess == null) {
      RequestDispatcher dispatcher = req.getRequestDispatcher("home");
      dispatcher.forward(req, resp);
      return;
    }

    resp.setContentType("text/html");
    PrintWriter out = new PrintWriter(resp.getOutputStream());
    Session dataSession = PopServlet.getDataSession();
    try {
      String patientNameLast = req.getParameter(PARAM_PATIENT_NAME_LAST);
      String patientNameFirst = req.getParameter(PARAM_PATIENT_NAME_FIRST);
      String patientReportedExternalLink = req.getParameter(PARAM_PATIENT_REPORTED_EXTERNAL_LINK);

      List<PatientReported> patientReportedList = null;
      String action = req.getParameter(PARAM_ACTION);
      if (action != null) {
        if (action.equals(ACTION_SEARCH)) {
          Query query = dataSession
              .createQuery("from PatientReported where patientNameLast like :patientNameLast "
                  + "and patientNameFirst like :patientNameFirst "
                  + "and patientReportedExternalLink like :patientReportedExternalLink "
                  + "and orgReported = :orgReported");
          query.setParameter("patientNameLast", patientNameLast + "%");
          query.setParameter("patientNameFirst", patientNameFirst + "%");
          query.setParameter("patientReportedExternalLink", patientReportedExternalLink + "%");
          query.setParameter("orgReported", orgAccess.getOrg());
          patientReportedList = query.list();
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

      HomeServlet.doHeader(out, session);

      out.println("    <h2>" + orgAccess.getOrg().getOrganizationName() + "</h2>");
      PatientReported patientReportedSelected = null;
      if (req.getParameter(PARAM_PATIENT_REPORTED_ID) != null) {
        patientReportedSelected = (PatientReported) dataSession.get(PatientReported.class,
            Integer.parseInt(req.getParameter(PARAM_PATIENT_REPORTED_ID)));
      }

      if (patientReportedSelected == null) {

        out.println("    <div class=\"w3-container w3-half w3-margin-top\">");
        out.println("    <h3>Search Patient Registry</h3>");
        out.println(
            "    <form method=\"GET\" action=\"patient\" class=\"w3-container w3-card-4\">");
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
        out.println(
            "          <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\""
                + PARAM_ACTION + "\" value=\"" + ACTION_SEARCH + "\"/>");
        out.println("    </form>");
        out.println("    </div>");

        out.println("  <div class=\"w3-container\">");

        boolean showingRecent = false;
        if (patientReportedList == null) {
          showingRecent = true;
          Query query = dataSession.createQuery("from PatientReported where orgReported = :orgReported "
              + "order by updatedDate desc ");
          query.setParameter("orgReported", orgAccess.getOrg());
          patientReportedList = query.list();
        }

        if (patientReportedList != null) {
          if (patientReportedList.size() == 0) {
            out.println("<div class=\"w3-panel w3-yellow\"><p>No Records Found</p></div>");
          } else {
            if (showingRecent) {
              out.println("<h4>Recent Updates</h4>");
            }
            out.println(
                "<table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
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
              String link = "patient?" + PARAM_PATIENT_REPORTED_ID + "="
                  + patientReported.getPatientReportedId();
              out.println("  <tr>");
              out.println("    <td><a href=\"" + link + "\">"
                  + patientReported.getPatientReportedExternalLink() + "</a></td>");
              out.println("    <td><a href=\"" + link + "\">" + patientReported.getPatientNameLast()
                  + "</a></td>");
              out.println("    <td><a href=\"" + link + "\">"
                  + patientReported.getPatientNameFirst() + "</a></td>");
              out.println("    <td><a href=\"" + link + "\">"
                  + sdf.format(patientReported.getUpdatedDate()) + "</a></td>");
              out.println("  </tr>");
            }
            out.println("  </tbody>");
            out.println("</table>");

            if (count > 100) {
              out.println("<em>Only the first 100 are shown</em>");
            }
          }
        }
        out.println("  </div>");
      } else {
        SimpleDateFormat sdfDate = new SimpleDateFormat("MM/dd/yyyy");
        out.println("    <div class=\"w3-container w3-half w3-margin-top\">");
        out.println(
            "<table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
        out.println("  <tbody>");
        out.println("  <tr>");
        out.println("    <th class=\"w3-green\">External Id (MRN)</th>");
        out.println(
            "    <td>" + patientReportedSelected.getPatientReportedExternalLink() + "</td>");
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
        out.println("  <div class=\"w3-container\">");
        out.println("<h4>Vaccinations</h4>");
        List<VaccinationReported> vaccinationReportedList;
        {
          Query query = dataSession
              .createQuery("from VaccinationReported where patientReported = :patientReported");
          query.setParameter("patientReported", patientReportedSelected);
          vaccinationReportedList = query.list();
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
          out.println("    <th>Action</th>");
          out.println("  </tr>");
          out.println("  <tbody>");
          for (VaccinationReported vaccinationReported : vaccinationReportedList) {
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
        out.println("  </div>");

        out.println("  <div class=\"w3-container\">");

        out.println("<h3>Messages Received</h3>");
        Query query = dataSession.createQuery(
            "from MessageReceived where patientReported = :patientReported order by reportedDate asc");
        query.setParameter("patientReported", patientReportedSelected);
        List<MessageReceived> messageReceivedList = query.list();
        if (messageReceivedList.size() == 0) {
          out.println("<div class=\"w3-panel w3-yellow\"><p>No Messages Received</p></div>");
        } else {
          for (MessageReceived messageReceived : messageReceivedList) {
            printMessageReceived(out, messageReceived);
          }
        }
        out.println("  </div>");
      }
    } catch (Exception e) {
      e.printStackTrace(System.err);
    } finally {
      dataSession.close();
    }
    HomeServlet.doFooter(out, session);
    out.flush();
    out.close();
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
