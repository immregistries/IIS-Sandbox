package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.immregistries.iis.kernal.logic.IncomingMessageHandler;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.repository.RepositoryClientFactory;
import org.immregistries.smm.transform.ScenarioManager;
import org.immregistries.smm.transform.TestCaseMessage;
import org.immregistries.smm.transform.Transformer;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

public class PopServlet extends HttpServlet {
	@Autowired
	 private IncomingMessageHandler handler;
	@Autowired
	RepositoryClientFactory repositoryClientFactory;


	public static final String PARAM_MESSAGE = "MESSAGEDATA";
  public static final String PARAM_USERID = "USERID";
  public static final String PARAM_PASSWORD = "PASSWORD";
  public static final String PARAM_FACILITYID = "FACILITYID";

  private static SessionFactory factory;

  public static Session getDataSession() {
    if (factory == null) {
      factory = new Configuration().configure().buildSessionFactory();
    }
    return factory.openSession();
  }

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
          orgAccess = ServletHelper.authenticateOrgAccess(userId, password, facilityId, dataSession);
        }
        if (orgAccess == null) {
          resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          out.println(
              "Access is not authorized. Facilityid, userid and/or password are not recognized. ");
        } else {
//			  IncomingMessageHandler handler = new IncomingMessageHandler(dataSession);

			  ack = handler.process(message, orgAccess);
          session.setAttribute("orgAccess", orgAccess);
			  IGenericClient fhirClient = repositoryClientFactory.newGenericClient(orgAccess);
          session.setAttribute("fhirClient", fhirClient);
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
        userId = "DEFAULT";
      }
      String password = req.getParameter(PARAM_PASSWORD);
      if (password == null || password.equals("")) {
        password = "DEFAULT";
      }
      String facilityId = req.getParameter(PARAM_FACILITYID);
      if (facilityId == null || facilityId.equals("")) {
        facilityId = "DEFAULT";
      }
      {
        HomeServlet.doHeader(out, session);
        out.println("    <h2>Send Now</h2>");
        out.println("    <form action=\"pop\" method=\"POST\" target=\"_blank\">");
        out.println("      <h3>VXU Message</h3>");
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
            "      <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"submit\" value=\"Submit\"/>");
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

}
