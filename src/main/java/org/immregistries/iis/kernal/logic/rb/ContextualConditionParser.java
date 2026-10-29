package org.immregistries.iis.kernal.logic.rb;

import java.util.ArrayList;
import java.util.List;
import org.immregistries.smm.tester.manager.HL7Reader;

/**
 * Reads the contextual-condition OBX segments that follow the QPD in a risk-based query (Z45).
 * The conditions are informational for the response only. They play no role in patient matching.
 */
public class ContextualConditionParser {

  private ContextualConditionParser() {}

  public static List<ContextualCondition> parse(HL7Reader reader) {
    List<ContextualCondition> conditions = new ArrayList<ContextualCondition>();
    reader.resetPostion();
    if (!reader.advanceToSegment("QPD")) {
      return conditions;
    }
    int obxPosition = 0;
    while (reader.advance()) {
      if (!"OBX".equals(reader.getSegmentName())) {
        continue;
      }
      obxPosition++;
      ContextualCondition condition = new ContextualCondition();
      condition.setObxPosition(obxPosition);
      condition.setValueType(reader.getValue(2));
      condition.setCode(reader.getValue(3, 1));
      condition.setDescription(reader.getValue(3, 2));
      condition.setCodeSystem(reader.getValue(3, 3));
      condition.setSubId(reader.getValue(4));
      condition.setValue(reader.getValue(5));
      conditions.add(condition);
    }
    reader.resetPostion();
    return conditions;
  }
}
