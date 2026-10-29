package org.immregistries.iis.kernal.logic.rb;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * One contextual condition sent with a risk-based query (QBP Z45), read from an OBX segment that
 * follows the QPD. The condition is identified by a code in the local code system 99CDS, for
 * example 007 (patient is currently pregnant) with a Y/N value, or 170 (onset of pregnancy) with a
 * date value.
 */
public class ContextualCondition {

  public static final String CODE_SYSTEM = "99CDS";

  private int obxPosition = 0; // ordinal of the OBX among the OBX segments of the message
  private String valueType = "";
  private String code = "";
  private String description = "";
  private String codeSystem = "";
  private String subId = "";
  private String value = "";

  public int getObxPosition() {
    return obxPosition;
  }

  public void setObxPosition(int obxPosition) {
    this.obxPosition = obxPosition;
  }

  public String getValueType() {
    return valueType;
  }

  public void setValueType(String valueType) {
    this.valueType = valueType;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getCodeSystem() {
    return codeSystem;
  }

  public void setCodeSystem(String codeSystem) {
    this.codeSystem = codeSystem;
  }

  public String getSubId() {
    return subId;
  }

  public void setSubId(String subId) {
    this.subId = subId;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  /** True when the value is Y (case insensitive). */
  public boolean isTrue() {
    return "Y".equalsIgnoreCase(value);
  }

  /** Reads the value as a yyyyMMdd date (anything after the first 8 characters is ignored). */
  public Date getDateValue() {
    if (value == null || value.length() < 8) {
      return null;
    }
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    sdf.setLenient(false);
    try {
      return sdf.parse(value.substring(0, 8));
    } catch (ParseException e) {
      return null;
    }
  }

  @Override
  public String toString() {
    return code + "=" + value;
  }
}
