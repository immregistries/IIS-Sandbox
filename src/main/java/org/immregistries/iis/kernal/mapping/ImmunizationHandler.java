package org.immregistries.iis.kernal.mapping;

import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.model.VaccinationReported;

import java.math.BigDecimal;
import java.util.Date;


public class ImmunizationHandler {


	public static VaccinationReported vaccinationReportedFromFhir(Immunization i) {
		VaccinationReported vaccinationReported = new VaccinationReported();
		vaccinationReported.setVaccinationReportedId(i.getId());
		fillVaccinationReported(vaccinationReported,i);
		VaccinationMaster vaccinationMaster = getVaccinationMaster(null,i);
		vaccinationReported.setVaccination(vaccinationMaster);
		vaccinationMaster.setVaccinationReported(vaccinationReported);
		return vaccinationReported;
	}

//  public static void patientReportedFromFhirImmunization(PatientReported patientReported,
//      Immunization i) {
//    if (i != null) {
//      patientReported.setReportedDate(i.getRecorded());
//      patientReported.setUpdatedDate(i.getOccurrenceDateTimeType().getValue());
//      patientReported.setPatientReportedAuthority(i.getIdentifierFirstRep().getValue());
//    }
//  }
  /**
   * This method fills a vaccinationReported object with mapped fhir immunization information
   * @param vr the vaccinationReported
   * @param i the Immunization resource
   */
  public static void fillVaccinationReported(VaccinationReported vr, Immunization i) {
     vr.setVaccinationReportedId(MappingHelper.filterIdentifier(i.getIdentifier(),"PatientReported").getValue());

	  vr.setReportedDate(i.getRecorded());
	  vr.setAdministeredDate(i.getOccurrenceDateTimeType().getValue());

	  vr.setVaccineCvxCode(i.getVaccineCode().getCode("CVX"));
	  vr.setVaccineNdcCode(i.getVaccineCode().getCode("NDC"));
	  vr.setVaccineMvxCode(i.getVaccineCode().getCode("MVX"));

	 vr.setAdministeredAmount(i.getDoseQuantity().getValue().toString());
	 vr.setInformationSource(i.getInformationSourceCodeableConcept().getCode("informationSource"));
    vr.setUpdatedDate(new Date());

    vr.setLotnumber(i.getLotNumber());
    vr.setExpirationDate(i.getExpirationDate());
    switch(i.getStatus()){
        case COMPLETED : vr.setCompletionStatus("CP"); break;
        case ENTEREDINERROR : vr.setActionCode("D"); break;
        case NOTDONE : vr.setCompletionStatus("RE"); break; //Could also be NA or PA
      default:
        break;
    }

    vr.setRefusalReasonCode(i.getReasonFirstRep().getConcept().getCodingFirstRep().getCode());
	 vr.setBodySite(i.getSite().getCodingFirstRep().getCode());
	 vr.setBodyRoute(i.getRoute().getCodingFirstRep().getCode());
	 vr.setFundingSource(i.getFundingSource().getCodingFirstRep().getCode());
	 vr.setFundingEligibility(i.getProgramEligibilityFirstRep().getCodingFirstRep().getCode());

//	 vr.setOrgLocation(LocationMapper.orgLocationFromFhir(i.getLocation()));
//	  vr.setEnteredBy();
//	  vr.setAdministeringProvider();
//	  vr.getOrderingProvider()
  }


  public static VaccinationMaster getVaccinationMaster(VaccinationMaster vm, Immunization i){
	  if (vm == null){
		  vm = new VaccinationMaster();
	  }
	  vm.setVaccinationId(MappingHelper.filterIdentifier(i.getIdentifier(), "VaccinationMaster").getValue());
	  vm.setAdministeredDate(i.getOccurrenceDateTimeType().getValue());
	  vm.setVaccineCvxCode(i.getVaccineCode().getCode("CVX"));
//	  vm.setPatient();
//	  vm.setVaccinationReported();
	  return vm;
  }

