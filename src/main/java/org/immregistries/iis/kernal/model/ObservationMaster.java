package org.immregistries.iis.kernal.model;

import java.io.Serializable;

public class ObservationMaster implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  
  private int observationId = 0;
  private PatientMaster patient = null;
  private VaccinationMaster vaccination = null;
  private String identifierCode = "";
  private String valueCode = "";
  private ObservationReported observationReported = null;

  public int getObservationId() {
    return observationId;
  }

  public void setObservationId(int observationId) {
    this.observationId = observationId;
  }

  public PatientMaster getPatient() {
    return patient;
  }

  public void setPatient(PatientMaster patient) {
    this.patient = patient;
  }

  public VaccinationMaster getVaccination() {
    return vaccination;
  }

  public void setVaccination(VaccinationMaster vaccination) {
    this.vaccination = vaccination;
  }

  public String getIdentifierCode() {
    return identifierCode;
  }

  public void setIdentifierCode(String identifierCode) {
    this.identifierCode = identifierCode;
  }

  public String getValueCode() {
    return valueCode;
  }

  public void setValueCode(String valueCode) {
    this.valueCode = valueCode;
  }

  public ObservationReported getObservationReported() {
    return observationReported;
  }

  public void setObservationReported(ObservationReported observationReported) {
    this.observationReported = observationReported;
  }

}
