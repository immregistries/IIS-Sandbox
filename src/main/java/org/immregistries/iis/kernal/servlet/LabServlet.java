package org.immregistries.iis.kernal.servlet;

import org.immregistries.mqe.hl7util.parser.HL7Reader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class LabServlet extends HttpServlet {


  public static final String ACTION_CONVERT = "Convert";

  public static final String PARAM_ACTION = "action";
  public static final String PARAM_MESSAGE = "message";
  public static final String PARAM_MESSAGE_VXU = "messageVxu";

  private static Map<String, String> covid19Tests = new HashMap<>();

  static {
    covid19Tests.put("94763-0", "SARS coronavirus 2");
    covid19Tests.put("94661-6", "SARS coronavirus 2 Ab");
    covid19Tests.put("94762-2", "SARS coronavirus 2 Ab");
    covid19Tests.put("94769-7", "SARS coronavirus 2 Ab");
    covid19Tests.put("94504-8", "SARS coronavirus 2 Ab panel");
    covid19Tests.put("94558-4", "SARS coronavirus 2 Ag");
    covid19Tests.put("94509-7", "SARS coronavirus 2 E gene");
    covid19Tests.put("94758-0", "SARS coronavirus 2 E gene");
    covid19Tests.put("94765-5", "SARS coronavirus 2 E gene");
    covid19Tests.put("94315-9", "SARS coronavirus 2 E gene");
    covid19Tests.put("94562-6", "SARS coronavirus 2 Ab.IgA");
    covid19Tests.put("94768-9", "SARS coronavirus 2 Ab.IgA");
    covid19Tests.put("94720-0", "SARS coronavirus 2 Ab.IgA");
    covid19Tests.put("95125-1", "SARS coronavirus 2 Ab.IgA+IgM");
    covid19Tests.put("94761-4", "SARS coronavirus 2 Ab.IgG");
    covid19Tests.put("94563-4", "SARS coronavirus 2 Ab.IgG");
    covid19Tests.put("94507-1", "SARS coronavirus 2 Ab.IgG");
    covid19Tests.put("94505-5", "SARS coronavirus 2 Ab.IgG");
    covid19Tests.put("94503-0", "SARS coronavirus 2 Ab.IgG & IgM panel");
    covid19Tests.put("94547-7", "SARS coronavirus 2 Ab.IgG+IgM");
    covid19Tests.put("94564-2", "SARS coronavirus 2 Ab.IgM");
    covid19Tests.put("94508-9", "SARS coronavirus 2 Ab.IgM");
    covid19Tests.put("94506-3", "SARS coronavirus 2 Ab.IgM");
    covid19Tests.put("94510-5", "SARS coronavirus 2 N gene");
    covid19Tests.put("94311-8", "SARS coronavirus 2 N gene");
    covid19Tests.put("94312-6", "SARS coronavirus 2 N gene");
    covid19Tests.put("94760-6", "SARS coronavirus 2 N gene");
    covid19Tests.put("94533-7", "SARS coronavirus 2 N gene");
    covid19Tests.put("94756-4", "SARS coronavirus 2 N gene");
    covid19Tests.put("94757-2", "SARS coronavirus 2 N gene");
    covid19Tests.put("94766-3", "SARS coronavirus 2 N gene");
    covid19Tests.put("94316-7", "SARS coronavirus 2 N gene");
    covid19Tests.put("94307-6", "SARS coronavirus 2 N gene");
    covid19Tests.put("94308-4", "SARS coronavirus 2 N gene");
    covid19Tests.put("94644-2", "SARS coronavirus 2 ORF1ab region");
    covid19Tests.put("94511-3", "SARS coronavirus 2 ORF1ab region");
    covid19Tests.put("94559-2", "SARS coronavirus 2 ORF1ab region");
    covid19Tests.put("94639-2", "SARS coronavirus 2 ORF1ab region");
    covid19Tests.put("94646-7", "SARS coronavirus 2 RdRp gene");
    covid19Tests.put("94645-9", "SARS coronavirus 2 RdRp gene");
    covid19Tests.put("94534-5", "SARS coronavirus 2 RdRp gene");
    covid19Tests.put("94314-2", "SARS coronavirus 2 RdRp gene");
    covid19Tests.put("94745-7", "SARS coronavirus 2 RNA");
    covid19Tests.put("94746-5", "SARS coronavirus 2 RNA");
    covid19Tests.put("94819-0", "SARS coronavirus 2 RNA");
    covid19Tests.put("94565-9", "SARS coronavirus 2 RNA");
    covid19Tests.put("94759-8", "SARS coronavirus 2 RNA");
    covid19Tests.put("94500-6", "SARS coronavirus 2 RNA");
    covid19Tests.put("94845-5", "SARS coronavirus 2 RNA");
    covid19Tests.put("94822-4", "SARS coronavirus 2 RNA");
    covid19Tests.put("94660-8", "SARS coronavirus 2 RNA");
    covid19Tests.put("94309-2", "SARS coronavirus 2 RNA");
    covid19Tests.put("94531-1", "SARS coronavirus 2 RNA panel");
    covid19Tests.put("94306-8", "SARS coronavirus 2 RNA panel");
    covid19Tests.put("94642-6", "SARS coronavirus 2 S gene");
    covid19Tests.put("94643-4", "SARS coronavirus 2 S gene");
    covid19Tests.put("94640-0", "SARS coronavirus 2 S gene");
    covid19Tests.put("94767-1", "SARS coronavirus 2 S gene");
    covid19Tests.put("94641-8", "SARS coronavirus 2 S gene");
    covid19Tests.put("94764-8", "SARS coronavirus 2 whole genome");
    covid19Tests.put("95209-3", "SARS coronavirus+SARS coronavirus 2 Ag");
    covid19Tests.put("94313-4", "SARS-like coronavirus N gene");
    covid19Tests.put("94310-0", "SARS-like coronavirus N gene");
    covid19Tests.put("94502-2", "SARS-related coronavirus RNA");
    covid19Tests.put("94647-5", "SARS-related coronavirus RNA");
    covid19Tests.put("94532-9", "SARS-related coronavirus+MERS coronavirus RNA");

  }

  private static final String EXAMPLE_LAB_MESSAGE =
      "MSH|^~\\&#|STARLIMS.AR.STAG^2.16.840.1.114222.4.3.3.2.5.2^ISO|AR.LittleRock.SPHL^2.16.840.1.114222.4.1.20083^ISO|US WHO Collab LabSys^2.16.840.1.114222.4.3.3.7^ISO|CDC-EPI Surv Branch^2.16.840.1.114222.4.1.10416^ISO|20190422132236-0500||ORU^R01^ORU_R01|1312-2|T|2.5.1|||NE|NE|USA||||PHLabReport-NoAck^ELR251R1_Rcvr_Prof^2.16.840.1.113883.9.11^ISO~PHLIP_ELSM_251^PHLIP_Profile_Flu^2.16.840.1.113883.9.179^ISO\r"
          + "SFT|Software Vendor|v12|Software Name|Binary ID unknown||20181008\r"
          + "PID|1||PID13295037^^^STARLIMS.AR.STAG&2.16.840.1.114222.4.3.3.2.5.2&ISO^PI||~^^^^^^S||19340726|F||2106-3^White^CDCREC^^^^^^White|^^^AR^72016^USA|||||||||||U^Unknown^HL70189^^^^^^Unknown\r"
          + "ORC|RE|1905700000256-13^PHLIP-Test-EHR^2.16.840.1.113883.3.72.5.24^ISO|1905700000256-177^STARLIMS.AR.STAG^2.16.840.1.114222.4.3.3.2.5.2^ISO|||||||||1412941681^Smith^John^C^^DR^^^NPI&2.16.840.1.113883.4.6&ISO^L^^^NPI^^^^^^^^MD||^WPN^PH^^1^707^2643378|||||||Little Rock General Hospital Lab^D^^^^NPI&2.16.840.1.113883.4.6&ISO^NPI^^^1255402921|2217 Trancas^Suite 22^Little Rock^AR^72205^USA^M|^WPN^PH^^1^707^5549876\r"
          + "OBR|1|1905700000256-13^PHLIP-Test-EHR^2.16.840.1.113883.3.72.5.24^ISO|1905700000256-177^STARLIMS.AR.STAG^2.16.840.1.114222.4.3.3.2.5.2^ISO|94309-2^SARS-CoV-2 RNA XXX NAA+probe-Imp^LN|||201902281257-0500|||||||||1412941681^Smith^John^C^^DR^^^NPI&2.16.840.1.113883.4.6&ISO^L^^^NPI^^^^^^^^MD|^WPN^PH^^1^707^2643378|||||20190402082143-0500|||F\r"
          + "OBX|1|CWE|94309-2^SARS-CoV-2 RNA XXX NAA+probe-Imp^LN||260373001^Detected^SCT||||||F|||201902281257-0500|||||201904020721-0500||||Public Health Laboratory^D^^^^CLIA&2.16.840.1.113883.19.4.6&ISO^XX^^^05D0897628|3434 Industrial Loop^^Little Rock^AR^72205^USA^B\r"
          + "NTE|1|L|94309-2 is a report code. It should be conditional in the panel = either this OR all the target codes MUST be used; both may be used also.\r"
          + "SPM|1|^1905700000256-12&STARLIMS.AR.STAG&2.16.840.1.114222.4.3.3.2.5.2&ISO||258500001^Nasopharyngeal swab (specimen)^SCT|||||||||||||201902281257-0500|201903011118-0500";

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
      String action = req.getParameter(PARAM_ACTION);
      String message = req.getParameter(PARAM_MESSAGE);
      if (message == null) {
        message = EXAMPLE_LAB_MESSAGE;
      }
      String messageVxu = null;
      String messageError = null;
      String messageConfirmation = null;
      if (action != null) {
        if (action.equals(ACTION_CONVERT)) {

          StringBuilder sb = new StringBuilder();
          HL7Reader reader = new HL7Reader(message);
          if (reader.advanceToSegment("MSH")) {
            if (reader.getValue(9).equals("ORU") && reader.getValue(9, 2).equals("R01")) {
              String dateTimeOfMessage = reader.getOriginalField(7);
              sb.append("MSH|^~\\&|");
              sb.append(reader.getOriginalField(3) + "|");
              sb.append(reader.getOriginalField(4) + "|");
              sb.append(reader.getOriginalField(5) + "|");
              sb.append(reader.getOriginalField(6) + "|");
              sb.append(dateTimeOfMessage + "|");
              sb.append("|");
              sb.append("VXU^V04^VXU_V04|");
              sb.append(reader.getOriginalField(10) + "|");
              sb.append(reader.getOriginalField(11) + "|");
              sb.append("2.5.1|");
              sb.append("|");
              sb.append("|");
              sb.append("ER|");
              sb.append("AL|");
              sb.append("|");
              sb.append("|");
              sb.append("|");
              sb.append("|");
              sb.append("Z22^CDCPHINVS\r");

              if (dateTimeOfMessage.length() > 8) {
                dateTimeOfMessage = dateTimeOfMessage.substring(0, 8);
              }


              if (reader.advanceToSegment("PID")) {
                sb.append(reader.getOriginalSegment() + "\r");
              }
              int count = 0;
              while (reader.advanceToSegment("OBX")) {
                String question = reader.getValue(3);
                if (covid19Tests.containsKey(question)) {
                  count++;
                  sb.append("ORC|RE||9999^IIS\r");
                  sb.append("RXA|0|1|" + dateTimeOfMessage
                      + "||998^No Vaccination Administered^CVX|999||||||||||||||NA\r");
                  sb.append(reader.getOriginalSegment() + "\r");
                }
              }
              messageVxu = sb.toString();
              messageConfirmation = "Found " + count + " SARS-CoV2 test(s)";
            } else {
              messageError = "Unable to convert, not an ORU^R01 message";
            }
          } else {
            messageError = "Does not appear to be HL7 message, MSH Segment not found";
          }
        }
      }
      HomeServlet.doHeader(out, session, "IIS Sandbox");



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
      out.println("    <div class=\"w3-container w3-card-4\">");
      out.println("    <h2>Convert Lab Message (ORU^R01) with SARS-CoV-2 Results to VXU</h2>");
      out.println("    <form method=\"POST\" action=\"lab\" class=\"w3-container w3-card-4\">");
      out.println("      <textarea class=\"w3-input\" name=\"" + PARAM_MESSAGE
          + "\" rows=\"15\" cols=\"160\">" + message + "</textarea></td>");
      out.println(
          "          <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\""
              + PARAM_ACTION + "\" value=\"" + ACTION_CONVERT + "\"/>");
      if (messageVxu != null) {
        out.println("      <textarea class=\"w3-input\" name=\"" + PARAM_MESSAGE_VXU
            + "\" rows=\"15\" cols=\"160\">" + messageVxu + "</textarea></td>");
      }
      out.println("    </form>");
      out.println("    </div>");

    } catch (Exception e) {
      System.err.println("Unable to render page: " + e.getMessage());
      e.printStackTrace(System.err);
    }
    HomeServlet.doFooter(out, session);
    out.flush();
    out.close();
  }

}
