package org.immregistries.iis.kernal.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.immregistries.iis.kernal.logic.IncomingMessageHandler;
import org.immregistries.iis.kernal.logic.ReceivedResponse;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgMaster;

@SuppressWarnings("serial")
public class StatusServlet extends HttpServlet {

  public static final String PARAM_ORGANIZATION_NAME = "organizationName";

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    resp.setContentType("text/html");
    PrintWriter out = new PrintWriter(resp.getOutputStream());
    String organizationName = "Mercy Healthcare";
    if (req.getParameter(PARAM_ORGANIZATION_NAME) != null) {
      organizationName = req.getParameter(PARAM_ORGANIZATION_NAME);
    }
    try {
      {
        out.println("<html>");
        out.println("  <head>");
        out.println("    <title>IIS Kernel Status</title>");
        out.println("  </head>");
        out.println("  <body>");
        out.println("    <h1>Messages Recently Received</h1>");
        out.println("    <form method=\"GET\" action=\"status\">");
        out.println("    Organization: <input type=\"text\" name=\"" + PARAM_ORGANIZATION_NAME
            + "\" value=\"" + organizationName + "\"/><br/>");
        out.println("      <input type=\"submit\" name=\"sumbit\" value=\"Submit\"/>");
        out.println("    </form>");
        out.println("    <h2>" + organizationName + "</h2>");
        List<ReceivedResponse> receivedResponseList =
            IncomingMessageHandler.getReceivedResponseList(organizationName);
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
        out.println("  </body>");
        out.println("</html>");
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
