package org.immregistries.iis.kernal.logic;

import org.hl7.fhir.r4.model.*;
import org.immregistries.iis.kernal.model.*;

import java.math.BigDecimal;
import java.util.Date;

public class ImmunizationHandler {

    public static void patientReportedFromFhirImmunization(PatientReported patientReported, Immunization i) {
        if(!i.equals(null)){
            patientReported.setReportedDate(i.getRecorded());
            patientReported.setUpdatedDate(i.getOccurrenceDateTimeType().getValue());
            patientReported.setPatientReportedAuthority(i.getIdentifierFirstRep().getValue());
            //patientReported.setPatientReportedType(patientReportedType);
        }

    }

    public static void vaccinationReportedFromFhirImmunization(VaccinationReported vaccinationReported, Immunization i) {
        //vaccinationReported.setVaccinationReportedId(0);
        vaccinationReported.setVaccinationReportedExternalLink(i.getId());
        if(i.getRecorded()!=null) {
        vaccinationReported.setReportedDate(i.getRecorded());
        }else {
            vaccinationReported.setReportedDate(new Date());
        }
        vaccinationReported.setUpdatedDate(new Date());
        vaccinationReported.setLotnumber(i.getLotNumber());
        vaccinationReported.setAdministeredDate(i.getOccurrenceDateTimeType().getValue());
        vaccinationReported.setAdministeredAmount(i.getDoseQuantity().getValue().toString());
        vaccinationReported.setExpirationDate(i.getExpirationDate());
        vaccinationReported.setVaccinationReportedExternalLink(i.getIdentifier().get(0).getValue());
        /*switch (i.getStatus().toCode()){
            case "completed" : vaccinationReported.setCompletionStatus("CP");
            case "entered-in-error" : vaccinationReported.setCompletionStatus("entered-in-error"); //TODO find accurate value
            case "not-done" : vaccinationReported.setCompletionStatus("not-done");  //TODO find accurate value
        }*/

        //vaccinationReported.setActionCode();
        vaccinationReported.setRefusalReasonCode(i.getReasonCodeFirstRep().getText());

        vaccinationReported.setVaccineCvxCode(i.getVaccineCode().getCodingFirstRep().getCode());

        VaccinationMaster vaccinationMaster = vaccinationReported.getVaccination();

        vaccinationMaster.setAdministeredDate(vaccinationReported.getAdministeredDate());
        vaccinationMaster.setVaccinationId(vaccinationReported.getVaccinationReportedId());
        vaccinationMaster.setVaccinationReported(vaccinationReported);
        vaccinationMaster.setVaccineCvxCode(vaccinationReported.getVaccineCvxCode());
    }

    public static void orgLocationFromFhirImmunization(OrgLocation orgLocation, Immunization i){
        Location l = i.getLocationTarget();
        orgLocation.setOrgFacilityCode(l.getId()); //TODO create an external identifier or change the usage of the name
        orgLocation.setOrgFacilityName(l.getName());
        //orgLocation.setLocationType(l.getTypeFirstRep());
        orgLocation.setAddressCity(l.getAddress().getLine().get(0).getValueNotNull());
        if (l.getAddress().getLine().size() > 1) {
            orgLocation.setAddressLine2(l.getAddress().getLine().get(1).getValueNotNull());
        }
        orgLocation.setAddressCity(l.getAddress().getCity());
        orgLocation.setAddressState(l.getAddress().getState());
        orgLocation.setAddressZip(l.getAddress().getPostalCode());
        orgLocation.setAddressCountry(l.getAddress().getCountry());
    }

    public static Immunization getImmunization(OrgLocation ol, VaccinationReported vr, PatientReported pr) {
	Immunization i = new Immunization();
	i.setId(vr.getVaccinationReportedExternalLink());
	i.setRecorded(vr.getReportedDate());
	i.setLotNumber(vr.getLotnumber());
	i.getOccurrenceDateTimeType().setValue(vr.getAdministeredDate());
	i.setDoseQuantity(new Quantity());
	i.getDoseQuantity().setValue(new BigDecimal(vr.getAdministeredAmount()));
	i.setExpirationDate(vr.getExpirationDate());
	if (!vr.getCompletionStatus().equals("")) {
	    i.setStatus(Immunization.ImmunizationStatus.valueOf(vr.getCompletionStatus()));
	}

	i.addReasonCode().addCoding().setCode(vr.getRefusalReasonCode());
	i.getVaccineCode().addCoding().setCode(vr.getVaccineCvxCode());
	i.setPatient(new Reference("Patient/"+pr.getPatientReportedId() ));

	Location location = i.getLocationTarget();
	if (ol != null) {
	    location.setId(ol.getOrgFacilityCode());
	    location.setName(ol.getOrgFacilityName());

	    Address address = location.getAddress();
	    address.addLine(ol.getAddressLine1());
	    address.addLine(ol.getAddressLine2());
	    address.setCity(ol.getAddressCity());
	    address.setState(ol.getAddressState());
	    address.setPostalCode(ol.getAddressZip());
	    address.setCountry(ol.getAddressCountry());
	}
    Extension links = new Extension("#links");
	Extension link = new Extension();
	link.setValue(new CodeType("level1")).setUrl("Immunization/"+vr.getVaccination().getVaccinationId())
;	//link.setProperty("target",new StringType("Immunization/123"));
	//link.setProperty("assurance",);
    links.addExtension(link);
    i.addExtension(links);


        return i;
    }

}
