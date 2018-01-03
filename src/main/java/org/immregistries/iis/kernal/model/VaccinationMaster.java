package org.immregistries.iis.kernal.model;

import java.util.Date;

/**
 * Created by Eric on 12/20/17.
 */
public class VaccinationMaster {
    private int vaccinationId = 0;
    private int patientId = 0;
    private Date administeredDate = null;
    private String vaccineCvxCode = "";

    public int getVaccinationId() {
        return vaccinationId;
    }

    public void setVaccinationId(int vaccinationId) {
        this.vaccinationId = vaccinationId;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
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


}
