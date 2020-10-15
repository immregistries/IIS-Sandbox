package org.immregistries.iis.kernal.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.immregistries.iis.kernal.SoftwareVersion;
import org.immregistries.iis.kernal.logic.IncomingMessageHandler;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgLocation;
import org.immregistries.iis.kernal.model.OrgMaster;
import org.immregistries.iis.kernal.model.Person;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.smm.transform.ScenarioManager;
import org.immregistries.smm.transform.TestCaseMessage;
import org.immregistries.smm.transform.Transformer;
import org.immregistries.vfa.connect.model.TestEvent;

@SuppressWarnings("serial")
public class EventServlet extends PopServlet {

  public static final String PARAM_USERID = "USERID";
  public static final String PARAM_PASSWORD = "PASSWORD";
  public static final String PARAM_FACILITYID = "FACILITYID";

  public static final String[] PARAMS_PATIENT =
      new String[] {"patientReportedExternalLink", "patientReportedAuthority",
          "patientReportedType", "patientNameLast", "patientNameFirst", "patientNameMiddle",
          "patientMotherMaiden", "patientBirthDate", "patientSex", "patientRace", "patientRace2",
          "patientRace3", "patientRace4", "patientRace5", "patientRace6", "patientAddressLine1",
          "patientAddressLine2", "patientAddressCity", "patientAddressState", "patientAddressZip",
          "patientAddressCountry", "patientAddressCountyParish", "patientPhone", "patientEmail",
          "patientEthnicity", "patientBirthFlag", "patientBirthOrder", "patientDeathFlag",
          "patientDeathDate", "publicityIndicator", "publicityIndicatorDate", "protectionIndicator",
          "protectionIndicatorDate", "registryStatusIndicator", "registryStatusIndicatorDate",
          "guardianLast", "guardianFirst", "guardianMiddle", "guardianRelationship"};

  public static final String[] PARAMS_VACCINATION = new String[] {"vaccinationReportedExternalLink",
      "administeredDate", "vaccineCvxCode", "vaccineNdcCode", "vaccineMvxCode = ",
      "administeredAmount", "informationSource", "lotnumber", "expirationDate", "completionStatus",
      "actionCode", "refusalReasonCode", "bodySite", "bodyRoute", "fundingSource",
      "fundingEligibility", "orgLocationFacilityCode"};

  private static SessionFactory factory;

  public static Session getDataSession() {
    if (factory == null) {
      factory = new AnnotationConfiguration().configure().buildSessionFactory();
    }
    return factory.openSession();
  }


  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    resp.setContentType("text/html");
    PrintWriter out = new PrintWriter(resp.getOutputStream());
    try {
      String userId = req.getParameter(PARAM_USERID);
      String password = req.getParameter(PARAM_PASSWORD);
      String facilityId = req.getParameter(PARAM_FACILITYID);
      HttpSession session = req.getSession(true);
      OrgAccess orgAccess = (OrgAccess) session.getAttribute("orgAccess");
      String ack = "";
      Session dataSession = getDataSession();
      try {
        if (orgAccess == null) {
          orgAccess = authenticateOrgAccess(userId, password, facilityId, dataSession);
        }
        if (orgAccess == null) {
          resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          out.println(
              "Access is not authorized. Facilityid, userid and/or password are not recognized. ");
        } else {

          session.setAttribute("orgAccess", orgAccess);
        }
      } finally {
        dataSession.close();
      }
      resp.setContentType("text/plain");
      out.print(ack);
    } catch (Exception e) {
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      e.printStackTrace(out);
      e.printStackTrace(System.err);
    }
    out.flush();
    out.close();
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    HttpSession session = req.getSession(true);
    resp.setContentType("text/html");
    PrintWriter out = new PrintWriter(resp.getOutputStream());
    try {

      String userId = req.getParameter(PARAM_USERID);
      if (userId == null || userId.equals("")) {
        userId = "Mercy";
      }
      String password = req.getParameter(PARAM_PASSWORD);
      if (password == null || password.equals("")) {
        password = "password1234";
      }
      String facilityId = req.getParameter(PARAM_FACILITYID);
      if (facilityId == null || facilityId.equals("")) {
        facilityId = "Mercy Healthcare";
      }
      {
        HomeServlet.doHeader(out, session);
        out.println("    <h2>Send Now</h2>");
        out.println("    <form action=\"pop\" method=\"POST\" target=\"_blank\">");
        out.println("    <div class=\"w3-container w3-half w3-margin-top\">");
        out.println("    <div class=\"w3-container w3-card-4\">");
        out.println("      <h3>Authentication</h3>");
        OrgAccess orgAccess = (OrgAccess) session.getAttribute("orgAccess");
        if (orgAccess == null) {
          out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_USERID
              + "\" value=\"" + userId + "\"/>");
          out.println("      <label>User Id</label>");
          out.println("      <input class=\"w3-input\" type=\"password\" name=\"" + PARAM_PASSWORD
              + "\"/>");
          out.println("      <label>Password</label>");
          out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_FACILITYID
              + "\" value=\"" + facilityId + "\"/>");
          out.println("      <label>Facility Id</label>");
        } else {
          out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_USERID
              + "\" value=\"" + userId + "\"/ disabled>");
          out.println("      <label>User Id</label>");
          out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_FACILITYID
              + "\" value=\"" + facilityId + "\" disabled/>");
          out.println("      <label>Facility Id</label>");
        }
        out.println("      <h3>Patient</h3>");
        for (String s : PARAMS_PATIENT) {
          out.println("      <label>" + s + "</label>");
          out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + s + "\"/>");
        }
        out.println("");
        out.println("      <h3>Vaccination</h3>");
        for (String s : PARAMS_VACCINATION) {
          out.println("      <label>" + s + "</label>");
          out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + s + "\"/>");
        }
        out.println("      <br/>");
        out.println(
            "      <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"sumbit\" value=\"Submit\"/>");
        out.println("    </div>");
        out.println("    </div>");
        out.println("    </form>");
        HomeServlet.doFooter(out, session);
      }
    } catch (Exception e) {
      e.printStackTrace(System.err);
    }
    out.flush();
    out.close();
  }

}
