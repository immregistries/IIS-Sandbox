package org.immregistries.iis.kernal.logic.rb;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the scripted risk-based forecast for a Z45 query and writes it as HL7 segments, plus the
 * ERR segments for conditions the script does not recognize.
 */
public class RiskBasedForecast {

  /** LOINC for the vaccine recommendation category. The draft has none yet: placeholder. */
  public static final String CATEGORY_LOINC = "99999-9";
  public static final String CATEGORY_LOINC_LABEL = "Vaccine Recommendation Category";
  public static final String CATEGORY_TABLE = "99VRC";
  public static final String SCHEDULE_USED_LOINC = "59779-9";
  public static final String SCHEDULE_USED_VALUE = "VXC16^ACIP^CDCPHINVS";
  public static final String REASON_LOINC = "30982-3";
  public static final String REASON_LABEL = "Reason for Recommendation";

  /** One recommendation (one OBX sub-ID group in the response). */
  public static class Recommendation {
    public String cvx;
    public String cvxLabel;
    public String[] status;
    public String[] category;
    public Date earliest;
    public Date recommended;
    public Date latest;
    public List<ContextualCondition> reasons = new ArrayList<ContextualCondition>();
  }

  private RiskBasedForecast() {}

  /** Conditions that are not in the script's table. One ERR is sent per such condition. */
  public static List<ContextualCondition> findUnknown(List<ContextualCondition> conditions) {
    List<ContextualCondition> unknown = new ArrayList<ContextualCondition>();
    for (ContextualCondition condition : conditions) {
      if (!isCodeSystemOk(condition) || !RiskBasedScript.isKnownCode(condition.getCode())) {
        unknown.add(condition);
      }
    }
    return unknown;
  }

  /** 99CDC is accepted too, because the draft uses both spellings. */
  private static boolean isCodeSystemOk(ContextualCondition condition) {
    String system = condition.getCodeSystem();
    return system.equals("") || system.equals(ContextualCondition.CODE_SYSTEM)
        || system.equals("99CDC");
  }

  public static List<Recommendation> build(List<ContextualCondition> conditions, Date today) {
    Map<String, Recommendation> byCvx = new LinkedHashMap<String, Recommendation>();
    for (ContextualCondition condition : conditions) {
      if (!isCodeSystemOk(condition) || !isActive(condition)) {
        continue;
      }
      for (RiskBasedScript.Rule rule : RiskBasedScript.getRulesForCondition(condition.getCode())) {
        Recommendation recommendation = byCvx.get(rule.cvx);
        if (recommendation == null) {
          recommendation = new Recommendation();
          recommendation.cvx = rule.cvx;
          recommendation.cvxLabel = rule.cvxLabel;
          recommendation.status = rule.status;
          recommendation.category = rule.category;
          applyDates(recommendation, rule, conditions, today);
          byCvx.put(rule.cvx, recommendation);
        }
        recommendation.reasons.add(condition);
        for (String echoCode : rule.alsoEchoCodes) {
          for (ContextualCondition other : conditions) {
            if (other.getCode().equals(echoCode) && !recommendation.reasons.contains(other)) {
              recommendation.reasons.add(other);
            }
          }
        }
      }
    }
    return new ArrayList<Recommendation>(byCvx.values());
  }

  /** A Y/N condition applies only when Y. Other value types (dates) apply when present. */
  private static boolean isActive(ContextualCondition condition) {
    if ("ID".equals(condition.getValueType()) || "N".equalsIgnoreCase(condition.getValue())) {
      return condition.isTrue();
    }
    return !condition.getValue().equals("");
  }

  private static void applyDates(Recommendation recommendation, RiskBasedScript.Rule rule,
      List<ContextualCondition> conditions, Date today) {
    Date base = null;
    if (rule.dateRule == RiskBasedScript.DateRule.FROM_TODAY) {
      base = today;
    } else if (rule.dateRule == RiskBasedScript.DateRule.FROM_CONDITION_DATE) {
      for (ContextualCondition condition : conditions) {
        if (condition.getCode().equals(rule.dateConditionCode)) {
          base = condition.getDateValue();
        }
      }
    }
    if (base != null) {
      recommendation.earliest = addDays(base, rule.earliestDays);
      recommendation.recommended = addDays(base, rule.recommendedDays);
      recommendation.latest = addDays(base, rule.latestDays);
    }
  }

