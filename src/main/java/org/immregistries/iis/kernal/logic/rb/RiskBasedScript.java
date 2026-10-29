package org.immregistries.iis.kernal.logic.rb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The scripted (canned) risk-based forecast used for the RB demo. This is NOT a real forecaster:
 * each contextual condition code maps to a fixed recommendation. Edit the table in the static block
 * to change what the demo returns.
 *
 * Columns of a rule: condition code, CVX, vaccine label, status-in-series LA code, status label,
 * category code (99VRC placeholder), category label, date rule, extra condition codes echoed in the
 * reason for recommendation.
 */
public class RiskBasedScript {

  /** How the dates of a recommendation are derived. */
  public enum DateRule {
    /** No earliest/recommended/latest dates. */
    NONE,
    /** Offsets in days from the date value of another condition (dateConditionCode). */
    FROM_CONDITION_DATE,
    /** Offsets in days from the day the query is answered. */
    FROM_TODAY
  }

  /** Status in series (59783-1) answers. Only LA codes required by the draft are used. */
  public static final String[] STATUS_COMPLETE = {"LA13421-5", "Complete"};
  public static final String[] STATUS_ON_SCHEDULE = {"LA13422-3", "On schedule"};
  public static final String[] STATUS_IMMUNE = {"LA27183-5", "Immune"};
  public static final String[] STATUS_CONTRAINDICATED = {"LA4216-3", "Contraindicated"};

  /** Placeholder 99VRC values. The draft lists Routine, SCDM and High-Risk but no codes yet. */
  public static final String[] CATEGORY_ROUTINE = {"R", "Routine"};
  public static final String[] CATEGORY_SCDM = {"S", "SCDM"};
  public static final String[] CATEGORY_HIGH_RISK = {"H", "High-Risk"};

  public static class Rule {
    public String conditionCode;
    public String cvx;
    public String cvxLabel;
    public String[] status;
    public String[] category;
    public DateRule dateRule = DateRule.NONE;
    public String dateConditionCode = "";
    public int earliestDays = 0;
    public int recommendedDays = 0;
    public int latestDays = 0;
    /** Other condition codes that are echoed as reasons when present in the query. */
    public List<String> alsoEchoCodes = new ArrayList<String>();
  }

  private static final List<Rule> RULE_LIST = new ArrayList<Rule>();

  /** Condition codes the demo knows about, with a description. Others get an ERR in the RSP. */
  private static final Map<String, String> KNOWN_CODES = new LinkedHashMap<String, String>();

  static {
    String[][] known = {{"007", "Patient is currently pregnant"}, {"170", "Onset of pregnancy"},
        {"016", "Chronic heart disease"},
        {"023", "Laboratory Evidence of Immunity or confirmation of Varicella disease"},
        {"024", "Healthcare provider verified history of or diagnosis of Varicella"},
        {"045", "Travel to country with a Yellow Fever vaccination entry requirement"},
        {"053", "Rabies researchers"}, {"055", "Healthcare personnel"},
        {"056", "Health-care workers who have close contact with patients who might be excreting wild polioviruses"},
        {"057", "Public safety worker exposed to blood or infection body fluids"},
        {"058", "Staff of institutions for persons with developmental disabilities"},
        {"059", "Occupation exposure for Hepatitis A"},
        {"278", "Birth mother received RSV vaccine during pregnancy"}};
    for (String[] k : known) {
      KNOWN_CODES.put(k[0], k[1]);
    }

    // Pregnant: Tdap, window 27 to 36 weeks after the pregnancy onset date (condition 170).
    Rule rule = rule("007", "115", "Tdap", STATUS_ON_SCHEDULE, CATEGORY_HIGH_RISK);
    rule.dateRule = DateRule.FROM_CONDITION_DATE;
    rule.dateConditionCode = "170";
    rule.earliestDays = 27 * 7;
    rule.recommendedDays = 27 * 7;
    rule.latestDays = 36 * 7;
    rule.alsoEchoCodes.add("170");

    // Birth mother had the RSV vaccine in pregnancy: infant does not need nirsevimab.
    rule("278", "306", "RSV-mAb (nirsevimab) - not needed", STATUS_IMMUNE, CATEGORY_ROUTINE);

    // Healthcare personnel: Hep B and influenza.
    rule("055", "45", "HepB", STATUS_ON_SCHEDULE, CATEGORY_HIGH_RISK);
    rule = rule("055", "88", "Influenza", STATUS_ON_SCHEDULE, CATEGORY_ROUTINE);
    rule.dateRule = DateRule.FROM_TODAY;
    rule.earliestDays = 0;
    rule.recommendedDays = 0;
    rule.latestDays = 90;

    // Extra rows to show the table is data driven.
    rule("045", "37", "Yellow Fever", STATUS_ON_SCHEDULE, CATEGORY_HIGH_RISK);
    rule("059", "85", "HepA", STATUS_ON_SCHEDULE, CATEGORY_HIGH_RISK);
  }

  private static Rule rule(String conditionCode, String cvx, String cvxLabel, String[] status,
      String[] category) {
    Rule rule = new Rule();
    rule.conditionCode = conditionCode;
    rule.cvx = cvx;
    rule.cvxLabel = cvxLabel;
    rule.status = status;
    rule.category = category;
    RULE_LIST.add(rule);
    return rule;
  }

  private RiskBasedScript() {}

  public static List<Rule> getRules() {
    return Collections.unmodifiableList(RULE_LIST);
  }

  public static boolean isKnownCode(String code) {
    return KNOWN_CODES.containsKey(code);
  }

  public static String getDescription(String code) {
    return KNOWN_CODES.get(code);
  }

  public static List<Rule> getRulesForCondition(String code) {
    List<Rule> list = new ArrayList<Rule>();
    for (Rule rule : RULE_LIST) {
      if (rule.conditionCode.equals(code)) {
        list.add(rule);
      }
    }
    return list;
  }
}
