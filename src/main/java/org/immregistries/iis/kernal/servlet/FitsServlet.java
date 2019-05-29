package org.immregistries.iis.kernal.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.logic.CodeMapManager;
import org.immregistries.iis.kernal.logic.FitsExamples;
import org.immregistries.vfa.connect.IISConnector;
import org.immregistries.vfa.connect.IISConnector.ParseDebugLine;
import org.immregistries.vfa.connect.model.Admin;
import org.immregistries.vfa.connect.model.EvaluationActual;
import org.immregistries.vfa.connect.model.ForecastActual;
import org.immregistries.vfa.connect.model.Software;
import org.immregistries.vfa.connect.model.SoftwareResult;
import org.immregistries.vfa.connect.model.TestCase;
import org.immregistries.vfa.connect.model.TestEvent;
import org.immregistries.vfa.connect.model.VaccineGroup;
import org.immregistries.vfa.connect.util.ForecastResultPrinter;

@SuppressWarnings("serial")
public class FitsServlet extends HttpServlet {

  public static final String RSP_MESSAGE = "rsp";
  public static final String MESSAGE_NAME = "messageName";
  public static final String EXAMPLE_NAME = "exampleName";


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
    if (req.getParameter(EXAMPLE_NAME) != null) {
      messageName = req.getParameter(EXAMPLE_NAME);
      rsp = FitsExamples.exampleMap.get(messageName);
    }
    messageName = messageName.replaceAll("\\s", "_");
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
              case OK_FORECAST:
              case OK_EVALUATION:
                color = "#009933";
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
          out.println("<br/>");

