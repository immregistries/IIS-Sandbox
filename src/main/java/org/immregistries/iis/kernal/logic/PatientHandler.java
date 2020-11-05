package org.immregistries.iis.kernal.logic;

import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Patient;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.model.VaccinationReported;

import java.util.Date;

public class PatientHandler {
    public static void patientReportedFromFhirPatient(PatientReported patientReported, Patient p) {
        //patientReported.setPatientReportedId(;
        //patientReported.setPatientReportedType(p.get);
        patientReported.setReportedDate(new Date());
        patientReported.setPatientReportedExternalLink(p.getId()); //TODO modify
        patientReported.setPatientNameLast(p.getNameFirstRep().getFamily());
        patientReported.setPatientNameFirst(p.getNameFirstRep().getGiven().get(0).getValueNotNull());
        if (p.getNameFirstRep().getGiven().size() > 1) {
            patientReported.setPatientNameMiddle(p.getNameFirstRep().getGiven().get(1).getValueNotNull());
        }
        patientReported.setPatientBirthDate(p.getBirthDate());
        patientReported.setPatientSex(String.valueOf(p.getGender().toString().charAt(0))); // Get the first char of MALE or FEMALE
        patientReported.setPatientAddressLine1(p.getAddress().get(0).getLine().get(0).getValueNotNull());
        if (p.getAddress().get(0).getLine().size() > 1) {
            patientReported.setPatientAddressLine2(p.getAddress().get(0).getLine().get(1).getValueNotNull());
        }
        patientReported.setPatientAddressCity(p.getAddress().get(0).getCity());
        patientReported.setPatientAddressState(p.getAddress().get(0).getState());
        patientReported.setPatientAddressZip(p.getAddress().get(0).getPostalCode());
        patientReported.setPatientAddressCountry(p.getAddress().get(0).getCountry());
        patientReported.setPatientAddressCountyParish(p.getAddress().get(0).getDistrict());
        for (ContactPoint contact : p.getTelecom()) {
            if (contact.getSystem().name().equals("PHONE")) {
                patientReported.setPatientPhone(p.getTelecomFirstRep().getValue());
            } else if (contact.getSystem().name().equals("EMAIL")) {
                patientReported.setPatientEmail(p.getTelecom().get(1).getValue());
            }
        }
        patientReported.setPatientBirthFlag(p.getBirthDate().toString()); //TODO look for flag format
        //patientReported.setPatientBirthOrder(patientBirthOrder);
        //patientReported.setPatientDeathFlag(p.getDeceasedBooleanType().toString());
        if (p.getDeceasedBooleanType().booleanValue()) {
            patientReported.setPatientDeathDate(p.getDeceasedDateTimeType().getValue());
        }
        //patientReported.setRegistryStatusIndicator(p.getActive());
        patientReported.setGuardianLast(p.getContactFirstRep().getName().getFamily());
        patientReported.setGuardianFirst(p.getContactFirstRep().getName().getGiven().get(0).getValueNotNull());
        if (p.getContactFirstRep().getName().getGiven().size() > 1) {
            patientReported.setGuardianMiddle(p.getContactFirstRep().getName().getGiven().get(1).getValueNotNull());
        }
        patientReported.setGuardianRelationship(p.getContactFirstRep().getRelationshipFirstRep().getText());

        PatientMaster patientMaster = patientReported.getPatient();
        patientMaster.setPatientId(patientReported.getPatientReportedId());
        patientMaster.setPatientExternalLink(patientReported.getPatientReportedExternalLink());
        patientMaster.setPatientNameLast(patientReported.getPatientNameLast());
        patientMaster.setPatientNameFirst(patientReported.getPatientNameFirst());
        patientMaster.setPatientNameMiddle(patientReported.getPatientNameMiddle());
        patientMaster.setPatientBirthDate(patientReported.getPatientBirthDate());
        patientMaster.setPatientPhoneFrag(patientReported.getPatientPhone());
        patientMaster.setPatientAddressFrag(patientReported.getPatientAddressZip());
    }

}
