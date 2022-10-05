package org.immregistries.iis.kernal.mapping;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.model.ModelPerson;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.model.VaccinationReported;
import org.immregistries.iis.kernal.repository.FhirRequests;

import java.math.BigDecimal;
import java.util.Date;


public class ImmunizationMapper {
	public static final String CVX = "http://hl7.org/fhir/sid/cvx";
	public static final String MVX = "http://terminology.hl7.org/CodeSystem/MVX";
	public static final String NDC = "NDC";
	public static final String INFORMATION_SOURCE = "informationSource";
	public static final String FUNCTION = "http://hl7.org/fhir/ValueSet/immunization-function";
	public static final String ORDERING = "OP";
	public static final String ORDERING_DISPLAY = "Ordering Provider";
	public static final String ENTERING = "entering";
	public static final String ENTERING_DISPLAY = "Entering Provider";
	public static final String ADMINISTERING = "AP";
	public static final String ADMINISTERING_DISPLAY = "Administering Provider";
	public static final String REFUSAL_REASON_CODE = "refusalReasonCode";
	public static final String BODY_PART = "bodyPart";
	public static final String BODY_ROUTE = "bodyRoute";
	public static final String FUNDING_SOURCE = "fundingSource";
	public static final String FUNDING_ELIGIBILITY = "fundingEligibility";

	public static VaccinationReported getReportedWithMaster(Immunization i, FhirRequests fhirRequests, IGenericClient fhirClient) {
		VaccinationReported vaccinationReported = getReported(i);
		VaccinationMaster vaccinationMaster = fhirRequests.searchVaccinationMaster(fhirClient,
			Immunization.IDENTIFIER.exactly().systemAndIdentifier(
				MappingHelper.VACCINATION_REPORTED,
				vaccinationReported.getVaccinationReportedExternalLink()));
		if (vaccinationMaster!= null) {
			vaccinationReported.setVaccination(vaccinationMaster);
			vaccinationMaster.setVaccinationReported(vaccinationReported);
		}
		return vaccinationReported;
	}
	public static VaccinationReported getReported(Immunization i) {
		VaccinationReported vr = new VaccinationReported();
		vr.setVaccinationReportedId(new  IdType(i.getId()).getIdPart());
		vr.setUpdatedDate(i.getMeta().getLastUpdated());
		vr.setVaccinationReportedExternalLink(MappingHelper.filterIdentifier(i.getIdentifier(),MappingHelper.VACCINATION_REPORTED).getValue());
		vr.setPatientReportedId(i.getPatient().getReference());

		vr.setReportedDate(i.getRecorded());
		vr.setAdministeredDate(i.getOccurrenceDateTimeType().getValue());

		vr.setVaccineCvxCode(i.getVaccineCode().getCode(CVX));
		vr.setVaccineNdcCode(i.getVaccineCode().getCode(NDC));
		vr.setVaccineMvxCode(i.getVaccineCode().getCode(MVX));

		vr.setVaccineMvxCode(i.getManufacturer().getIdentifier().getValue());


		vr.setAdministeredAmount(i.getDoseQuantity().getValue().toString());

		vr.setInformationSource(i.getInformationSourceCodeableConcept().getCode(INFORMATION_SOURCE));
		vr.setUpdatedDate(new Date());

		vr.setLotnumber(i.getLotNumber());
		vr.setExpirationDate(i.getExpirationDate());
		if (i.getStatus() != null) {
			switch(i.getStatus()){
				case COMPLETED : {
					vr.setCompletionStatus("CP");
					break;
				}
				case ENTEREDINERROR : {
					vr.setActionCode("D");
					break;
				}
				case NOTDONE : {
					vr.setCompletionStatus("RE");
					break;
				} //Could also be NA or PA
				case NULL:
				default:
					vr.setCompletionStatus("");
					break;
			}
		}
		vr.setRefusalReasonCode(i.getReasonFirstRep().getConcept().getCodingFirstRep().getCode());
		vr.setBodySite(i.getSite().getCodingFirstRep().getCode());
		vr.setBodyRoute(i.getRoute().getCodingFirstRep().getCode());
		vr.setFundingSource(i.getFundingSource().getCodingFirstRep().getCode());
		vr.setFundingEligibility(i.getProgramEligibilityFirstRep().getCodingFirstRep().getCode());

	  vr.setOrgLocationId(i.getLocation().getId()); // Dealt with in servlet
		if (i.getInformationSource().isResource()) {
			vr.setEnteredById(i.getInformationSourceReference().getId());
		}
		for (Immunization.ImmunizationPerformerComponent performer: i.getPerformer()) {
			switch (performer.getFunction().getCode(FUNCTION)){
				case ADMINISTERING: {
					vr.setAdministeringProviderId(performer.getActor().getId());
					break;
				}
				case ORDERING: {
					vr.setOrderingProviderId(performer.getActor().getId());
					break;
				}
			}
		}
		return vr;
	}

  public static VaccinationMaster getMaster( Immunization i){
	  VaccinationMaster vaccinationMaster = new VaccinationMaster();
	  vaccinationMaster.setVaccinationId(i.getId());
	  vaccinationMaster.setExternalLink(MappingHelper.filterIdentifier(i.getIdentifier(), MappingHelper.VACCINATION_MASTER).getValue());
//	  vaccinationMaster.set(MappingHelper.filterIdentifier(i.getIdentifier(), MappingHelper.VACCINATION_MASTER).getValue());
	  vaccinationMaster.setAdministeredDate(i.getOccurrenceDateTimeType().getValue());
	  vaccinationMaster.setVaccineCvxCode(i.getVaccineCode().getCode(CVX));
//	  vaccinationMaster.setPatient();
//	  vaccinationMaster.setVaccinationReported();
	  return vaccinationMaster;
  }