  /**
   * This method create the immunization resource based on the vaccinationReported information
   * @param vaccinationReported the vaccinationReported
   * @return the Immunization resource
   */
  public static Immunization getImmunization(VaccinationMaster vaccinationMaster, VaccinationReported vaccinationReported) {
    Immunization i = new Immunization();
	 if (vaccinationMaster != null ){
		i.addIdentifier(MappingHelper.getFhirIdentifier("VaccinationMaster",vaccinationMaster.getVaccinationId()));
		i.setOccurrence(new DateTimeType(vaccinationMaster.getAdministeredDate()));
		i.getVaccineCode().addCoding().setSystem("CVX").setCode(vaccinationMaster.getVaccineCvxCode());
//		i.setPatient()
		 i.setPatient(MappingHelper.getFhirReference("Patient","PatientReported", vaccinationMaster.getPatient().getPatientId()));

	 }
	 if (vaccinationReported != null) {
		 fillImmunization(i, vaccinationReported);
	 }
    return i;
  }

	/**
	 * This method fills the immunization resource with the mapped information of a vaccinationReported object
	 * @param i
	 * @param vr
	 */
  public static void fillImmunization(Immunization i, VaccinationReported vr) {

//    i.setId(vr.getVaccinationReportedId());
	  i.addIdentifier(MappingHelper.getFhirIdentifier("vaccinationReported", vr.getVaccinationReportedId()));
	  i.setPatient(MappingHelper.getFhirReference("Patient","PatientReported", vr.getPatientReported().getPatientReportedId()));
	  i.setRecorded(vr.getReportedDate());
	  i.getOccurrenceDateTimeType().setValue(vr.getAdministeredDate());

	  i.getVaccineCode().addCoding().setCode(vr.getVaccineCvxCode()).setSystem("CVX");
	  i.getVaccineCode().addCoding().setCode(vr.getVaccineNdcCode()).setSystem("NDC");
	  i.getVaccineCode().addCoding().setCode(vr.getVaccineMvxCode()).setSystem("Mvx");

	  i.setDoseQuantity(new Quantity().setValue(new BigDecimal(vr.getAdministeredAmount())));
	  i.setInformationSource(new CodeableConcept(new Coding().setSystem("informationSource").setCode(vr.getInformationSource()))); // TODO change system name
	  i.setLotNumber(vr.getLotnumber());
	  i.setExpirationDate(vr.getExpirationDate());

	  if (vr.getActionCode().equals("D")) {
		  i.setStatus(Immunization.ImmunizationStatusCodes.ENTEREDINERROR);
	  } else {
		  switch(vr.getCompletionStatus()) {
			  case "CP" : {
				  i.setStatus(Immunization.ImmunizationStatusCodes.COMPLETED);
				  break;
			  }
			  case "NA" :
			  case "PA" :
			  case "RE" : {
				  i.setStatus(Immunization.ImmunizationStatusCodes.NOTDONE);
				  break;
			  }
			  case "" : {
				  i.setStatus(Immunization.ImmunizationStatusCodes.NULL);
				  break;
			  }
			  default: break;
		  }
	  }
	  i.addReason().setConcept(new CodeableConcept(new Coding("refusalReasonCode",vr.getRefusalReasonCode(),vr.getRefusalReasonCode())));
	  i.getSite().addCoding().setSystem("bodyPart").setCode(vr.getBodySite());
	  i.getRoute().addCoding().setSystem("bodyRoute").setCode(vr.getBodyRoute());
	  i.getFundingSource().addCoding().setSystem("fundingSource").setCode(vr.getFundingSource());
	  i.addProgramEligibility().addCoding().setSystem("fundingEligibility").setCode(vr.getFundingEligibility());


	  Location location  = LocationMapper.fhirLocation(vr.getOrgLocation()); // TODO save it ?
	  i.setLocation(new Reference(location));

	  if (vr.getEnteredBy() != null) {
		  i.addPerformer()
			  .setFunction(new CodeableConcept().addCoding(new Coding().setSystem("iis-sandbox-function").setCode("entering")))
			  .setActor(MappingHelper.getFhirReference("Person","Person", vr.getEnteredBy().getPersonId()));
	  }
	  if (vr.getOrderingProvider() != null) {
		  i.addPerformer()
			  .setFunction(new CodeableConcept().addCoding(new Coding().setSystem("iis-sandbox-function").setCode("ordering")))
			  .setActor(MappingHelper.getFhirReference("Person", "Person", vr.getOrderingProvider().getPersonId()));
	  }
	  if (vr.getAdministeringProvider() != null) {
		  i.addPerformer()
			  .setFunction(new CodeableConcept().addCoding(new Coding().setSystem("iis-sandbox-function").setCode("administering")))
			  .setActor(MappingHelper.getFhirReference("Person", "Person", vr.getAdministeringProvider().getPersonId()));
	  }

  }


}