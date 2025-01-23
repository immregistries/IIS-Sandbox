package org.immregistries.iis.kernal.model;

import java.io.Serializable;

public class ObservationMaster extends AbstractMappedObject implements Serializable {
  private static final long serialVersionUID = 1L;

  private String observationId = "";
  private String patientId = "";
  private String vaccinationId = "";
  private String identifierCode = "";
  private String valueCode = "";
  private ObservationReported observationReported = null;

  public String getObservationId() {
    return observationId;
  }

  public void setObservationId(String observationId) {
    this.observationId = observationId;
  }

  public void setPatient(PatientMaster patient) {
	  if (patientId != null) {
		  setPatientId(patient.getPatientId());
	  }
  }


  public void setVaccination(VaccinationMaster vaccination) {
	  if (vaccinationId != null) {
		  setVaccinationId(vaccination.getVaccinationId());
	  }
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

	public String getPatientId() {
		return patientId;
	}

	public void setPatientId(String patientId) {
		this.patientId = patientId;
	}

	public String getVaccinationId() {
		return vaccinationId;
	}

	public void setVaccinationId(String vaccinationId) {
		this.vaccinationId = vaccinationId;
	}
}
