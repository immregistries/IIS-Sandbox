package org.immregistries.iis.kernal.model;

import java.util.Date;

/**
 * Created by Eric on 12/20/17.
 */
public class VaccinationReported {
    private int vaccinationReportedId = 0;
    private PatientReported patientReported = null;
    private String vaccinationReportedExternalLink = "";
    private VaccinationMaster vaccination = null;
    private String vaccinationData = "";
    private Date reportedDate = null;
    private Date updatedDate = null;

    public int getVaccinationReportedId() {
        return vaccinationReportedId;
    }

    public void setVaccinationReportedId(int reportedVaccinationId) {
        this.vaccinationReportedId = reportedVaccinationId;
    }

    public PatientReported getPatientReported() {
		return patientReported;
	}

	public void setPatientReported(PatientReported reportedPatient) {
		this.patientReported = reportedPatient;
	}

	public String getVaccinationReportedExternalLink() {
        return vaccinationReportedExternalLink;
    }

    public void setVaccinationReportedExternalLink(String reportedOrderId) {
        this.vaccinationReportedExternalLink = reportedOrderId;
    }

    public VaccinationMaster getVaccination() {
		return vaccination;
	}

	public void setVaccination(VaccinationMaster vaccination) {
		this.vaccination = vaccination;
	}

	public String getVaccinationData() {
        return vaccinationData;
    }

    public void setVaccinationData(String vaccinationData) {
        this.vaccinationData = vaccinationData;
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
