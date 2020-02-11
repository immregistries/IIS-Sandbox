package org.immregistries.iis.kernal.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.immregistries.iis.kernal.model.MessageReceived;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgMaster;

@SuppressWarnings("serial")
public class MessageServlet extends HttpServlet {

  public static final String PARAM_ORG_ID = "orgId";
  public static final String PARAM_USERID = "USERID";
  public static final String PARAM_PASSWORD = "PASSWORD";
  public static final String PARAM_FACILITYID = "FACILITYID";

  public static final String PARAM_ACTION = "action";

  public static final String ACTION_LOGIN = "Login";
  public static final String ACTION_LOGOUT = "Logout";
  public static final String ACTION_SEARCH = "Search";
  public static final String ACTION_SWITCH = "Switch";

  public static final String PARAM_SEARCH = "search";

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
      String messageError = null;
      String messageConfirmation = null;
      if (action != null) {
        if (action.equals(ACTION_LOGOUT)) {
          session.removeAttribute("orgAccess");
        } else if (action.equals(ACTION_LOGIN)) {
          String userId = req.getParameter(PARAM_USERID);
          String facilityId = req.getParameter(PARAM_FACILITYID);
          String password = req.getParameter(PARAM_PASSWORD);
          OrgAccess orgAccess = authenticateOrgAccess(userId, password, facilityId, dataSession);
          if (orgAccess == null) {
            messageError = "Unable to login, unrecognized credentials";
          } else {
            session.setAttribute("orgAccess", orgAccess);
            Map<Integer, OrgAccess> orgAccessMap = new HashMap<Integer, OrgAccess>();
            Query query = dataSession.createQuery("from OrgMaster order by organizationName");
            List<OrgMaster> orgMasterList = query.list();
            for (OrgMaster orgMaster : orgMasterList) {
              OrgAccess oa =
                  authenticateOrgAccessForFacility(userId, password, dataSession, orgMaster);
              if (oa != null) {
                orgAccessMap.put(orgMaster.getOrgId(), oa);
              }
            }
            session.setAttribute("orgAccessMap", orgAccessMap);
            messageConfirmation = "Logged in to " + orgAccess.getOrg().getOrganizationName();
          }
        } else if (action.equals(ACTION_SWITCH)) {
          OrgMaster orgMaster = (OrgMaster) dataSession.get(OrgMaster.class,
              Integer.parseInt(req.getParameter(PARAM_ORG_ID)));
          @SuppressWarnings("unchecked")
          Map<Integer, OrgAccess> orgAccessMap =
              (Map<Integer, OrgAccess>) session.getAttribute("orgAccessMap");
          if (orgAccessMap != null) {
            OrgAccess orgAccess = orgAccessMap.get(orgMaster.getOrgId());
            if (orgAccess != null) {
              session.setAttribute("orgAccess", orgAccess);
              messageConfirmation = "Switched to " + orgAccess.getOrg().getOrganizationName() + "";
            }
          }
        }
      }
      HomeServlet.doHeader(out, session);
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
        out.println("    <div class=\"w3-container w3-card-4\">");
        out.println("    <h2>Login</h2>");
        out.println(
            "    <form method=\"POST\" action=\"message\" class=\"w3-container w3-card-4\">");
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
        out.println("    </div>");
      } else {
        out.println("    <div class=\"w3-container w3-half w3-margin-top\">");
        out.println("    <h2>" + orgAccess.getOrg().getOrganizationName() + "</h2>");
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
          query.setParameter("orgMaster", orgAccess.getOrg());
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
            if (count > 100) {
              out.println("  <em>Only showing first 100 messages</em>");
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
    HomeServlet.doFooter(out, session);
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
      orgAccess = authenticateOrgAccessForFacility(userId, password, dataSession, orgMaster);
    }
    return orgAccess;
  }

  public OrgAccess authenticateOrgAccessForFacility(String userId, String password,
      Session dataSession, OrgMaster orgMaster) {
    OrgAccess orgAccess = null;
    Query query = dataSession
        .createQuery("from OrgAccess where accessName = ? and accessKey = ? and org = ?");
    query.setParameter(0, userId);
    query.setParameter(1, password);
    query.setParameter(2, orgMaster);
    List<OrgAccess> orgAccessList = query.list();
    if (orgAccessList.size() != 0) {
      orgAccess = orgAccessList.get(0);
    }
    return orgAccess;
  }
}
