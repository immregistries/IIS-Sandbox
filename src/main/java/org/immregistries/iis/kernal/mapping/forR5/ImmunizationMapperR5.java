package org.immregistries.iis.kernal.mapping.forR5;

import ca.uhn.fhir.jpa.starter.annotations.OnR5Condition;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.mapping.Interfaces.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.model.ModelPerson;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.model.VaccinationReported;
import org.immregistries.iis.kernal.repository.FhirRequesterR5;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

@Service
@Conditional(OnR5Condition.class)
public class ImmunizationMapperR5 implements ImmunizationMapper<Immunization> {
	@Autowired
    LocationMapperR5 locationMapper;
	@Autowired
	FhirRequesterR5 fhirRequests;

	public static final String CVX = "http://hl7.org/fhir/sid/cvx";
	public static final String MVX = "http://terminology.hl7.org/CodeSystem/MVX";
	public static final String NDC = "NDC";
	public static final String INFORMATION_SOURCE = "NIP001"; //TODO get system from actual message
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

	public VaccinationReported getReportedWithMaster(Immunization i) {
		VaccinationReported vaccinationReported = getReported(i);
		VaccinationMaster vaccinationMaster = fhirRequests.searchVaccinationMaster(
			Immunization.IDENTIFIER.exactly().identifier(
				vaccinationReported.getVaccinationReportedExternalLink()));
		if (vaccinationMaster!= null) {
			vaccinationReported.setVaccination(vaccinationMaster);
			vaccinationMaster.setVaccinationReported(vaccinationReported);
		}
		return vaccinationReported;
	}

	public VaccinationReported getReported(Immunization i) {
		VaccinationReported vr = new VaccinationReported();
		vr.setVaccinationReportedId(new IdType(i.getId()).getIdPart());
		vr.setUpdatedDate(i.getMeta().getLastUpdated());
		vr.setVaccinationReportedExternalLink(i.getIdentifierFirstRep().getValue());
		vr.setVaccinationReportedExternalLinkSystem(i.getIdentifierFirstRep().getSystem());
		if (i.getPatient() != null && i.getPatient().getReference() != null && !i.getPatient().getReference().isBlank()) {
			vr.setPatientReported(fhirRequests.readPatientReported(i.getPatient().getReference()));
//			vr.setPatientReported(fhirRequests.readPatientReported(i.getPatient().getReference().split("Patient/")[0]));
		}

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
				case COMPLETED: {
					vr.setCompletionStatus("CP");
					break;
				}
				case ENTEREDINERROR: {
					vr.setActionCode("D");
					break;
				}
				case NOTDONE: {
					vr.setCompletionStatus("RE");
					break;
				} //Could also be NA or PA
				case NULL:
				default:
					vr.setCompletionStatus("");
					break;
			}
		}
		vr.setRefusalReasonCode(i.getStatusReason().getCodingFirstRep().getCode());
		vr.setBodySite(i.getSite().getCodingFirstRep().getCode());
		vr.setBodyRoute(i.getRoute().getCodingFirstRep().getCode());
		vr.setFundingSource(i.getFundingSource().getCodingFirstRep().getCode());
		vr.setFundingEligibility(i.getProgramEligibilityFirstRep().getCodingFirstRep().getCode());

		if (i.getLocation() != null && i.getLocation().getReference() != null && !i.getLocation().getReference().isBlank()){
			vr.setOrgLocation(fhirRequests.readOrgLocation(i.getLocation().getReference()));
		}
		if (i.hasInformationSourceReference() && i.getInformationSourceReference().getReference() != null && !i.getInformationSourceReference().getReference().isBlank()) {
			vr.setEnteredBy(fhirRequests.readPractitionerPerson(i.getInformationSourceReference().getReference()));
		} else {
//			vr.set
		}
		for (Immunization.ImmunizationPerformerComponent performer: i.getPerformer()) {
			if (performer.getActor() != null && performer.getActor().getReference() != null && !performer.getActor().getReference().isBlank()){
				switch (performer.getFunction().getCode(FUNCTION)){
					case ADMINISTERING: {
						vr.setAdministeringProvider(fhirRequests.readPractitionerPerson(performer.getActor().getReference()));
						break;
					}
					case ORDERING: {
						vr.setOrderingProvider(fhirRequests.readPractitionerPerson(performer.getActor().getReference()));
						break;
					}
				}
			}
		}
		return vr;
	}

	/**
	 * Converts golden resource to VaccinationMaster
	 * @param i
	 * @return
	 */
  public VaccinationMaster getMaster(Immunization i){
	  VaccinationMaster vaccinationMaster = new VaccinationMaster();
	  vaccinationMaster.setVaccinationId(i.getId());
	  vaccinationMaster.setExternalLink(i.getIdentifierFirstRep().getValue());
	  vaccinationMaster.setAdministeredDate(i.getOccurrenceDateTimeType().getValue());
	  vaccinationMaster.setVaccineCvxCode(i.getVaccineCode().getCode(CVX));
	  if (i.getPatient() != null && !i.getPatient().getId().isBlank()) {
		  vaccinationMaster.setPatient(fhirRequests.readPatientMaster(i.getPatient().getId()));
	  }
//	  vaccinationMaster.setVaccinationReported();
	  return vaccinationMaster;
  }

  /**
   * This method create the immunization resource based on the vaccinationReported information
   * @param vr the vaccinationReported
   * @return the Immunization resource
   */
  public Immunization getFhirResource(VaccinationReported vr) {
     Immunization i = new Immunization();
	  i.addIdentifier(MappingHelper.getFhirIdentifier(vr.getVaccinationReportedExternalLinkSystem(), vr.getVaccinationReportedExternalLink())); // TODO if system empty ?
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
	  i.setStatusReason(new CodeableConcept(new Coding(REFUSAL_REASON_CODE,vr.getRefusalReasonCode(),vr.getRefusalReasonCode())));
//	  i.addReason().setConcept());
	  i.getSite().addCoding().setSystem(BODY_PART).setCode(vr.getBodySite());
	  i.getRoute().addCoding().setSystem(BODY_ROUTE).setCode(vr.getBodyRoute());
	  i.getFundingSource().addCoding().setSystem(FUNDING_SOURCE).setCode(vr.getFundingSource());
	  i.addProgramEligibility().addCoding().setSystem(FUNDING_ELIGIBILITY).setCode(vr.getFundingEligibility());


	  Location location  = locationMapper.getFhirResource(vr.getOrgLocation()); // Should have been saved in Event/MessageHandler
	  if (location != null) {
		  i.setLocation(new Reference(MappingHelper.LOCATION + "/" + location.getId()));
	  }

	  if (vr.getEnteredBy() != null) {
		  i.setInformationSource(new Reference(MappingHelper.PRACTITIONER+"/" + vr.getEnteredBy().getPersonId()));
	  }
	  if (vr.getOrderingProvider() != null) {
		  i.addPerformer(performer(vr.getOrderingProvider(),ORDERING, ORDERING_DISPLAY));
	  }
	  if (vr.getAdministeringProvider() != null) {
		  i.addPerformer(performer(vr.getAdministeringProvider(),ADMINISTERING, ADMINISTERING_DISPLAY));
	  }
     return i;
  }

  private Immunization.ImmunizationPerformerComponent performer(ModelPerson person, String functionCode, String functionDisplay ) {
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