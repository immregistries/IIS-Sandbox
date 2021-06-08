package org.immregistries.iis.kernal.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.immregistries.iis.kernal.model.OrgAccess;

@SuppressWarnings("serial")
public class VXUDownloadFormServlet extends HttpServlet {


  protected static final String EXPORT_YYYY_MM_DD = "yyyy-MM-dd";

  public static final String ACTION_GENERATE = "Generate";

  public static final String PARAM_ACTION = "action";
  public static final String PARAM_DATE_START = "dateStart";
  public static final String PARAM_DATE_END = "dateEnd";
  public static final String PARAM_CVX_CODES = "cvxCodes";
  public static final String PARAM_INCLUDE_PHI = "includePhi";

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
    OrgAccess orgAccess = (OrgAccess) session.getAttribute("orgAccess");
    if (orgAccess == null) {
      RequestDispatcher dispatcher = req.getRequestDispatcher("home");
      dispatcher.forward(req, resp);
      return;
    }

    try {
      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
      String action = req.getParameter(PARAM_ACTION);
      String messageError = null;
      String dateStartString = req.getParameter(PARAM_DATE_START);
      String dateEndString = req.getParameter(PARAM_DATE_END);
      HomeServlet.doHeader(out, session);


      Date dateStart = null;
      Date dateEnd = null;
      if (dateStartString == null) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        dateStartString = sdf.format(calendar.getTime());
      } else {
        try {
          dateStart = sdf.parse(dateStartString);
        } catch (ParseException pe) {
          messageError = "Start date is unparsable";
        }
      }
      if (dateEndString == null) {
        dateEndString = sdf.format(new Date());
      } else {
        try {
          dateEnd = sdf.parse(dateEndString);
        } catch (ParseException pe) {
          messageError = "End date is unparsable";
        }
      }
      String cvxCodes = req.getParameter(PARAM_CVX_CODES);
      if (StringUtils.isEmpty(cvxCodes)) {
        cvxCodes = CovidServlet.COVID_CVX_CODES;
      }
      boolean includePhi =
          req.getParameter(PARAM_CVX_CODES) == null || req.getParameter(PARAM_INCLUDE_PHI) != null;

      if (messageError != null) {
        out.println("  <div class=\"w3-panel w3-red\">");
        out.println("    <p>" + messageError + "</p>");
        out.println("  </div>");
      }
      out.println("    <div class=\"w3-container w3-card-4\">");
      out.println("    <h2>Download COVID-19 HL7 for CDC Reporting</h2>");
      out.println(
          "    <form method=\"POST\" action=\"VXUDownloadForm\" class=\"w3-container w3-card-4\">");
      out.println("          <label>Start Date</label>");
      out.println("          <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_DATE_START
          + "\" value=\"" + dateStartString + "\"/>");
      out.println("          <label>End Date</label>");
      out.println("          <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_DATE_END
          + "\" value=\"" + dateEndString + "\"/>");
      out.println("          <label>End Date</label>");
      out.println("          <label>CVX Codes to Include</label>");
      out.println("          <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_CVX_CODES
          + "\" value=\"" + cvxCodes + "\"/>");
      out.println("          <label>Include PHI</label>");
      out.println("          <input class=\"w3-input\" type=\"checkbox\" name=\""
          + PARAM_INCLUDE_PHI + "\" value=\"Y\"" + (includePhi ? " checked" : "") + "/>");
      out.println(
          "          <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\""
              + PARAM_ACTION + "\" value=\"" + ACTION_GENERATE + "\"/>");
      out.println("    </form>");
      if (action != null) {
        if (action.equals(ACTION_GENERATE) && dateStart != null && dateEnd != null) {
          String link = "VXUDownload";
          link += "?" + PARAM_DATE_START + "=" + dateStartString;
          link += "&" + PARAM_DATE_END + "=" + dateEndString;
          link += "&" + PARAM_CVX_CODES + "=" + cvxCodes;
          if (includePhi) {
            link += "&" + PARAM_DATE_END + "=" + cvxCodes;
          }
          SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd");
          out.println("<a href=\"" + link + "\" download=\"export" + sdf2.format(dateEnd)
              + ".vxu.txt\">Download</a>");
        }
      }
      out.println("    </div>");

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

}
