package org.immregistries.iis.kernal.model;

import java.util.HashSet;
import java.util.Set;

public enum ProcessingFlavor {
                              LIME("Lime", "Missing non-critical required fields"),
                              COCONUT("Coconut", "Forecast never returned"),
                              ORANGE("Orange", "Z32 is returned for query matches"),
                              LEMON("Lemon", "Vaccinations are randomly not returned"),
                              GREEN("Green", "Typhoid vaccination is never returned"),
                              CRANBERRY("Cranberry",
                                  "Vaccination update is not accepted without at least one vaccination reported"),
                              PEAR("Pear",
                                  "The administered-at-location will not be recognized if valued and the message rejected"),
                              BLACKBERRY("Blackberry", "The patient's address is required"),
                              SPRUCE("Spruce",
                                  "The RXR segment is required for all administered vaccinations"),
                              SNAIL("Snail",
                                  "Patients are not returned until 0, 30, 60, or 90 seconds after they have been submitted"),
                              SNAIL30("Snail30",
                                  "Patients are not returned until 30 seconds after they have been submitted"),
                              SNAIL60("Snail60",
                                  "Patients are not returned until 60 seconds after they have been submitted"),
                              SNAIL90("Snail90",
                                  "Patients are not returned until 90 seconds after they have been submitted");

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
