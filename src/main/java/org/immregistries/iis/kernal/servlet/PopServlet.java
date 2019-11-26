package org.immregistries.iis.kernal.servlet;

import java.io.IOException;
import java.io.PrintWriter;
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
import org.immregistries.iis.kernal.model.OrgMaster;
import org.immregistries.smm.transform.ScenarioManager;
import org.immregistries.smm.transform.TestCaseMessage;
import org.immregistries.smm.transform.Transformer;

@SuppressWarnings("serial")
public class PopServlet extends HttpServlet {

  public static final String PARAM_MESSAGE = "MESSAGEDATA";
  public static final String PARAM_USERID = "USERID";
  public static final String PARAM_PASSWORD = "PASSWORD";
  public static final String PARAM_FACILITYID = "FACILITYID";

  private static SessionFactory factory;

  public static Session getDataSession() {
    if (factory == null) {
      factory = new AnnotationConfiguration().configure().buildSessionFactory();
    }
    return factory.openSession();
  }

  private static final String EXAMPLE_HL7 =
      "MSH|^~\\&|Test EHR Application|X68||NIST Test Iz Reg|20120701082240-0500||VXU^V04^VXU_V04|NIST-IZ-001.00|P|2.5.1|||ER|AL|||||Z22^CDCPHINVS\n"
          + "PID|1||D26376273^^^NIST MPI^MR||Snow^Madelynn^Ainsley^^^^L|Lam^Morgan^^^^^M|20070706|F||2076-8^Native Hawaiian or Other Pacific Islander^CDCREC|32 Prescott Street Ave^^Warwick^MA^02452^USA^L||^PRN^PH^^^657^5558563|||||||||2186-5^non Hispanic or Latino^CDCREC\n"
          + "PD1|||||||||||02^Reminder/Recall - any method^HL70215|||||A|20120701|20120701\n"
          + "NK1|1|Lam^Morgan^^^^^L|MTH^Mother^HL70063|32 Prescott Street Ave^^Warwick^MA^02452^USA^L|^PRN^PH^^^657^5558563\n"
          + "ORC|RE||IZ-783274^NDA|||||||I-23432^Burden^Donna^A^^^^^NIST-AA-1^^^^PRN||57422^RADON^NICHOLAS^^^^^^NIST-AA-1^L^^^MD\n"
          + "RXA|0|1|20120814||33332-0010-01^Influenza, seasonal, injectable, preservative free^NDC|0.5|mL^MilliLiter [SI Volume Units]^UCUM||00^New immunization record^NIP001|7832-1^Lemon^Mike^A^^^^^NIST-AA-1^^^^PRN|^^^X68||||Z0860BB|20121104|CSL^CSL Behring^MVX|||CP|A\n"
          + "RXR|C28161^Intramuscular^NCIT|LD^Left Arm^HL70163\n"
          + "OBX|1|CE|64994-7^Vaccine funding program eligibility category^LN|1|V05^VFC eligible - Federally Qualified Health Center Patient (under-insured)^HL70064||||||F|||20120701|||VXC40^Eligibility captured at the immunization level^CDCPHINVS\n"
          + "OBX|2|CE|30956-7^vaccine type^LN|2|88^Influenza, unspecified formulation^CVX||||||F\n"
          + "OBX|3|TS|29768-9^Date vaccine information statement published^LN|2|20120702||||||F\n"
          + "OBX|4|TS|29769-7^Date vaccine information statement presented^LN|2|20120814||||||F\n";

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    resp.setContentType("text/html");
    PrintWriter out = new PrintWriter(resp.getOutputStream());
    try {
      String message = req.getParameter(PARAM_MESSAGE);
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
          IncomingMessageHandler handler = new IncomingMessageHandler(dataSession);
          ack = handler.process(message, orgAccess);
          session.setAttribute("orgAccess", orgAccess);
        }
      } finally {
        dataSession.close();
      }
      resp.setContentType("text/plain");
      out.print(ack);
    } catch (Exception e) {
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
      String message = req.getParameter(PARAM_MESSAGE);
      if (message == null || message.equals("")) {
        TestCaseMessage testCaseMessage =
            ScenarioManager.createTestCaseMessage(ScenarioManager.SCENARIO_1_R_ADMIN_CHILD);
        Transformer transformer = new Transformer();
        transformer.transform(testCaseMessage);
        message = testCaseMessage.getMessageText();
      }
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
        out.println("      <h3>Message To Send</h3>");
        out.println("      <textarea class=\"w3-input\" name=\"" + PARAM_MESSAGE
            + "\" rows=\"15\" cols=\"160\">" + message + "</textarea></td>");
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
        out.println("      <br/>");
        out.println(
            "      <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"sumbit\" value=\"Submit\"/>");
        out.println("    <span class=\"w3-yellow\">Test Data Only</span>");

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

  @SuppressWarnings("unchecked")
  public OrgAccess authenticateOrgAccess(String userId, String password, String facilityId,
      Session dataSession) {
    OrgMaster orgMaster = null;
    OrgAccess orgAccess = null;
    {
      Query query = dataSession.createQuery("from OrgMaster where organizationName = ?");
      query.setParameter(0, facilityId);
      List<OrgMaster> orgMasterList = query.list();
      if (orgMasterList.size() > 0) {
        orgMaster = orgMasterList.get(0);
      } else {
        orgMaster = new OrgMaster();
        orgMaster.setOrganizationName(facilityId);
        orgAccess = new OrgAccess();
        orgAccess.setOrg(orgMaster);
        orgAccess.setAccessName(userId);
        orgAccess.setAccessKey(password);
        Transaction transaction = dataSession.beginTransaction();
        dataSession.save(orgMaster);
        dataSession.save(orgAccess);
        transaction.commit();
      }

    }
    if (orgAccess == null) {
      Query query = dataSession
          .createQuery("from OrgAccess where accessName = ? and accessKey = ? and org = ?");
      query.setParameter(0, userId);
      query.setParameter(1, password);
      query.setParameter(2, orgMaster);
      List<OrgAccess> orgAccessList = query.list();
      if (orgAccessList.size() != 0) {
        orgAccess = orgAccessList.get(0);
      }
    }
    return orgAccess;
  }
}
