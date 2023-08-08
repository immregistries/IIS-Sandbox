package org.immregistries.iis.kernal.servlet;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.immregistries.iis.kernal.logic.IncomingEventHandler;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

import static org.immregistries.iis.kernal.servlet.LoginServlet.*;

@SuppressWarnings("serial")
public class EventServlet extends PopServlet {
	@Autowired
	IncomingEventHandler incomingEventHandler;
  private static SessionFactory factory;

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
      OrgAccess orgAccess = ServletHelper.getOrgAccess();
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
//          IncomingEventHandler incomingEventHandler = new IncomingEventHandler(dataSession);
          ack = incomingEventHandler.process(req, orgAccess);
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
        HomeServlet.doHeader(out, "IIS Sandbox");
			out.println("    <h2>Send Now</h2>");
        out.println("    <form action=\"event\" method=\"POST\" target=\"_blank\">");
        out.println("    <div class=\"w3-container w3-half w3-margin-top\">");
        out.println("    <div class=\"w3-container w3-card-4\">");
        out.println("      <h3>Authentication</h3>");
        OrgAccess orgAccess = ServletHelper.getOrgAccess();
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
        for (String s : IncomingEventHandler.PARAMS_PATIENT) {
          out.println("      <label>" + s + "</label>");
          out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + s + "\"/>");
        }
        out.println("");
        out.println("      <h3>Vaccination</h3>");
        for (String s : IncomingEventHandler.PARAMS_VACCINATION) {
          out.println("      <label>" + s + "</label>");
          out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + s + "\"/>");
        }
        out.println("      <br/>");
        out.println(
            "      <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"sumbit\" value=\"Submit\"/>");
        out.println("    </div>");
        out.println("    </div>");
        out.println("    </form>");
        HomeServlet.doFooter(out);
      }
    } catch (Exception e) {
      e.printStackTrace(System.err);
    }
    out.flush();
    out.close();
  }

}
