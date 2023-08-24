package org.immregistries.iis.kernal.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by Eric on 12/20/17.
 */
public class PatientReported extends PatientMaster implements Serializable {
	PatientMaster patient;
  public PatientMaster getPatient() {
    return patient;
  }

  public void setPatient(PatientMaster patient) {
    this.patient = patient;
  }

}
