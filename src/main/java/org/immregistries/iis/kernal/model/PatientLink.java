package org.immregistries.iis.kernal.model;

import java.io.Serializable;

/**
 * Created by Eric on 12/20/17.
 */
public class PatientLink implements Serializable {
  private int id;
  private PatientMaster patientMaster = null;
  private PatientReported patientReported = null;
  /*private int patientMasterId= 0;
  private int patientReportedId=0;*/

  private int levelConfidence;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public PatientMaster getPatientMaster() {
    return patientMaster;
  }

  public void setPatientMaster(PatientMaster patientMaster) {
    this.patientMaster = patientMaster;
  }

  public PatientReported getPatientReported() {
    return patientReported;
  }

  public void setPatientReported(PatientReported patientReported) {
    this.patientReported = patientReported;
  }

  public int getLevelConfidence() {
    return levelConfidence;
  }

  public void setLevelConfidence(int levelConfidence) {
    this.levelConfidence = levelConfidence;
  }

  /*public int getPatientMasterId() {
  	return patientMasterId;
  }
  
  public void setPatientMasterId(int patientMasterId) {
  	this.patientMasterId = patientMasterId;
  }
  
  public int getPatientReportedId() {
  	return patientReportedId;
  }
  
  public void setPatientReportedId(int patientReportedId) {
  	this.patientReportedId = patientReportedId;
  }*/
}
