package org.immregistries.iis.kernal.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.tch.fc.IISConnector;
import org.tch.fc.IISConnector.ParseDebugLine;
import org.tch.fc.model.EvaluationActual;
import org.tch.fc.model.ForecastActual;
import org.tch.fc.model.Software;
import org.tch.fc.model.SoftwareResult;
import org.tch.fc.model.TestCase;
import org.tch.fc.model.TestEvent;
import org.tch.fc.model.VaccineGroup;
import org.tch.fc.util.ForecastResultPrinter;

@SuppressWarnings("serial")
public class FitsServlet extends HttpServlet {

  public static final String RSP_MESSAGE = "rsp";
  public static final String MESSAGE_NAME = "messageName";


  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    resp.setContentType("text/html");
    PrintWriter out = new PrintWriter(resp.getOutputStream());
    String rsp = "";
    if (req.getParameter(RSP_MESSAGE) != null) {
      rsp = req.getParameter(RSP_MESSAGE);
    }
    String messageName = "";
    if (req.getParameter(MESSAGE_NAME) != null) {
      messageName = req.getParameter(MESSAGE_NAME);
    }
    try {
      {
        out.println("<html>");
        out.println("  <head>");
        out.println("    <title>FITS HL7 Read Inspector</title>");
        out.println("  </head>");
        out.println("  <body>");
        out.println("    <h1>FITS HL7 Read Inspector</h1>");
        out.println("    <form method=\"POST\" action=\"fits\">");
        out.println("    <textarea name=\"" + RSP_MESSAGE + "\"\" cols=\"80\" rows=\"30\">" + rsp
            + "</textarea><br/>");
        out.println("    JUnit Name (no spaces): <input type=\"text\" name=\"" + MESSAGE_NAME
            + "\"\" size=\"25\" value=\"" + messageName + "\"/><br/>");
        out.println("      <input type=\"submit\" name=\"sumbit\" value=\"Submit\"/>");
        out.println("    </form>");
        if (rsp.length() > 4) {
          out.println("    <h2>Reading RSP</h2>");
          Software software = new Software();
          SoftwareResult softwareResult = new SoftwareResult();

          IISConnector c = new IISConnector(software, VaccineGroup.getForecastItemList());
          List<ForecastActual> forecastActualList = new ArrayList<ForecastActual>();

          TestCase testCase = IISConnector.recreateTestCase(rsp);
          List<ParseDebugLine> parseDebugLineList = new ArrayList<>();
          c.readRSP(forecastActualList, testCase, softwareResult, rsp, parseDebugLineList);

          out.println("<pre>");
          ForecastResultPrinter.printOutResultInFixedWidth(forecastActualList, testCase, out);
          out.println("</pre>");

          out.println("<table border=\"1\" cellpadding=\"3\" cellspacing=\"0\">");
          out.println("<tr>");
          out.println("  <th width=\"20%\">Status</th>");
          out.println("  <th width=\"50%\">Line</th>");
          out.println("  <th width=\"30%\">Reason</th>");
          out.println("</tr>");
          for (ParseDebugLine parseDebugLine : parseDebugLineList) {
            String color = "white";
            switch (parseDebugLine.getLineStatus()) {
              case EXPECTED_BUT_NOT_READ:
                color = "#ccffff";
                break;
              case NOT_READ:
                color = "#ffffff";
                break;
              case OK:
                color = "#66ff66";
                break;
              case PROBLEM:
                color = "#ff6666";
                break;
            }
            out.println("<tr>");
            out.println("  <td style=\"background-color: " + color + ";\">"
                + parseDebugLine.getLineStatus() + "</td>");
            out.println("  <td style=\"background-color: " + color + ";\">"
                + parseDebugLine.getLine() + "</td>");
            out.println("  <td style=\"background-color: " + color + ";\">"
                + parseDebugLine.getLineStatusReason() + "</td>");
            out.println("</tr>");
          }
          out.println("</table>");

          out.println("<h3>JUnit test for " + messageName + "</h3>");
          out.println("<pre>");
          out.println(
              "  private static final String RSP_" + messageName.toUpperCase() + " = \"\" ");
          BufferedReader in = new BufferedReader(new StringReader(rsp));
          String line;
          while ((line = in.readLine()) != null) {
            out.print("    + \"");
            int pos = line.indexOf('\\');
            while (pos >= 0) {
              out.print(line.substring(0, pos));
              out.print("\\\\");
              line = line.substring(pos + 1);
              pos = line.indexOf('\\');
            }
            out.println(line + "\\r\"");
          }
          out.println("  ;");
          out.println("  @Test");
          out.println("  public void testRSP_" + messageName + "() throws Exception {");
          out.println(
              "    List&lt;ForecastActual&gt; forecastActualList = new ArrayList&lt;ForecastActual&gt;();");
          out.println("    TestCase testCase = run(forecastActualList, RSP_"
              + messageName.toUpperCase() + ");");
          SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
          out.println("    SimpleDateFormat sdf = new SimpleDateFormat(\"MM/dd/yyyy\");");
          out.println("    assertEquals(\"Not all test events read\", "
              + testCase.getTestEventList().size() + ", testCase.getTestEventList().size()); ");
          {
            int posA = 0;
            for (TestEvent testEvent : testCase.getTestEventList()) {
              if (testEvent.getEvaluationActualList() != null
                  && testEvent.getEvaluationActualList().size() > 0) {
                out.println("    assertEquals(\"Wrong number of evaluations\", "
                    + testEvent.getEvaluationActualList().size()
                    + ", testCase.getTestEventList().get(" + posA
                    + ").getEvaluationActualList().size()); ");
                int posB = 0;
                for (EvaluationActual evaluationActual : testEvent.getEvaluationActualList()) {
                  if (evaluationActual.getVaccineCvx() != null) {
                    out.println("    assertEquals(\"Wrong CVX found\", \""
                        + evaluationActual.getVaccineCvx() + "\", testCase.getTestEventList().get("
                        + posA + ").getEvaluationActualList().get(" + posB
                        + ").getVaccineCvx()); ");
                    out.println("    assertEquals(\"Wrong validity found\", \""
                        + evaluationActual.getDoseValid() + "\", testCase.getTestEventList().get("
                        + posA + ").getEvaluationActualList().get(" + posB + ").getDoseValid()); ");
                  }
                  posB++;
                }
              }
              posA++;
            }
          }
          out.println("    assertEquals(\"Not all forecasts read\", " + forecastActualList.size()
              + ",forecastActualList.size()); ");

          {
            int posA = 0;
            for (ForecastActual forecastActual : forecastActualList) {
              out.println("    assertEquals(\"Forecast not found\", \""
                  + forecastActual.getVaccineGroup().getLabel() + "\", forecastActualList.get("
                  + posA + ").getVaccineGroup().getLabel()); ");
              out.println(
                  "    assertEquals(\"Wrong status found\", \"" + forecastActual.getAdminStatus()
                      + "\", forecastActualList.get(" + posA + ").getAdminStatus()); ");
              if (forecastActual.getValidDate() == null) {
                out.println("    assertNull(\"Valid date should be null\", forecastActualList.get("
                    + posA + ").getValidDate()); ");
              } else {
                out.println("    assertNotNull(\"Valid date should not be null\", forecastActualList.get("
                    + posA + ").getValidDate()); ");
                out.println(
                    "    assertEquals(\"Wrong earliest date found\", \"" + sdf.format(forecastActual.getValidDate())
                        + "\", sdf.format(forecastActualList.get(" + posA + ").getValidDate())); ");
              }
              if (forecastActual.getDueDate() == null) {
                out.println("    assertNull(\"Due date should be null\", forecastActualList.get("
                    + posA + ").getDueDate()); ");
              } else {
                out.println("    assertNotNull(\"Due date should not be null\", forecastActualList.get("
                    + posA + ").getDueDate()); ");
                out.println(
                    "    assertEquals(\"Wrong due date found\", \"" + sdf.format(forecastActual.getDueDate())
                        + "\", sdf.format(forecastActualList.get(" + posA + ").getDueDate())); ");
              }
              if (forecastActual.getOverdueDate() == null) {
                out.println("    assertNull(\"Overdue date should be null\", forecastActualList.get("
                    + posA + ").getOverdueDate()); ");
              } else {
                out.println("    assertNotNull(\"Overdue date should not be null\", forecastActualList.get("
                    + posA + ").getOverdueDate()); ");
                out.println(
                    "    assertEquals(\"Wrong overdue date found\", \"" + sdf.format(forecastActual.getOverdueDate())
                        + "\", sdf.format(forecastActualList.get(" + posA + ").getOverdueDate())); ");
              }
              posA++;
            }
          }
          out.println("  }");
          in.close();
        }
        out.println("</pre>");
        out.println("  </body>");
        out.println("</html>");
      }
    } catch (Exception e) {
      e.printStackTrace(System.err);
    }
    out.flush();
    out.close();
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

  private static String pad(Object o, int len) {
    if (o == null) {
      return pad("", len);
    }
    return pad(o.toString(), len);
  }

  private static String pad(Date date, int len) {
    if (date == null) {
      return pad("", len);
    }
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    return pad(sdf.format(date), len);
  }

  private static String pad(String s, int len) {
    if (s == null) {
      s = "";
    }
    for (int i = s.length(); i < len; i++) {
      s += " ";
    }
    if (s.length() > len) {
      s = s.substring(0, len);
    }
    return s;
  }

}
