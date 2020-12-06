package org.immregistries.iis.kernal.model;

import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Patient;

import java.io.Serializable;
import java.util.Date;


public class VaccinationLink implements Serializable {
    private int id;
    private VaccinationMaster vaccinationMaster = null;
    private VaccinationReported vaccinationReported =null;

    private int levelConfidence;

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public int getLevelConfidence() {
        return levelConfidence;
    }
    public void setLevelConfidence(int levelConfidence) {
        this.levelConfidence = levelConfidence;
    }
    public VaccinationMaster getVaccinationMaster() {
        return vaccinationMaster;
    }
    public void setVaccinationMaster(VaccinationMaster vaccinationMaster) {
        this.vaccinationMaster = vaccinationMaster;
    }
    public VaccinationReported getVaccinationReported() {
        return vaccinationReported;
    }
    public void setVaccinationReported(VaccinationReported vaccinationReported) {
        this.vaccinationReported = vaccinationReported;
    }
}
