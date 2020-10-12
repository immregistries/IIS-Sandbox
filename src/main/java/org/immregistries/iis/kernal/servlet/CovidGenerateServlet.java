package org.immregistries.iis.kernal.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.hibernate.Session;
import org.immregistries.smm.tester.TestCovidReporting;

@SuppressWarnings("serial")
public class CovidGenerateServlet extends HttpServlet {


  public static final String ACTION_GENERATE = "Generate";

  public static final String PARAM_ACTION = "action";

  public static final String PARAM_MESSAGE_COUNT = "messageCount";
  public static final String PARAM_INCLUDE_ADMIN = "includeAdmin";
  public static final String PARAM_INCLUDE_REFUSAL = "includeRefusal";
  public static final String PARAM_INCLUDE_COMORBIDITY = "includeComorbidity";
  public static final String PARAM_INCLUDE_MISSED = "includeMissed";
  public static final String PARAM_INCLUDE_SEROLOGY = "includeSerology";

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
      int messageCount = 100;
      boolean includeAdmin = true;
      boolean includeRefusal = true;
      boolean includeComorbidity = true;
      boolean includeMissed = true;
      boolean includeSerology = true;
      if (req.getParameter(PARAM_MESSAGE_COUNT) != null) {
        messageCount = Integer.parseInt(req.getParameter(PARAM_MESSAGE_COUNT));
        includeAdmin = req.getParameter(PARAM_INCLUDE_ADMIN) != null;
        includeRefusal = req.getParameter(PARAM_INCLUDE_REFUSAL) != null;
        includeComorbidity = req.getParameter(PARAM_INCLUDE_COMORBIDITY) != null;
        includeMissed = req.getParameter(PARAM_INCLUDE_MISSED) != null;
        includeSerology = req.getParameter(PARAM_INCLUDE_SEROLOGY) != null;
      }
      HomeServlet.doHeader(out, session);



      out.println("    <div class=\"w3-container w3-card-4\">");
      out.println("    <h2>Generate HL7 Messages with COVID-19 Vaccination Events</h2>");
      out.println(
          "    <form method=\"POST\" action=\"covidGenerate\" class=\"w3-container w3-card-4\">");
      out.println("          <label>Count</label>");
      out.println("          <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_MESSAGE_COUNT
          + "\" value=\"" + messageCount + "\"/>");
      out.println("          <label>Include Administered</label>");
      out.println("          <input class=\"w3-input\" type=\"checkbox\" name=\""
          + PARAM_INCLUDE_ADMIN + "\" value=\"Y\"" + (includeAdmin ? " checked" : "") + "/>");
      out.println("          <label>Include Refusals</label>");
      out.println("          <input class=\"w3-input\" type=\"checkbox\" name=\""
          + PARAM_INCLUDE_REFUSAL + "\" value=\"Y\"" + (includeRefusal ? " checked" : "") + "/>");
      out.println("          <label>Include Comorbidity</label>");
      out.println("          <input class=\"w3-input\" type=\"checkbox\" name=\""
          + PARAM_INCLUDE_COMORBIDITY + "\" value=\"Y\"" + (includeComorbidity ? " checked" : "")
          + "/>");
      out.println("          <label>Include Missed Appointments</label>");
      out.println("          <input class=\"w3-input\" type=\"checkbox\" name=\""
          + PARAM_INCLUDE_MISSED + "\" value=\"Y\"" + (includeMissed ? " checked" : "") + "/>");
      out.println("          <label>Include Serology</label>");
      out.println("          <input class=\"w3-input\" type=\"checkbox\" name=\""
          + PARAM_INCLUDE_SEROLOGY + "\" value=\"Y\"" + (includeSerology ? " checked" : "") + "/>");
      out.println(
          "          <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\""
              + PARAM_ACTION + "\" value=\"" + ACTION_GENERATE + "\"/>");
      out.println("    </form>");
      if (action != null) {
        if (action.equals(ACTION_GENERATE)) {
          TestCovidReporting.Options options = new TestCovidReporting.Options();
          boolean includeDummy = includeComorbidity || includeMissed || includeSerology;
          if (includeAdmin && includeRefusal && !includeDummy) {
            options.setAdministeredPercentage(0.75);
            options.setRefusedPercentage(1.0);
          } else if (includeAdmin && !includeRefusal && includeDummy) {
            options.setAdministeredPercentage(0.75);
            options.setRefusedPercentage(0.0);
          } else if (includeAdmin && !includeRefusal && !includeDummy) {
            options.setAdministeredPercentage(1.0);
          } else if (!includeAdmin && includeRefusal && includeDummy) {
            options.setAdministeredPercentage(0.0);
            options.setRefusedPercentage(0.5);
          } else if (!includeAdmin && includeRefusal && !includeDummy) {
            options.setAdministeredPercentage(0.0);
            options.setRefusedPercentage(1.0);
          } else if (!includeAdmin && !includeRefusal && includeDummy) {
            options.setAdministeredPercentage(0.0);
            options.setRefusedPercentage(0.0);
          }
          options.setComorbidityPercentage(includeComorbidity ? 0.4 : 0);
          options.setMissedPercentage(includeMissed ? 0.4 : 0);
          options.setSerologyPercentage(includeSerology ? 0.4 : 0);

          out.print(
              "<textarea cols=\"80\" rows=\"30\" style=\"white-space: nowrap;  overflow: auto;\">");
          for (int i = 0; i < messageCount; i++) {
            out.print(TestCovidReporting.createHL7Message(options));
          }
          out.println("</textarea>");
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