  private static Date addDays(Date date, int days) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    calendar.add(Calendar.DAY_OF_MONTH, days);
    return calendar.getTime();
  }

  /**
   * Writes the RXA (998, no vaccination administered) followed by the OBX segments of every
   * recommendation. The caller prints the ORC before this. Returns an empty string when there are
   * no recommendations.
   */
  public static String writeForecast(List<Recommendation> recommendations, Date today,
      int obxSetId, int obsSubId) {
    if (recommendations.isEmpty()) {
      return "";
    }
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    StringBuilder sb = new StringBuilder();
    sb.append("RXA|0|1|" + sdf.format(today) + "||998^No Vaccination Administered^CVX|999||||||||||||||NA\r");
    for (Recommendation recommendation : recommendations) {
      obsSubId++;
      obxSetId = obx(sb, obxSetId, "CE", "30956-7^Vaccine type^LN", obsSubId,
          recommendation.cvx + "^" + recommendation.cvxLabel + "^CVX");
      obxSetId = obx(sb, obxSetId, "CE", "59783-1^Status in series^LN", obsSubId,
          recommendation.status[0] + "^" + recommendation.status[1] + "^LN");
      if (recommendation.earliest != null) {
        obxSetId = obx(sb, obxSetId, "DT", "30981-5^Earliest date^LN", obsSubId,
            sdf.format(recommendation.earliest));
        obxSetId = obx(sb, obxSetId, "DT", "30980-7^Recommended date^LN", obsSubId,
            sdf.format(recommendation.recommended));
        obxSetId = obx(sb, obxSetId, "DT", "59778-1^Latest date^LN", obsSubId,
            sdf.format(recommendation.latest));
      }
      obxSetId = obx(sb, obxSetId, "CE", SCHEDULE_USED_LOINC + "^Schedule used^LN", obsSubId,
          SCHEDULE_USED_VALUE);
      obxSetId = obx(sb, obxSetId, "CE",
          CATEGORY_LOINC + "^" + CATEGORY_LOINC_LABEL + "^LN", obsSubId,
          recommendation.category[0] + "^" + recommendation.category[1] + "^" + CATEGORY_TABLE);
      for (ContextualCondition reason : recommendation.reasons) {
        String description = reason.getDescription();
        if (description.equals("")) {
          description = RiskBasedScript.getDescription(reason.getCode());
        }
        obxSetId = obx(sb, obxSetId, "CE", REASON_LOINC + "^" + REASON_LABEL + "^LN", obsSubId,
            reason.getCode() + "^" + description + "^" + ContextualCondition.CODE_SYSTEM);
      }
    }
    return sb.toString();
  }

  private static int obx(StringBuilder sb, int obxSetId, String valueType, String identifier,
      int obsSubId, String value) {
    obxSetId++;
    sb.append("OBX|" + obxSetId + "|" + valueType + "|" + identifier + "|" + obsSubId + "|" + value
        + "||||||F\r");
    return obxSetId;
  }

  /**
   * One ERR segment (warning) per unrecognized condition. The query is not rejected, the condition
   * is just ignored.
   */
  public static String writeErrors(List<ContextualCondition> unknown) {
    StringBuilder sb = new StringBuilder();
    for (ContextualCondition condition : unknown) {
      boolean codeSystemProblem = !isCodeSystemOk(condition);
      sb.append("ERR||OBX^" + condition.getObxPosition() + "^3|103^Table value not found^HL70357|W||||");
      if (codeSystemProblem) {
        sb.append("Contextual condition code system '" + condition.getCodeSystem()
            + "' is not recognized (expected 99CDS), condition '" + condition.getCode()
            + "' was ignored");
      } else {
        sb.append("Contextual condition code '" + condition.getCode()
            + "' is not recognized, it was ignored");
      }
      sb.append("|\r");
    }
    return sb.toString();
  }
}
