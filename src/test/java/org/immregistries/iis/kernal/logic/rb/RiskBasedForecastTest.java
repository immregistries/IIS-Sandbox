package org.immregistries.iis.kernal.logic.rb;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import junit.framework.TestCase;
import org.immregistries.smm.tester.manager.HL7Reader;

public class RiskBasedForecastTest extends TestCase {

  private static final String QPD =
      "MSH|^~\\&|EHR|CLINIC|IIS|IIS|20261029120000-0500||QBP^Q11^QBP_Q11|Q1|P|2.5.1|||ER|AL|||||Z45^CDCPHINVS\r"
          + "QPD|Z45^Request Evaluated Immunization History and Risk-Based Forecast^CDCPHINVS|Q1|MP1001^^^RBDEMO^MR|DemoPreg^Maria^^^^^L|DemoMom^Mother^^^^^M|19950412|F\r";
  private static final String RCP = "RCP|I|1^RD&Records&HL70126\r";

  private Date today;

  @Override
  protected void setUp() throws Exception {
    today = new SimpleDateFormat("yyyyMMdd").parse("20261029");
  }

  private List<ContextualCondition> parse(String obx) {
    return ContextualConditionParser.parse(new HL7Reader(QPD + obx + RCP));
  }

  private String forecast(String obx) {
    List<ContextualCondition> conditions = parse(obx);
    return RiskBasedForecast.writeForecast(RiskBasedForecast.build(conditions, today), today, 0, 0);
  }

  public void testParse() {
    List<ContextualCondition> conditions =
        parse("OBX|1|ID|007^Patient is currently pregnant^99CDS|1|Y||||||F\r"
            + "OBX|2|DT|170^Onset of pregnancy^99CDS|2|20260701||||||F\r");
    assertEquals(2, conditions.size());
    assertEquals("007", conditions.get(0).getCode());
    assertEquals("Patient is currently pregnant", conditions.get(0).getDescription());
    assertEquals("99CDS", conditions.get(0).getCodeSystem());
    assertEquals("ID", conditions.get(0).getValueType());
    assertTrue(conditions.get(0).isTrue());
    assertEquals(1, conditions.get(0).getObxPosition());
    assertEquals("170", conditions.get(1).getCode());
    assertEquals("20260701", conditions.get(1).getValue());
    assertNotNull(conditions.get(1).getDateValue());
    assertEquals(2, conditions.get(1).getObxPosition());
  }

  public void testParseNoObx() {
    assertEquals(0, parse("").size());
  }

  public void testPregnancyTdap() {
    String out = forecast("OBX|1|ID|007^Patient is currently pregnant^99CDS|1|Y||||||F\r"
        + "OBX|2|DT|170^Onset of pregnancy^99CDS|2|20260701||||||F\r");
    assertTrue(out, out.startsWith("RXA|0|1|20261029||998^No Vaccination Administered^CVX|999"));
    assertTrue(out, out.contains("OBX|1|CE|30956-7^Vaccine type^LN|1|115^Tdap^CVX||||||F\r"));
    assertTrue(out, out.contains("|59783-1^Status in series^LN|1|LA13422-3^On schedule^LN||"));
    // 27 weeks (189 days) after 20260701 is 20270106; 36 weeks (252 days) is 20270310
    assertTrue(out, out.contains("|30981-5^Earliest date^LN|1|20270106||"));
    assertTrue(out, out.contains("|30980-7^Recommended date^LN|1|20270106||"));
    assertTrue(out, out.contains("|59778-1^Latest date^LN|1|20270310||"));
    assertTrue(out, out.contains("|59779-9^Schedule used^LN|1|VXC16^ACIP^CDCPHINVS||"));
    assertTrue(out, out.contains("|99999-9^Vaccine Recommendation Category^LN|1|H^High-Risk^99VRC||"));
    assertTrue(out, out.contains(
        "|30982-3^Reason for Recommendation^LN|1|007^Patient is currently pregnant^99CDS||"));
    assertTrue(out, out.contains("|30982-3^Reason for Recommendation^LN|1|170^Onset of pregnancy^99CDS||"));
  }

  public void testPregnantNoIsNoRecommendation() {
    assertEquals("", forecast("OBX|1|ID|007^Patient is currently pregnant^99CDS|1|N||||||F\r"));
  }

  public void testRsvInfant() {
    String out = forecast(
        "OBX|1|ID|278^Birth mother received RSV vaccine during pregnancy^99CDS|1|Y||||||F\r");
    assertTrue(out, out.contains("|306^RSV-mAb (nirsevimab) - not needed^CVX||"));
    assertTrue(out, out.contains("LA27183-5^Immune^LN"));
    assertFalse(out, out.contains("Earliest date"));
    assertTrue(out, out.contains("|30982-3^Reason for Recommendation^LN|1|278^Birth mother"));
  }

  public void testHealthcareWorker() {
    String out = forecast("OBX|1|ID|055^Healthcare personnel^99CDS|1|Y||||||F\r");
    assertTrue(out, out.contains("|45^HepB^CVX||"));
    assertTrue(out, out.contains("|88^Influenza^CVX||"));
    // two groups: sub-IDs 1 and 2, each with a 055 reason
    assertTrue(out, out.contains("|30982-3^Reason for Recommendation^LN|1|055^Healthcare personnel^99CDS||"));
    assertTrue(out, out.contains("|30982-3^Reason for Recommendation^LN|2|055^Healthcare personnel^99CDS||"));
    assertTrue(out, out.contains("|30981-5^Earliest date^LN|2|20261029||"));
  }

  public void testUnknownCodeGetsErrNotRecommendation() {
    List<ContextualCondition> conditions =
        parse("OBX|1|ID|055^Healthcare personnel^99CDS|1|Y||||||F\r"
            + "OBX|2|ID|999^Something odd^99CDS|2|Y||||||F\r");
    List<ContextualCondition> unknown = RiskBasedForecast.findUnknown(conditions);
    assertEquals(1, unknown.size());
    String err = RiskBasedForecast.writeErrors(unknown);
    assertEquals("ERR||OBX^2^3|103^Table value not found^HL70357|W||||"
        + "Contextual condition code '999' is not recognized, it was ignored|\r", err);
    // known condition still produces its forecast
    assertEquals(2, RiskBasedForecast.build(conditions, today).size());
  }

  public void testOnlyUnknownCodeGivesNoForecast() {
    List<ContextualCondition> conditions =
        parse("OBX|1|ID|999^Something odd^99CDS|1|Y||||||F\r");
    assertEquals(0, RiskBasedForecast.build(conditions, today).size());
    assertEquals(1, RiskBasedForecast.findUnknown(conditions).size());
  }

  public void testCodeSystemTypoIsAcceptedButOtherSystemIsNot() {
    assertEquals(0, RiskBasedForecast
        .findUnknown(parse("OBX|1|ID|055^Healthcare personnel^99CDC|1|Y||||||F\r")).size());
    assertEquals(1, RiskBasedForecast
        .findUnknown(parse("OBX|1|ID|055^Healthcare personnel^LN|1|Y||||||F\r")).size());
  }
}
