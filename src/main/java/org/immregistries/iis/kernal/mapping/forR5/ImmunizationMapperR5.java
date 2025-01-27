package org.immregistries.iis.kernal.mapping.forR5;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.TokenParam;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.mapping.internalClient.FhirRequesterR5;
import org.immregistries.iis.kernal.fhir.annotations.OnR5Condition;
import org.immregistries.iis.kernal.mapping.interfaces.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.model.ModelPerson;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.model.VaccinationReported;
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


	public VaccinationReported localObjectReportedWithMaster(Immunization i) {
		VaccinationReported vaccinationReported = this.localObjectReported(i);
		VaccinationMaster vaccinationMaster = fhirRequests.searchVaccinationMaster(
			new SearchParameterMap(Immunization.SP_IDENTIFIER, new TokenParam().setValue(vaccinationReported.getExternalLink())));
		if (vaccinationMaster!= null) {
			vaccinationReported.setVaccination(vaccinationMaster);
		}
		return vaccinationReported;
	}

	/**
	 * Returns Vaccination Reported for Casting reasons
	 *
	 * @param vr Vaccination object to be filled
	 * @param i
	 * @return
	 */
	public void fillFromFhirResource(VaccinationMaster vr, Immunization i) {
		vr.setVaccinationId(new IdType(i.getId()).getIdPart());
		vr.setUpdatedDate(i.getMeta().getLastUpdated());
		vr.setExternalLink(i.getIdentifierFirstRep().getValue());
		vr.setExternalLinkSystem(i.getIdentifierFirstRep().getSystem());
		if (i.getPatient() != null && StringUtils.isNotBlank(i.getPatient().getReference())) {
			vr.setPatientReported(fhirRequests.readPatientReported(i.getPatient().getReference()));
//			vr.setPatientReported(fhirRequests.readPatientReported(i.getPatient().getReference().split("Patient/")[0]));
		}

		if (vr.getReportedDate() != null) {
			Extension recorded = i.addExtension()
				.setUrl(RECORDED)
				.setValue(new DateType(vr.getReportedDate()));
		}
		Extension recorded = i.getExtensionByUrl(RECORDED);
		if (recorded != null) {
			vr.setReportedDate(MappingHelper.extensionGetDate(recorded));
		}
//		vr.setReportedDate(i.getMeta().getLastUpdated()); // TODO change
		vr.setAdministeredDate(i.getOccurrenceDateTimeType().getValue());

		vr.setVaccineCvxCode(i.getVaccineCode().getCode(CVX));
		vr.setVaccineNdcCode(i.getVaccineCode().getCode(NDC));
		vr.setVaccineMvxCode(i.getVaccineCode().getCode(MVX));

		vr.setVaccineMvxCode(i.getManufacturer().getReference().getIdentifier().getValue());

		if (i.getDoseQuantity().hasValue()) {
			vr.setAdministeredAmount(i.getDoseQuantity().getValue().toString());
		}

		vr.setInformationSource(i.getInformationSource().getConcept().getCode(INFORMATION_SOURCE));
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
		vr.setFundingEligibility(i.getProgramEligibilityFirstRep().getProgram().getCodingFirstRep().getCode());

		if (i.getLocation() != null && StringUtils.isNotBlank(i.getLocation().getReference())) {
			vr.setOrgLocation(fhirRequests.readOrgLocation(i.getLocation().getReference()));
		}
		if (i.hasInformationSource() && i.getInformationSource().getReference() != null && StringUtils.isNotBlank(i.getInformationSource().getReference().getReference())) {
			vr.setEnteredBy(fhirRequests.readPractitionerPerson(i.getInformationSource().getReference().getReference()));
		} else {
//			vr.set
		}
		for (Immunization.ImmunizationPerformerComponent performer : i.getPerformer()) {
			if (performer.getActor() != null && StringUtils.isNotBlank(performer.getActor().getReference())) {
				switch (performer.getFunction().getCode(FUNCTION)) {
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
	}

//	/**
//	 * Converts golden resource to VaccinationMaster
//	 * @param i
//	 * @return
//	 */
//  public VaccinationMaster getMaster(Immunization i){
//	  VaccinationMaster vaccinationMaster = new VaccinationMaster();
//	  vaccinationMaster.setVaccinationId(i.getId());
//	  vaccinationMaster.setExternalLink(i.getIdentifierFirstRep().getValue());
//	  vaccinationMaster.setAdministeredDate(i.getOccurrenceDateTimeType().getValue());
//	  vaccinationMaster.setVaccineCvxCode(i.getVaccineCode().getCode(CVX));
//	  if (i.getPatient() != null && StringUtils.isNotBlank(i.getPatient().getId())) {
//		  vaccinationMaster.setPatient(fhirRequests.readPatientMaster(i.getPatient().getId()));
//	  }
////	  vaccinationMaster.setVaccinationReported();
//	  return vaccinationMaster;
//  }
public VaccinationReported localObjectReported(Immunization i) {
		VaccinationReported vaccinationReported = new VaccinationReported();
		fillFromFhirResource(vaccinationReported, i); // TODO assert not golden record ?
		return vaccinationReported;
	}

	public VaccinationMaster localObject(Immunization i) {
		VaccinationMaster vaccinationMaster = new VaccinationMaster();
		fillFromFhirResource(vaccinationMaster, i); // TODO assert golden record ?
		return vaccinationMaster;
	}

  /**
   * This method create the immunization resource based on the vaccinationReported information
   * @param vr the vaccinationReported
   * @return the Immunization resource
   */
  public Immunization fhirResource(VaccinationMaster vr) {
	  Immunization i = new Immunization();
	  i.addIdentifier(MappingHelper.getFhirIdentifierR5(vr.getExternalLinkSystem(), vr.getExternalLink())); // TODO if system empty ?
	  Reference patientReference = new Reference()
		  .setReference("Patient/" + vr.getPatientReported().getPatientId())
//		  .setIdentifier(new Identifier()
//			  .setValue(vr.getPatientReported().getPatientReportedExternalLink())
//			  .setSystem(vr.getPatientReported().getPatientReportedAuthority()))
		  ;
	  i.setPatient(patientReference);
	  if (vr.getReportedDate() != null) {
		  Extension recorded = i.addExtension()
			  .setUrl(RECORDED)
			  .setValue(new DateType(vr.getReportedDate()));
	  }
	  i.getOccurrenceDateTimeType().setValue(vr.getAdministeredDate());

	  if (StringUtils.isNotBlank(vr.getVaccineCvxCode())) {
		  i.getVaccineCode().addCoding().setCode(vr.getVaccineCvxCode()).setSystem(CVX);
	  }
	  if (StringUtils.isNotBlank(vr.getVaccineNdcCode())) {
		  i.getVaccineCode().addCoding().setCode(vr.getVaccineNdcCode()).setSystem(NDC);
	  }
	  i.setManufacturer(MappingHelper.getFhirCodeableReferenceR5(MappingHelper.ORGANIZATION, MVX, vr.getVaccineMvxCode()));

	  if (StringUtils.isNotBlank(vr.getAdministeredAmount())) {
		  i.setDoseQuantity(new Quantity().setValue(new BigDecimal(vr.getAdministeredAmount())));
	  }

	  i.setInformationSource(new CodeableReference(new CodeableConcept(new Coding().setSystem(INFORMATION_SOURCE).setCode(vr.getInformationSource())))); // TODO change system name
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
	  i.addProgramEligibility().setProgram(new CodeableConcept(new Coding().setSystem(FUNDING_ELIGIBILITY).setCode(vr.getFundingEligibility())));


	  Location location = locationMapper.fhirResource(vr.getOrgLocation()); // Should have been saved in Event/MessageHandler
	  if (location != null) {
		  i.setLocation(new Reference(MappingHelper.LOCATION + "/" + location.getId()));
	  }

	  if (vr.getEnteredBy() != null) {
		  i.setInformationSource(new CodeableReference(new Reference(MappingHelper.PRACTITIONER + "/" + vr.getEnteredBy().getPersonId())));
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
			  actor = new Reference(MappingHelper.PERSON + "/" + person.getPersonId());
			  ;
			  break;
		  }
		  default:{
			  actor = MappingHelper.getFhirReferenceR5(MappingHelper.PRACTITIONER, person.getIdentifierTypeCode(), person.getPersonExternalLink(), person.getPersonId());
		  }
	  }
	  performer.setActor(actor);
	  return performer;
  }


}