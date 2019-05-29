package org.immregistries.iis.kernal.model;

import java.util.HashSet;
import java.util.Set;

public enum ProcessingFlavor {
                              LIME("Lime", "Missing non-critical required fields"),
                              COCONUT("Coconut", "Forecast never returned"),
                              ORANGE("Orange", "Z32 is returned for query matches"),
                              LEMON("Lemon", "Vaccinations are randomly not returned");

  private String key = "";
  private String behaviorDescription = "";

  private ProcessingFlavor(String key, String behaviorDescription) {
    this.key = key;
    this.behaviorDescription = behaviorDescription;
  }

  public String getKey() {
    return key;
  }

  public static Set<ProcessingFlavor> getProcessingStyle(String label) {
    Set<ProcessingFlavor> processingFlavorSet = new HashSet<>();
    if (label != null) {
      label = label.toUpperCase();
      for (ProcessingFlavor ps : ProcessingFlavor.values()) {
        String key = ps.key.toUpperCase();
        if (label.startsWith(key + " ") || label.endsWith(" " + key)
            || label.indexOf(" " + key + " ") > 0) {
          processingFlavorSet.add(ps);
        }
      }
    }
    return processingFlavorSet;
  }

  public String getBehaviorDescription() {
    return behaviorDescription;
  }

}
