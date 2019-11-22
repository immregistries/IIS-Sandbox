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
import org.immregistries.iis.kernal.SoftwareVersion;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgMaster;

@SuppressWarnings("serial")
public class HomeServlet extends HttpServlet {

  private static SessionFactory factory;


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
    try {
      {
        doHeader(out, session);
        out.println(
            "    <div class=\"w3-panel w3-yellow\"><p class=\"w3-left-align\">This is an IIS HL7 Interface demonstration system. Only for test data use. "
                + " Do not put production data in here. Submitted data may be cleared from the database "
                + " as often as every day. </p></div>");
        out.println("    <h2>Documentation</h2>");
        out.println(
            "    <p class=\"w3-left-align\">For help and detailed documentation on functions please see the project wiki: "
                + "<a href=\"https://github.com/immregistries/IIS-Sandbox/wiki\">https://github.com/immregistries/IIS-Sandbox/wiki</a></p>");
        out.println("    <h2>Functions Supported</h2>");
        out.println("    <ul class=\"w3-ul w3-hoverable\">");
        out.println(
            "      <li><a href=\"status\">Status</a>: Most recently submitted messages</li>");
        out.println(
            "      <li><a href=\"pop\">Manual</a>: HL7 realtime interfacing using simple REST interface</li>");
        out.println(
            "      <li><a href=\"soap\">CDC WSDL</a>: HL7 realtime interfacing using CDC WSDL</li>");
        out.println("    </ul>");
        out.println("    <h2>Facilities</h2>");
        Session dataSession = PopServlet.getDataSession();
        try {
          Query query = dataSession.createQuery("from OrgMaster order by organizationName");
          List<OrgMaster> orgMasterList = query.list();
          out.println("    <ul class=\"w3-ul w3-hoverable\">");
          for (OrgMaster orgMaster : orgMasterList) {
            out.println("      <li>" + orgMaster.getOrganizationName() + "</li>");
          }
          out.println("    </ul>");
        } finally {
          dataSession.close();
        }
        doFooter(out, true);
      }
    } catch (Exception e) {
      e.printStackTrace(System.err);
    }
    out.flush();
    out.close();
  }

  public static void doHeader(PrintWriter out, HttpSession session) {
    out.println("<html>");
    out.println("  <head>");
    out.println("    <title>IIS Sandbox - Pop</title>");
    out.println("    <link rel=\"stylesheet\" href=\"https://www.w3schools.com/w3css/4/w3.css\"/>");
    out.println("  </head>");
    out.println("  <body>");
    out.println("    <header class=\"w3-container w3-light-grey\">");
    out.println("      <div class=\"w3-bar w3-light-grey\">");
    out.println(
        "        <a href=\"home\" class=\"w3-bar-item w3-button w3-green\">IIS Sandbox</a>");
    out.println("        <a href=\"status\" class=\"w3-bar-item w3-button\">Status</a>");
    out.println("        <a href=\"pop\" class=\"w3-bar-item w3-button\">Manual</a>");
    out.println("        <a href=\"soap\" class=\"w3-bar-item w3-button\">CDC WSDL</a>");
    OrgAccess orgAccess = (OrgAccess) session.getAttribute("orgAccess");
    if (orgAccess != null) {
      out.println("        <div class=\"w3-right-align\">"
          + orgAccess.getOrg().getOrganizationName() + "</div>");
    }
    out.println("      </div>");
    out.println("    </header>");
    out.println("    <div class=\"w3-container w3-half w3-margin-top\">");
  }

  public static void doFooter(PrintWriter out, boolean showPhoto) {
    out.println("  </div>");
    if (showPhoto) {
      out.println(
          "  <img src=\"images/110720-F-DM566-001.JPG\" class=\"w3-round\" alt=\"Sandbox\">");
    }
    out.println("  <p>IIS Sandbox version " + SoftwareVersion.VERSION + "</p>");
    out.println("  </body>");
    out.println("</html>");
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
