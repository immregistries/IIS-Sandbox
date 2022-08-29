package org.immregistries.iis.kernal.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by Eric on 12/20/17.
 */
public class VaccinationMaster implements Serializable {
  private static final long serialVersionUID = 1L;

  private String vaccinationId = "";
  private PatientMaster patient = null;
  private Date administeredDate = null;
  private String vaccineCvxCode = "";
  private VaccinationReported vaccinationReported = null;

	private String externalLink = "";

  public VaccinationReported getVaccinationReported() {
    return vaccinationReported;
  }

  public void setVaccinationReported(VaccinationReported vaccinationReported) {
    this.vaccinationReported = vaccinationReported;
  }

  public String getVaccinationId() {
    return vaccinationId;
  }

  public void setVaccinationId(String vaccinationId) {
    this.vaccinationId = vaccinationId;
  }

  public PatientMaster getPatient() {
    return patient;
  }

  public void setPatient(PatientMaster patient) {
    this.patient = patient;
  }

  public Date getAdministeredDate() {
    return administeredDate;
  }

  public void setAdministeredDate(Date administeredDate) {
    this.administeredDate = administeredDate;
  }

  public String getVaccineCvxCode() {
    return vaccineCvxCode;
  }

  public void setVaccineCvxCode(String vaccineCvxCode) {
    this.vaccineCvxCode = vaccineCvxCode;
  }

	public String getExternalLink() {
		return externalLink;
	}

	public void setExternalLink(String externalLink) {
		this.externalLink = externalLink;
	}
}
