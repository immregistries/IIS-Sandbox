package org.immregistries.iis.kernal.logic;

import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Patient;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.model.VaccinationReported;

public class ImmunizationHandler {

    public static void patientReportedFromFhirImmunization(PatientReported patientReported, Immunization i) {
        patientReported.setReportedDate(i.getRecorded());
        patientReported.setUpdatedDate(i.getOccurrenceDateTimeType().getValue());
        patientReported.setPatientReportedAuthority(i.getIdentifierFirstRep().getValue());
        //patientReported.setPatientReportedType(patientReportedType);
    }

    public static void vaccinationReportedFromFhirImmunization(VaccinationReported vaccinationReported, Immunization i) {
        //vaccinationReported.setVaccinationReportedId(0);
        vaccinationReported.setVaccinationReportedExternalLink(i.getId());
        vaccinationReported.setReportedDate(i.getRecorded());
        vaccinationReported.setUpdatedDate(i.getOccurrenceDateTimeType().getValue());
        vaccinationReported.setLotnumber(i.getLotNumber());
        vaccinationReported.setAdministeredDate(i.getOccurrenceDateTimeType().getValue());
        vaccinationReported.setAdministeredAmount(i.getDoseQuantity().getValue().toString());
        vaccinationReported.setExpirationDate(i.getExpirationDate());
        vaccinationReported.setCompletionStatus(i.getStatus().toString());

        VaccinationMaster vaccinationMaster = vaccinationReported.getVaccination();

        vaccinationMaster.setAdministeredDate(vaccinationReported.getAdministeredDate());
        vaccinationMaster.setVaccinationId(vaccinationReported.getVaccinationReportedId());
        vaccinationMaster.setVaccinationReported(vaccinationReported);
        vaccinationMaster.setVaccineCvxCode(vaccinationReported.getVaccineCvxCode());
    }

}
