package org.immregistries.iis.kernal.model;

import java.util.Date;

/**
 * Created by Eric on 12/20/17.
 */
public class PatientReported {
    private int patientReportedId = 0;
    private OrgMaster orgReported = null;
    private String patientReportedExternalLink = "";
    private PatientMaster patient = null;
    private String patientData = "";
    private Date reportedDate = null;
    private Date updatedDate = null;

    public OrgMaster getOrgReported() {
      return orgReported;
    }

    public void setOrgReported(OrgMaster reportedOrg) {
      this.orgReported = reportedOrg;
    }

    public int getPatientReportedId() {
        return patientReportedId;
    }

    public void setPatientReportedId(int reportedPatientId) {
        this.patientReportedId = reportedPatientId;
    }

    public String getPatientReportedExternalLink() {
        return patientReportedExternalLink;
    }

    public void setPatientReportedExternalLink(String reportedMrn) {
        this.patientReportedExternalLink = reportedMrn;
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
