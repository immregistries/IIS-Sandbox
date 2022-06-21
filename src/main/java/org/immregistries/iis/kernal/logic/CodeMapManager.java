package org.immregistries.iis.kernal.logic;

import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.CodeMapBuilder;

import java.io.InputStream;

public class CodeMapManager {

  private static CodeMapManager singleton = null;

  public static CodeMap getCodeMap() {
    if (singleton == null) {
      singleton = new CodeMapManager();
    }
    return singleton.codeMap;
  }

  CodeMapBuilder builder = CodeMapBuilder.INSTANCE;
  CodeMap codeMap = null;

  public CodeMapManager() {
    InputStream is = this.getClass().getResourceAsStream("/Compiled.xml");
    if (is == null) {
      System.err.println("Unable to find Compiled.xml!");
    }
    codeMap = builder.getCodeMap(is);
  }
}
