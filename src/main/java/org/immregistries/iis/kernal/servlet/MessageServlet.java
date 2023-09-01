package org.immregistries.iis.kernal.servlet;

import org.hibernate.Query;
import org.hibernate.Session;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.model.MessageReceived;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgMaster;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class MessageServlet extends HttpServlet {

  public static final String PARAM_ORG_ID = "orgId";

  public static final String PARAM_ACTION = "action";

  public static final String ACTION_SEARCH = "Search";


  public static final String PARAM_SEARCH = "search";

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

  @SuppressWarnings("unchecked")
@Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    HttpSession session = req.getSession(true);

    resp.setContentType("text/html");
    PrintWriter out = new PrintWriter(resp.getOutputStream());
    Session dataSession = PopServlet.getDataSession();
    try {
      String messageError = null;
      String messageConfirmation = null;
      HomeServlet.doHeader(out, "IIS Sandbox");
      if (messageError != null) {
        out.println("  <div class=\"w3-panel w3-red\">");
        out.println("    <p>" + messageError + "</p>");
        out.println("  </div>");
      }
      if (messageConfirmation != null) {
        out.println("  <div class=\"w3-panel w3-green\">");
        out.println("    <p>" + messageConfirmation + "</p>");
        out.println("  </div>");
      }

      OrgMaster orgMaster = ServletHelper.getOrgMaster();
      if (orgMaster != null) {
        out.println("    <div class=\"w3-container w3-half w3-margin-top\">");
			out.println("    <h2>Facility: " + orgMaster.getOrganizationName() + "</h2>");
			out.println("    <h3>Messages Recently Received</h3>");
        String search = req.getParameter(PARAM_SEARCH);
        if (search == null) {
          search = "";
        }
        out.println(
            "    <form method=\"GET\" action=\"message\" class=\"w3-container w3-card-4\">");
        out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_SEARCH
            + "\" value=\"" + search + "\"/>");
        out.println(
            "          <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\""
                + PARAM_ACTION + "\" value=\"" + ACTION_SEARCH + "\"/>");
        out.println("    </form>");
        out.println("    </div>");

        out.println("    <div class=\"w3-container\">");
        List<MessageReceived> messageReceivedList;
        {
          Query query = dataSession.createQuery(
              "from MessageReceived where orgMaster = :orgMaster order by reportedDate desc");
          query.setParameter("orgMaster", orgMaster);
          messageReceivedList = query.list();
        }

        if (messageReceivedList.size() == 0) {
          out.println("     <em>None Received</em>");
        } else {
          int count = 0;
          for (MessageReceived messageReceived : messageReceivedList) {
				 if (search.length() > 0) {
					 if (!messageReceived.getMessageRequest().contains(search)
						 && !messageReceived.getMessageResponse().contains(search)) {
						 continue;
					 }
				 }
				 count++;
				 if (count > 10) {
					 out.println("  <em>Only showing first 10 messages</em>");
					 break;
				 }
				 PatientServlet.printMessageReceived(out, messageReceived);
			 }
        }
        out.println("    </div>");
      }
    } catch (Exception e) {
      System.err.println("Unable to render page: " + e.getMessage());
      e.printStackTrace(System.err);
    } finally {
      dataSession.close();
    }
    HomeServlet.doFooter(out);
    out.flush();
    out.close();
  }

}
