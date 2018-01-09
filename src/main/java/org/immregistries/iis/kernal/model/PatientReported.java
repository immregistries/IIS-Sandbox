package org.immregistries.iis.kernal.model;

import java.util.Date;

/**
 * Created by Eric on 12/20/17.
 */
public class PatientReported {
    private int reportedPatientId = 0;
    private OrgMaster reportedOrg = null;
    private String reportedMrn = "";
    private PatientMaster patient = null;
    private String patientData = "";
    private Date reportedDate = null;
    private Date updatedDate = null;

    public OrgMaster getReportedOrg() {
      return reportedOrg;
    }

    public void setReportedOrg(OrgMaster reportedOrg) {
      this.reportedOrg = reportedOrg;
    }

    public int getReportedPatientId() {
        return reportedPatientId;
    }

    public void setReportedPatientId(int reportedPatientId) {
        this.reportedPatientId = reportedPatientId;
    }

    public String getReportedMrn() {
        return reportedMrn;
    }

    public void setReportedMrn(String reportedMrn) {
        this.reportedMrn = reportedMrn;
    }

    public PatientMaster getPatient() {
		return patient;
	}

	public void setPatient(PatientMaster patient) {
		this.patient = patient;
	}

	public String getPatientData() {
        return patientData;
    }

    public void setPatientData(String patientData) {
        this.patientData = patientData;
    }

    public Date getReportedDate() {
        return reportedDate;
    }

    public void setReportedDate(Date reportedDate) {
        this.reportedDate = reportedDate;
    }

    public Date getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }

}