  /**
   * This method create the immunization resource based on the vaccinationReported information
   * @param vr the vaccinationReported
   * @return the Immunization resource
   */
  public static Immunization getFhirResource(VaccinationReported vr) {
    Immunization i = new Immunization();
//	 if (vaccinationMaster != null ){
//		i.addIdentifier(MappingHelper.getFhirIdentifier(MappingHelper.VACCINATION_MASTER,vaccinationMaster.getVaccinationReported().getVaccinationReportedExternalLink()));
//		i.setOccurrence(new DateTimeType(vaccinationMaster.getAdministeredDate()));
//		i.getVaccineCode().addCoding().setSystem(CVX).setCode(vaccinationMaster.getVaccineCvxCode());
////		i.setPatient(MappingHelper.getFhirReference(MappingHelper.PATIENT,MappingHelper.PATIENT_MASTER, vaccinationMaster.getPatient().getPatientExternalLink()));
//	 }
	 if (vr != null) {
		 i.addIdentifier(MappingHelper.getFhirIdentifier(MappingHelper.VACCINATION_REPORTED, vr.getVaccinationReportedExternalLink()));

//		 i.setPatient(MappingHelper.getFhirReference(MappingHelper.PATIENT,MappingHelper.MRN_SYSTEM, vr.getPatientReported().getPatientReportedExternalLink(), vr.getPatientReported().getPatientReportedId()));
		 i.setPatient(new Reference().setReference("Patient/"+ vr.getPatientReported().getPatientReportedId()));
		 i.setRecorded(vr.getReportedDate());
		 i.getOccurrenceDateTimeType().setValue(vr.getAdministeredDate());

		 if(!vr.getVaccineCvxCode().isBlank()){
			 i.getVaccineCode().addCoding().setCode(vr.getVaccineCvxCode()).setSystem(CVX);
		 }
		 if(!vr.getVaccineNdcCode().isBlank()){
			 i.getVaccineCode().addCoding().setCode(vr.getVaccineNdcCode()).setSystem(NDC);
		 }
		 i.setManufacturer(MappingHelper.getFhirReference(MappingHelper.ORGANISATION,MVX,vr.getVaccineMvxCode()));

		 i.setDoseQuantity(new Quantity().setValue(new BigDecimal(vr.getAdministeredAmount())));
		 i.setInformationSource(new CodeableConcept(new Coding().setSystem(INFORMATION_SOURCE).setCode(vr.getInformationSource()))); // TODO change system name
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
				 case "" :
				 default: {
//					 i.setStatus(Immunization.ImmunizationStatusCodes..NULL);
					 break;
				 }
			 }
		 }
		 i.addReason().setConcept(new CodeableConcept(new Coding(REFUSAL_REASON_CODE,vr.getRefusalReasonCode(),vr.getRefusalReasonCode())));
		 i.getSite().addCoding().setSystem(BODY_PART).setCode(vr.getBodySite());
		 i.getRoute().addCoding().setSystem(BODY_ROUTE).setCode(vr.getBodyRoute());
		 i.getFundingSource().addCoding().setSystem(FUNDING_SOURCE).setCode(vr.getFundingSource());
		 i.addProgramEligibility().addCoding().setSystem(FUNDING_ELIGIBILITY).setCode(vr.getFundingEligibility());


		 Location location  = LocationMapper.fhirLocation(vr.getOrgLocation()); // Should have been saved in Event/MessageHandler
		 i.setLocation(new Reference(location));

		 if (vr.getEnteredBy() != null) {
			 i.setInformationSource(new Reference(MappingHelper.PRACTITIONER+"/"+vr.getEnteredBy().getPersonId()));
		 }
		 if (vr.getOrderingProvider() != null) {
			 i.addPerformer(performer(vr.getOrderingProvider(),ORDERING, ORDERING_DISPLAY));
		 }
		 if (vr.getAdministeringProvider() != null) {
			 i.addPerformer(performer(vr.getAdministeringProvider(),ADMINISTERING, ADMINISTERING_DISPLAY));
		 }
	 }
    return i;
  }

  private static Immunization.ImmunizationPerformerComponent performer(ModelPerson person, String functionCode, String functionDisplay ) {
	  Immunization.ImmunizationPerformerComponent performer = new Immunization.ImmunizationPerformerComponent();
	  performer.setFunction(new CodeableConcept().addCoding(new Coding().setSystem(FUNCTION).setCode(functionCode).setDisplay(functionDisplay)));
	  Reference actor;
	  switch (person.getIdentifierTypeCode()) {
		  case MappingHelper.PRACTITIONER: {
			  actor = new Reference(MappingHelper.PRACTITIONER+"/"+person.getPersonId());
			  break;
		  }
		  case MappingHelper.PERSON: {
			  actor = MappingHelper.getFhirReference(
				  MappingHelper.PERSON,
				  MappingHelper.PERSON_MODEL,
				  person.getPersonId(),
				  person.getPersonId());
			  break;
		  }
		  default:{
			  actor = MappingHelper.getFhirReference(MappingHelper.PRACTITIONER, person.getIdentifierTypeCode(), person.getPersonExternalLink(), person.getPersonId());
		  }
	  }
	  performer.setActor(actor);
	  return performer;
  }


}