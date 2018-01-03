package org.immregistries.iis.kernal.model;

import java.util.Date;

/**
 * Created by Eric on 12/20/17.
 */
public class VaccinationReported {
    private int reportedVaccinationId = 0;
    private int reportedPatientId = 0;
    private int reportedOrderId = 0;
    private int vaccinationId = 0;
    private String vaccinationData = "";
    private Date reportedDate = null;
    private Date updatedDate = null;

    public int getReportedVaccinationId() {
        return reportedVaccinationId;
    }

    public void setReportedVaccinationId(int reportedVaccinationId) {
        this.reportedVaccinationId = reportedVaccinationId;
    }

    public int getReportedPatientId() {
        return reportedPatientId;
    }

    public void setReportedPatientId(int reportedPatientId) {
        this.reportedPatientId = reportedPatientId;
    }

    public int getReportedOrderId() {
        return reportedOrderId;
    }

    public void setReportedOrderId(int reportedOrderId) {
        this.reportedOrderId = reportedOrderId;
    }

    public int getVaccinationId() {
        return vaccinationId;
    }

    public void setVaccinationId(int vaccinationId) {
        this.vaccinationId = vaccinationId;
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
