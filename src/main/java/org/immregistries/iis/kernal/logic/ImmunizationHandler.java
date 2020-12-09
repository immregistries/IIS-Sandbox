package org.immregistries.iis.kernal.logic;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.r4.model.*;
import org.immregistries.iis.kernal.model.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

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
    }

    public static void vaccinationMasterFromFhirImmunization( VaccinationMaster vaccinationMaster, Immunization i){
        vaccinationMaster.setAdministeredDate(i.getOccurrenceDateTimeType().getValue());
        vaccinationMaster.setVaccineCvxCode(i.getVaccineCode().getCodingFirstRep().getCode());
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

    public static Immunization getImmunization(RequestDetails theRequestDetails, VaccinationReported vr) {
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
	    //if (pr != null){
            i.setPatient(new Reference(theRequestDetails.getFhirServerBase()+"/Patient/"+vr.getPatientReported().getPatientReportedExternalLink()));
        //}

	    Location location = i.getLocationTarget();
	    OrgLocation ol = vr.getOrgLocation();
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
	    Extension link;
	    link = new Extension();
	    link.setValue(new StringType(theRequestDetails.getFhirServerBase()+"/MedicationAdministration/"+vr.getVaccination().getVaccinationReported().getVaccinationReportedExternalLink()));
	    links.addExtension(link);
        i.addExtension(links);
        return i;
    }

}