          Map<String, List<VaccineGroup>> familyMapping = c.getFamilyMapping();
          List<String> familyMappingNameList = new ArrayList<>(familyMapping.keySet());
          Collections.sort(familyMappingNameList, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
              int cvx1 = 0;
              try {
                cvx1 = Integer.parseInt(s1);
              } catch (NumberFormatException nfe) {
                // ignore
              }
              int cvx2 = 0;
              try {
                cvx2 = Integer.parseInt(s2);
              } catch (NumberFormatException nfe) {
                // ignore
              }
              if (cvx1 == 0) {
                if (cvx2 == 0) {
                  return s1.compareTo(s2);
                }
                return 1;
              } else if (cvx2 == 0) {
                return -1;
              }
              return (new Integer(cvx1)).compareTo(cvx2);
            }
          });

          out.println("<table border=\"1\" cellpadding=\"3\" cellspacing=\"0\">");
          out.println("<caption>Forecast Codes Map</caption>");
          out.println("<tr>");
          out.println("  <th>Value Received</th>");
          out.println("  <th>CVX Label from CDC</th>");
          out.println("  <th>Count</th>");
          out.println("  <th>Vaccine Group</th>");
          out.println("</tr>");
          CodeMap codeMap = CodeMapManager.getCodeMap();
          for (String familyMappingName : familyMappingNameList) {
            if (familyMapping.get(familyMappingName).size() > 0) {
              boolean first = true;
              int count = 0;
              for (ForecastActual forecastActual : forecastActualList) {
                if (forecastActual.getVaccineCvx().equals(familyMappingName)) {
                  count++;
                }
              }
              for (VaccineGroup vaccineGroup : familyMapping.get(familyMappingName)) {
                out.println("<tr>");
                String color = "white";
                if (count > 0) {
                  color = "#66ff66";
                }
                if (first) {
                  out.println("  <td style=\"background-color: " + color + ";\" rowspan=\""
                      + familyMapping.get(familyMappingName).size() + "\">" + familyMappingName
                      + "</td>");
                  Code code = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE,
                      familyMappingName);
                  if (code == null) {
                    out.println("  <td style=\"background-color: " + color + ";\" rowspan=\""
                        + familyMapping.get(familyMappingName).size() + "\">-</td>");
                  } else {
                    out.println("  <td style=\"background-color: " + color + ";\" rowspan=\""
                        + familyMapping.get(familyMappingName).size() + "\">" + code.getLabel()
                        + "</td>");
                  }
                  out.println("  <td style=\"background-color: " + color + ";\" rowspan=\""
                      + familyMapping.get(familyMappingName).size() + "\">" + count + "</td>");
                }
                out.println("  <td style=\"background-color: " + color + ";\">"
                    + vaccineGroup.getLabel() + "</td>");
                out.println("</tr>");
                first = false;
              }
            }
          }
          out.println("</table>");
          out.println("<br/>");

          out.println("<table border=\"1\" cellpadding=\"3\" cellspacing=\"0\">");
          out.println("<caption>Vaccine Groups Represented</caption>");
          out.println("<tr>");
          out.println("  <th>Vaccine Group</th>");
          out.println("  <th>Count</th>");
          out.println("</tr>");
          List<VaccineGroup> vaccineGroupList = VaccineGroup.getForecastItemList();
          Collections.sort(vaccineGroupList, new Comparator<VaccineGroup>() {
            @Override
            public int compare(VaccineGroup vg1, VaccineGroup vg2) {
              return vg1.getLabel().compareTo(vg2.getLabel());
            }
          });
          for (VaccineGroup vaccineGroup : vaccineGroupList) {
            int count = 0;
            for (ForecastActual forecastActual : forecastActualList) {
              if (forecastActual.getVaccineGroup().equals(vaccineGroup)) {
                count++;
              }
            }
            String color = "white";
            if (count > 0) {
              color = "#66ff66";
            }
            out.println("<tr>");
            out.println("  <td style=\"background-color: " + color + ";\">"
                + vaccineGroup.getLabel() + "</td>");
            out.println("  <td style=\"background-color: " + color + ";\">" + count + "</td>");
            out.println("</tr>");
          }
          out.println("</table>");
          out.println("<br/>");

          Map<String, Admin> adminStatusMap = IISConnector.getAdminstatusmap();
          Map<String, Admin> adminStatusLabelMap = IISConnector.getAdminstatuslabelmap();
          out.println("<table border=\"1\" cellpadding=\"3\" cellspacing=\"0\">");
          out.println("<caption>Evaluation Status Mapping</caption>");
          out.println("<tr>");
          out.println("  <th>Priority</th>");
          out.println("  <th>Position</th>");
          out.println("  <th>Case Sensitive</th>");
          out.println("  <th>Receive</th>");
          out.println("  <th>Evaluation Status</th>");
          out.println("</tr>");
          {
            List<String> adminStatusList = new ArrayList<String>(adminStatusMap.keySet());
            Collections.sort(adminStatusList);
            for (String adminStatus : adminStatusList) {
              Admin admin = adminStatusMap.get(adminStatus);
              if (admin != null) {
                String color = "white";
                out.println("<tr>");
                out.println("  <td style=\"background-color: " + color + ";\">1</td>");
                out.println("  <td style=\"background-color: " + color + ";\">OBX-5.1</td>");
                out.println("  <td style=\"background-color: " + color + ";\">Y</td>");
                out.println(
                    "  <td style=\"background-color: " + color + ";\">" + adminStatus + "</td>");
                out.println("  <td style=\"background-color: " + color + ";\">" + admin.getLabel()
                    + "</td>");
                out.println("</tr>");
              }
            }
          }
          {
            List<String> adminStatusLabelList = new ArrayList<String>(adminStatusLabelMap.keySet());
            Collections.sort(adminStatusLabelList);
            for (String adminStatusLabel : adminStatusLabelList) {
              Admin admin = adminStatusLabelMap.get(adminStatusLabel);
              if (admin != null) {
                String color = "white";
                out.println("<tr>");
                out.println("  <td style=\"background-color: " + color + ";\">2</td>");
                out.println("  <td style=\"background-color: " + color + ";\">OBX-5.2</td>");
                out.println("  <td style=\"background-color: " + color + ";\">N</td>");
                out.println("  <td style=\"background-color: " + color + ";\">" + adminStatusLabel
                    + "</td>");
                out.println("  <td style=\"background-color: " + color + ";\">" + admin.getLabel()
                    + "</td>");
                out.println("</tr>");
              }
            }
          }
          out.println("</table>");
          out.println("<br/>");

          printCode(out, rsp, messageName, forecastActualList, testCase);
        }
        out.println("    <h2>Example RSPs</h2>");
        List<String> exampleNameList = new ArrayList<String>(FitsExamples.exampleMap.keySet());
        Collections.sort(exampleNameList);
        out.println("<ul>");
        for (String exampleName : exampleNameList) {
          out.println("<li><a href=\"fits?" + EXAMPLE_NAME + "=" + exampleName + "\">" + exampleName
              + "</a></li>");
        }
        out.println("<ul>");

        out.println("  </body>");
        out.println("</html>");
      }
    } catch (Exception e) {
      e.printStackTrace(System.err);
    }
    out.flush();
    out.close();
  }

  public void printCode(PrintWriter out, String rsp, String messageName,
      List<ForecastActual> forecastActualList, TestCase testCase) throws IOException {
    out.println("<h3>JUnit test for " + messageName + "</h3>");
    out.println("<pre>");
    out.println("exampleMap.put(\"" + messageName + "\", RSP_" + messageName.toUpperCase() + ");");
    out.println("</pre>");
    out.println("<pre>");
    out.println("  private static final String RSP_" + messageName.toUpperCase() + " = \"\" ");
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
    out.println(
        "    TestCase testCase = run(forecastActualList, RSP_" + messageName.toUpperCase() + ");");
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
              + testEvent.getEvaluationActualList().size() + ", testCase.getTestEventList().get("
              + posA + ").getEvaluationActualList().size()); ");
          int posB = 0;
          for (EvaluationActual evaluationActual : testEvent.getEvaluationActualList()) {
            if (evaluationActual.getVaccineCvx() != null) {
              out.println("    assertEquals(\"Wrong CVX found\", \""
                  + evaluationActual.getVaccineCvx() + "\", testCase.getTestEventList().get(" + posA
                  + ").getEvaluationActualList().get(" + posB + ").getVaccineCvx()); ");
              out.println("    assertEquals(\"Wrong validity found\", \""
                  + evaluationActual.getDoseValid() + "\", testCase.getTestEventList().get(" + posA
                  + ").getEvaluationActualList().get(" + posB + ").getDoseValid()); ");
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
            + forecastActual.getVaccineGroup().getLabel() + "\", forecastActualList.get(" + posA
            + ").getVaccineGroup().getLabel()); ");
        out.println("    assertEquals(\"Wrong status found\", \"" + forecastActual.getAdminStatus()
            + "\", forecastActualList.get(" + posA + ").getAdminStatus()); ");
        if (forecastActual.getValidDate() == null) {
          out.println("    assertNull(\"Valid date should be null\", forecastActualList.get(" + posA
              + ").getValidDate()); ");
        } else {
          out.println("    assertNotNull(\"Valid date should not be null\", forecastActualList.get("
              + posA + ").getValidDate()); ");
          out.println("    assertEquals(\"Wrong earliest date found\", \""
              + sdf.format(forecastActual.getValidDate()) + "\", sdf.format(forecastActualList.get("
              + posA + ").getValidDate())); ");
        }
        if (forecastActual.getDueDate() == null) {
          out.println("    assertNull(\"Due date should be null\", forecastActualList.get(" + posA
              + ").getDueDate()); ");
        } else {
          out.println("    assertNotNull(\"Due date should not be null\", forecastActualList.get("
              + posA + ").getDueDate()); ");
          out.println("    assertEquals(\"Wrong due date found\", \""
              + sdf.format(forecastActual.getDueDate()) + "\", sdf.format(forecastActualList.get("
              + posA + ").getDueDate())); ");
        }
        if (forecastActual.getOverdueDate() == null) {
          out.println("    assertNull(\"Overdue date should be null\", forecastActualList.get("
              + posA + ").getOverdueDate()); ");
        } else {
          out.println(
              "    assertNotNull(\"Overdue date should not be null\", forecastActualList.get("
                  + posA + ").getOverdueDate()); ");
          out.println("    assertEquals(\"Wrong overdue date found\", \""
              + sdf.format(forecastActual.getOverdueDate())
              + "\", sdf.format(forecastActualList.get(" + posA + ").getOverdueDate())); ");
        }
        posA++;
      }
    }
    out.println("  }");
    in.close();
    out.println("</pre>");
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }


}
