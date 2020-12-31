package org.immregistries.iis.kernal;

import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.logic.CodeMapManager;
import junit.framework.TestCase;

public class TestCodeSet extends TestCase {

  public void testCodeMapManager() {
    CodeMap codeMap = CodeMapManager.getCodeMap();
    Code code = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, "03");
    assertNotNull(code);
    assertEquals("03", code.getValue());
    assertEquals("MMR", code.getLabel());
  }
}
