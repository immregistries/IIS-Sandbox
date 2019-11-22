package org.immregistries.iis.kernal.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.immregistries.iis.kernal.logic.IncomingMessageHandler;
import org.immregistries.iis.kernal.logic.ReceivedResponse;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgMaster;

@SuppressWarnings("serial")
public class StatusServlet extends HttpServlet {

  public static final String PARAM_ORG_ID = "orgId";
  public static final String PARAM_USERID = "USERID";
  public static final String PARAM_PASSWORD = "PASSWORD";
  public static final String PARAM_FACILITYID = "FACILITYID";

  public static final String PARAM_ACTION = "action";

  public static final String ACTION_LOGIN = "Login";
  public static final String ACTION_LOGOUT = "Logout";

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    HttpSession session = req.getSession(true);

    resp.setContentType("text/html");
    PrintWriter out = new PrintWriter(resp.getOutputStream());
    Session dataSession = PopServlet.getDataSession();
    try {
      String action = req.getParameter(PARAM_ACTION);
      if (action != null) {
        if (action.equals(ACTION_LOGOUT)) {
          session.removeAttribute("orgAccess");
        }
        HomeServlet.doHeader(out, session);
        if (action.equals(ACTION_LOGIN)) {
          String userId = req.getParameter(PARAM_USERID);
          String facilityId = req.getParameter(PARAM_FACILITYID);
          String password = req.getParameter(PARAM_PASSWORD);
          OrgAccess orgAccess = authenticateOrgAccess(userId, password, facilityId, dataSession);
          if (orgAccess == null) {
            out.println("  <div class=\"w3-panel w3-red\">");
            out.println("    <p>Unable to login, unrecognized credentials</p>");
            out.println("  </div>");
          } else {
            session.setAttribute("orgAccess", orgAccess);
          }
        }
      } else {
        HomeServlet.doHeader(out, session);
      }
      OrgAccess orgAccess = (OrgAccess) session.getAttribute("orgAccess");
      if (orgAccess == null) {
        String userId = req.getParameter(PARAM_USERID);
        String facilityId = req.getParameter(PARAM_FACILITYID);
        if (userId == null) {
          userId = "";
        }
        if (facilityId == null) {
          facilityId = "";
        }
        if (req.getParameter(PARAM_ORG_ID) != null) {
          OrgMaster orgMaster = (OrgMaster) dataSession.get(OrgMaster.class,
              Integer.parseInt(req.getParameter(PARAM_ORG_ID)));
          facilityId = orgMaster.getOrganizationName();
        }
        out.println("    <h2>Login</h2>");
        out.println(
            "    <form method=\"POST\" action=\"status\" class=\"w3-container w3-card-4\">");
        out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_USERID
            + "\" value=\"" + userId + "\" required/>");
        out.println("          <label>User Id</label>");
        out.println("          <input class=\"w3-input\" type=\"password\" name=\"" + PARAM_PASSWORD
            + "\" value=\"\"/>");
        out.println("          <label>Password</label>");
        out.println("          <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_FACILITYID
            + "\" value=\"" + facilityId + "\"/>");
        out.println("          <label>Facility Id</label><br/>");
        out.println(
            "          <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\""
                + PARAM_ACTION + "\" value=\"" + ACTION_LOGIN + "\"/>");
        out.println("    </form>");
      } else {
        out.println("    <h2>" + orgAccess.getOrg().getOrganizationName() + "</h2>");
        out.println("    <h3>Messages Recently Received</h3>");
        List<ReceivedResponse> receivedResponseList = IncomingMessageHandler
            .getReceivedResponseList(orgAccess.getOrg().getOrganizationName());
        if (receivedResponseList.size() == 0) {
          out.println("     <em>None Received</em>");
        } else {
          SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
          for (ReceivedResponse receivedResponse : receivedResponseList) {
            out.println("     <h3>" + sdf.format(receivedResponse.getReceivedDate()) + "</h3>");
            out.println("     <pre>" + receivedResponse.getReceivedMessage() + "</pre>");
            out.println("     <pre>" + receivedResponse.getResponseMessage() + "</pre>");
          }
        }
        out.println(
            "    <p><a href=\"status?" + PARAM_ACTION + "=" + ACTION_LOGOUT + "\">Logout</a></p>");
      }
    } catch (Exception e) {
      e.printStackTrace(System.err);
    } finally {
      dataSession.close();
    }
    HomeServlet.doFooter(out, false);
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
