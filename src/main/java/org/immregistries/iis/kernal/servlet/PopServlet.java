package org.immregistries.iis.kernal.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.immregistries.iis.kernal.logic.IncomingMessageHandler;

public class PopServlet extends HttpServlet {

  private static final String PARAM_MESSAGE = "message";
  private static final String PARAM_ACTION = "action";
  private static final String ACTION_SUBMIT = "Submit";

  private static final String EXAMPLE_HL7 =
      "MSH|^~\\&|Test EHR Application|X68||NIST Test Iz Reg|20120701082240-0500||VXU^V04^VXU_V04|NIST-IZ-001.00|P|2.5.1|||ER|AL|||||Z22^CDCPHINVS\n"
          + "PID|1||D26376273^^^NIST MPI^MR||Snow^Madelynn^Ainsley^^^^L|Lam^Morgan^^^^^M|20070706|F||2076-8^Native Hawaiian or Other Pacific Islander^CDCREC|32 Prescott Street Ave^^Warwick^MA^02452^USA^L||^PRN^PH^^^657^5558563|||||||||2186-5^non Hispanic or Latino^CDCREC\n"
          + "PD1|||||||||||02^Reminder/Recall - any method^HL70215|||||A|20120701|20120701\n"
          + "NK1|1|Lam^Morgan^^^^^L|MTH^Mother^HL70063|32 Prescott Street Ave^^Warwick^MA^02452^USA^L|^PRN^PH^^^657^5558563\n"
          + "ORC|RE||IZ-783274^NDA|||||||I-23432^Burden^Donna^A^^^^^NIST-AA-1^^^^PRN||57422^RADON^NICHOLAS^^^^^^NIST-AA-1^L^^^MD\n"
          + "RXA|0|1|20120814||33332-0010-01^Influenza, seasonal, injectable, preservative free^NDC|0.5|mL^MilliLiter [SI Volume Units]^UCUM||00^New immunization record^NIP001|7832-1^Lemon^Mike^A^^^^^NIST-AA-1^^^^PRN|^^^X68||||Z0860BB|20121104|CSL^CSL Behring^MVX|||CP|A\n"
          + "RXR|C28161^Intramuscular^NCIT|LD^Left Arm^HL70163\n"
          + "OBX|1|CE|64994-7^Vaccine funding program eligibility category^LN|1|V05^VFC eligible - Federally Qualified Health Center Patient (under-insured)^HL70064||||||F|||20120701|||VXC40^Eligibility captured at the immunization level^CDCPHINVS\n"
          + "OBX|2|CE|30956-7^vaccine type^LN|2|88^Influenza, unspecified formulation^CVX||||||F\n"
          + "OBX|3|TS|29768-9^Date vaccine information statement published^LN|2|20120702||||||F\n"
          + "OBX|4|TS|29769-7^Date vaccine information statement presented^LN|2|20120814||||||F\n";

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    resp.setContentType("text/html");
    PrintWriter out = new PrintWriter(resp.getOutputStream());
    try {
      String action = req.getParameter(PARAM_ACTION);
      String message = EXAMPLE_HL7;
      String actionStatus = null;
      if (action != null) {
        if (action.equals(ACTION_SUBMIT)) {
          message = req.getParameter(PARAM_MESSAGE);
          IncomingMessageHandler handler = new IncomingMessageHandler();
          handler.process(message);
          actionStatus = "Message Processed";
        }
      }
      out.println("<html>");
      out.println("  <head>");
      out.println("    <title>IIS Kernel Pop</title>");
      out.println("  </head>");
      out.println("  <body>");
      out.println("    <h1>Pop Test Interface</h1>");
      if (actionStatus != null) {
        out.println("    <p style=\"font-color: red;\">" + actionStatus + "</p>");
      }
      out.println("    <form method=\"POST\" action=\"pop\">");
      out.println("      <textarea name=\"" + PARAM_MESSAGE + "\" rows=\"15\" cols=\"160\">"
          + message + "</textarea>");
      out.println("      <input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\""
          + ACTION_SUBMIT + "\"/>");
      out.println("    </form>");
      out.println("  </body>");
      out.println("</html>");
    } catch (Exception e) {
      e.printStackTrace(System.err);
    }
    out.flush();
    out.close();
  }
}
