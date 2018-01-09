package org.immregistries.iis.kernal.model;

import java.util.Date;

/**
 * Created by Eric on 12/20/17.
 */
public class VaccinationReported {
    private int reportedVaccinationId = 0;
    private PatientReported reportedPatient = null;
    private int reportedOrderId = 0;
    private VaccinationMaster vaccination = null;
    private String vaccinationData = "";
    private Date reportedDate = null;
    private Date updatedDate = null;

    public int getReportedVaccinationId() {
        return reportedVaccinationId;
    }

    public void setReportedVaccinationId(int reportedVaccinationId) {
        this.reportedVaccinationId = reportedVaccinationId;
    }

    public PatientReported getReportedPatient() {
		return reportedPatient;
	}

	public void setReportedPatient(PatientReported reportedPatient) {
		this.reportedPatient = reportedPatient;
	}

	public int getReportedOrderId() {
        return reportedOrderId;
    }

    public void setReportedOrderId(int reportedOrderId) {
        this.reportedOrderId = reportedOrderId;
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
