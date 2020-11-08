package org.immregistries.iis.kernal.logic;

import org.hl7.fhir.r4.model.*;
import org.immregistries.iis.kernal.model.*;


import java.math.BigDecimal;
import java.util.*;

public class PatientHandler {
    public static void patientReportedFromFhirPatient(PatientReported patientReported, Patient p) {
        //patientReported.setPatientReportedId(;
        //patientReported.setPatientReportedType(p.get);
        patientReported.setReportedDate(new Date());
        patientReported.setPatientReportedExternalLink(p.getIdentifier().get(0).getValue()); //TODO modify
        { //Name
            HumanName name = p.getNameFirstRep();
            patientReported.setPatientNameLast(name.getFamily());
            if (name.getGiven().size() > 0) {
                patientReported.setPatientNameFirst(name.getGiven().get(0).getValueNotNull());
            }
            if (name.getGiven().size() > 1) {
                patientReported.setPatientNameMiddle(name.getGiven().get(1).getValueNotNull());
            }
        }

        patientReported.setPatientBirthDate(p.getBirthDate());
        patientReported.setPatientSex(String.valueOf(p.getGender().toString().charAt(0))); // Get the first char of MALE or FEMALE -> "M" or "F"

        { // Address
            Address address = p.getAddressFirstRep();
            if (address.getLine().size() > 0) {
                patientReported.setPatientAddressLine1(address.getLine().get(0).getValueNotNull());
            }
            if (address.getLine().size() > 1) {
                patientReported.setPatientAddressLine2(address.getLine().get(1).getValueNotNull());
            }
            patientReported.setPatientAddressCity(address.getCity());
            patientReported.setPatientAddressState(address.getState());
            patientReported.setPatientAddressZip(address.getPostalCode());
            patientReported.setPatientAddressCountry(address.getCountry());
            patientReported.setPatientAddressCountyParish(address.getDistrict());
        }
        for (ContactPoint contact : p.getTelecom()) {
            System.err.println(contact.getSystem());
            if (contact.getSystem().equals(ContactPoint.ContactPointSystem.PHONE)) {
                patientReported.setPatientPhone(contact.getValue());
            } else if (contact.getSystem().equals(ContactPoint.ContactPointSystem.EMAIL)) {
                patientReported.setPatientEmail(contact.getValue());
            }
        }
        //patientReported.setPatientBirthFlag(p.getBirthDate().toString()); //TODO look for flag format
        //patientReported.setPatientBirthOrder(patientBirthOrder);
        //patientReported.setPatientDeathFlag(p.getDeceasedBooleanType().toString());

        if (null != p.getDeceased()) {
            if (p.getDeceasedBooleanType().isBooleanPrimitive()) {
                patientReported.setPatientDeathFlag(p.getDeceasedBooleanType().toString());
            }
            //System.out.println(p.getDeceased().getId());
            //System.out.println(p.getDeceasedDateTimeType().);
            if (p.getDeceased().isDateTime()){
                patientReported.setPatientDeathDate(p.getDeceasedDateTimeType().getValue());

            }
        }

        //patientReported.setRegistryStatusIndicator(p.getActive());
        { // Patient Contact / Guardian
            Patient.ContactComponent contact = p.getContactFirstRep();
            patientReported.setGuardianLast(contact.getName().getFamily());
            if (p.getContactFirstRep().getName().getGiven().size() > 0) {
                patientReported.setGuardianFirst(contact.getName().getGiven().get(0).getValueNotNull());
            }
            if (p.getContactFirstRep().getName().getGiven().size() > 1) {
                patientReported.setGuardianMiddle(contact.getName().getGiven().get(1).getValueNotNull());
            }
            patientReported.setGuardianRelationship(contact.getRelationshipFirstRep().getText());
        }
        { // PatientMaster Ressource
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

    public static Patient getPatient(OrgLocation orgLocation, VaccinationReported vaccinationReported, PatientReported pr){
        Patient p = new Patient();
        p.setId(pr.getPatientReportedExternalLink());

        HumanName name = p.addName();
        name.setFamily(pr.getPatientNameLast());
        name.addGivenElement().setValue(pr.getPatientNameFirst());
        name.addGivenElement().setValue(pr.getPatientNameMiddle());

        p.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue(pr.getPatientEmail());
        p.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue(pr.getPatientPhone());
        switch (pr.getPatientSex()){
            case "M":p.setGender(Enumerations.AdministrativeGender.MALE);break;
            case "F":p.setGender(Enumerations.AdministrativeGender.FEMALE);break;
            default:p.setGender(Enumerations.AdministrativeGender.OTHER);
        }
        p.setBirthDate(pr.getPatientBirthDate());
        if (null == pr.getPatientDeathDate()){
            p.getDeceasedBooleanType().setValue(false);
        }else {
            p.getDeceasedBooleanType().setValue(true);
            p.getDeceasedDateTimeType().setValue(pr.getPatientDeathDate());
        }

        Address address = p.addAddress();
        address.addLine(pr.getPatientAddressLine1());
        address.addLine(pr.getPatientAddressLine2());
        address.setCity(pr.getPatientAddressCity());
        address.setCountry(pr.getPatientAddressCountry());
        address.setState(pr.getPatientAddressState());
        address.setPostalCode(pr.getPatientAddressZip());

        //TODO deal with contact (maybe create an id in the DB ?)
        Patient.ContactComponent contact = p.addContact();
        HumanName contactName = new HumanName();
        contactName.setFamily(pr.getGuardianLast());
        contactName.addGivenElement().setValue(pr.getGuardianFirst());
        contactName.addGivenElement().setValue(pr.getGuardianMiddle());
        contact.setName(contactName);
        return p;
    }


}
