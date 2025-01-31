package org.immregistries.iis.kernal.mapping.forR5;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.TokenParam;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.*;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.immregistries.iis.kernal.logic.CodeMapManager;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.mapping.interfaces.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.internalClient.FhirRequesterR5;
import org.immregistries.iis.kernal.model.BusinessIdentifier;
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
	private FhirRequesterR5 fhirRequests;


	public VaccinationReported localObjectReportedWithMaster(Immunization i) {
		VaccinationReported vaccinationReported = this.localObjectReported(i);
		VaccinationMaster vaccinationMaster = fhirRequests.searchVaccinationMaster(
			new SearchParameterMap(Immunization.SP_IDENTIFIER, new TokenParam().setValue(vaccinationReported.getFillerBusinessIdentifier().getValue())));
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
		vr.setVaccinationId(StringUtils.defaultString(new IdType(i.getId()).getIdPart()));
		vr.setUpdatedDate(i.getMeta().getLastUpdated());
		for (Identifier identifier : i.getIdentifier()) {
			vr.addBusinessIdentifier(BusinessIdentifier.fromR5(identifier));
		}
		if (i.getPatient() != null && StringUtils.isNotBlank(i.getPatient().getReference())) {
			vr.setPatientReported(fhirRequests.readPatientReported(i.getPatient().getReference()));
//			vr.setPatientReported(fhirRequests.readPatientReported(i.getPatient().getReference().split("Patient/")[0]));
		}

		Extension recorded = i.getExtensionByUrl(RECORDED);
		if (recorded != null) {
			vr.setReportedDate(MappingHelper.extensionGetDate(recorded));
		} else {
			vr.setReportedDate(null);
		}
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
		Extension actionCode = i.getExtensionByUrl(ACTION_CODE_EXTENSION);
		if (actionCode != null) {
			if (actionCode.hasValue()) {
				vr.setActionCode(MappingHelper.extensionGetCoding(actionCode).getCode());
			}
		} else {
			vr.setActionCode(null);
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
		} // TODO where to put and read information Source
		for (Immunization.ImmunizationPerformerComponent performer : i.getPerformer()) {
			if (performer.getActor() != null && StringUtils.isNotBlank(performer.getActor().getReference())) {
				switch (performer.getFunction().getCode(PERFORMER_FUNCTION_SYSTEM)) {
					case ADMINISTERING: {
						vr.setAdministeringProvider(fhirRequests.readPractitionerPerson(performer.getActor().getReference()));
						break;
					}
					case ORDERING: {
						vr.setOrderingProvider(fhirRequests.readPractitionerPerson(performer.getActor().getReference()));
						break;
					}
					case ENTERING: {
						vr.setEnteredBy(fhirRequests.readPractitionerPerson(performer.getActor().getReference()));
						break;
					}
				}
			}
		}
	}

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
	  /*
		* Id
		*/
	  i.setId(StringUtils.defaultString(vr.getVaccinationId()));
	  /*
		* Identifiers
		*/
	  for (BusinessIdentifier businessIdentifier : vr.getBusinessIdentifiers()) {
		  i.addIdentifier(businessIdentifier.toR5());
	  }
	  /*
		* Patient
		*/
	  i.setPatient(new Reference().setReference("Patient/" + vr.getPatientReported().getPatientId()));
	  /*
		* Recorded Date
		*/
	  if (vr.getReportedDate() != null) {
		  i.addExtension()
			  .setUrl(RECORDED)
			  .setValue(new DateType(vr.getReportedDate()));
	  }
	  /*
		* Occurrence
		*/
	  i.getOccurrenceDateTimeType().setValue(vr.getAdministeredDate());
	  /*
		* CVX
		*/
	  if (StringUtils.isNotBlank(vr.getVaccineCvxCode())) {
		  i.getVaccineCode().addCoding().setCode(vr.getVaccineCvxCode()).setSystem(CVX);
	  }
	  /*
		* NDC
		*/
	  if (StringUtils.isNotBlank(vr.getVaccineNdcCode())) {
		  i.getVaccineCode().addCoding().setCode(vr.getVaccineNdcCode()).setSystem(NDC);
	  }
	  /*
		* Manufacturer MVX
		*/
	  if (StringUtils.isNotBlank(vr.getVaccineMvxCode())) {
		  i.setManufacturer(MappingHelper.getFhirCodeableReferenceR5(MappingHelper.ORGANIZATION, MVX, vr.getVaccineMvxCode()));
	  }
	  /*
		* Administered Amount
		*/
	  if (StringUtils.isNotBlank(vr.getAdministeredAmount())) {
		  i.setDoseQuantity(new Quantity().setValue(new BigDecimal(vr.getAdministeredAmount())));
	  }

	  /*
		* Lot Number
		*/
	  i.setLotNumber(vr.getLotnumber());
	  /*
		* Expiration Date
		*/
	  i.setExpirationDate(vr.getExpirationDate());
	  /*
		* Action code Status
		*/
	  if (vr.getActionCode() != null) {
		  i.addExtension().setUrl(ACTION_CODE_EXTENSION).setValue(new Coding().setCode(vr.getActionCode()).setSystem(ACTION_CODE_SYSTEM));
		  if (vr.getActionCode().equals("D")) {
			  i.setStatus(Immunization.ImmunizationStatusCodes.ENTEREDINERROR);
		  } else {
			  switch (vr.getCompletionStatus()) {
				  case "CP": {
					  i.setStatus(Immunization.ImmunizationStatusCodes.COMPLETED);
					  break;
				  }
				  case "NA":
				  case "PA":
				  case "RE": {
					  i.setStatus(Immunization.ImmunizationStatusCodes.NOTDONE);
					  break;
				  }
				  case "":
				  default: {
//					 i.setStatus(Immunization.ImmunizationStatusCodes..NULL);
					  break;
				  }
			  }
		  }
	  }
	  /*
		* Status Reason
		*/
	  if (vr.getRefusalReasonCode() != null) {
		  Coding coding = new Coding().setSystem(REFUSAL_REASON_CODE).setCode(vr.getRefusalReasonCode());
		  Code code = CodeMapManager.getCodeMap().getCodeForCodeset(CodesetType.VACCINATION_REFUSAL, vr.getRefusalReasonCode());
		  if (code != null) {
			  coding.setDisplay(code.getLabel());
		  }
		  CodeableConcept codeableConcept = new CodeableConcept(coding);
		  i.setStatusReason(codeableConcept);
	  }
	  /*
		* Body Part
		*/
	  if (vr.getBodySite() != null) {
		  Coding coding = new Coding().setSystem(BODY_PART).setCode(vr.getBodySite());
		  Code code = CodeMapManager.getCodeMap().getCodeForCodeset(CodesetType.BODY_SITE, vr.getBodySite());
		  if (code != null) {
			  coding.setDisplay(code.getLabel());
		  }
		  i.getSite().addCoding(coding);
	  }
	  /*
		* Body Route
		*/
	  if (vr.getBodyRoute() != null) {
		  Coding coding = new Coding().setSystem(BODY_ROUTE).setCode(vr.getBodyRoute());
		  Code code = CodeMapManager.getCodeMap().getCodeForCodeset(CodesetType.BODY_ROUTE, vr.getBodyRoute());
		  if (code != null) {
			  coding.setDisplay(code.getLabel());
		  }
		  i.getRoute().addCoding(coding);
	  }
	  /*
		* Funding Source
		*/
	  if (vr.getFundingSource() != null) {
		  Coding coding = new Coding().setSystem(FUNDING_SOURCE).setCode(vr.getFundingSource());
		  Code code = CodeMapManager.getCodeMap().getCodeForCodeset(CodesetType.VACCINATION_FUNDING_SOURCE, vr.getFundingSource());
		  if (code != null) {
			  coding.setDisplay(code.getLabel());
		  }
		  i.getFundingSource().addCoding(coding);
	  }
	  /*
		* Program Funding Eligibility
		*/
	  if (vr.getFundingEligibility() != null) {
		  Coding coding = new Coding().setSystem(FUNDING_ELIGIBILITY).setCode(vr.getFundingEligibility());
		  Code code = CodeMapManager.getCodeMap().getCodeForCodeset(CodesetType.FINANCIAL_STATUS_CODE, vr.getFundingEligibility());
		  if (code != null) {
			  coding.setDisplay(code.getLabel());
		  }
		  i.addProgramEligibility().setProgram(new CodeableConcept(coding));
	  }
	  /*
		* Location
		*/
	  Location location = locationMapper.fhirResource(vr.getOrgLocation()); // Should have been saved in Event/MessageHandler
	  if (location != null) {
		  i.setLocation(new Reference(MappingHelper.LOCATION + "/" + location.getId()));
	  }
	  /*
		* Information Source / Report Origin code
		*/
	  if (vr.getInformationSource() != null) {
		  CodeableReference informationSource = i.getInformationSource();
		  if (informationSource == null) {
			  informationSource = new CodeableReference();
			  i.setInformationSource(informationSource);
		  }
		  Coding coding = new Coding().setSystem(INFORMATION_SOURCE).setCode(vr.getInformationSource()); // TODO change system name
		  Code code = CodeMapManager.getCodeMap().getCodeForCodeset(CodesetType.VACCINATION_INFORMATION_SOURCE, vr.getInformationSource());
		  if (code != null) {
			  coding.setDisplay(code.getLabel());
		  }
		  informationSource.setConcept(new CodeableConcept(coding));
	  }
	  /*
		* Information Source
		* Entering Performer
		*/
	  if (vr.getEnteredBy() != null) {
//		  CodeableReference informationSource = i.getInformationSource();
//		  if (informationSource == null) {
//			  informationSource = new CodeableReference();
//			  i.setInformationSource(informationSource);
//		  }
//		  informationSource.setReference(new Reference(MappingHelper.PRACTITIONER + "/" + vr.getEnteredBy().getPersonId()));
		  i.addPerformer(performer(vr.getEnteredBy(), ENTERING, ENTERING_DISPLAY));
	  }
	  /*
		* Ordering Performer
		*/
	  if (vr.getOrderingProvider() != null) {
		  i.addPerformer(performer(vr.getOrderingProvider(),ORDERING, ORDERING_DISPLAY));
	  }
	  /*
		* Administering Performer
		*/
	  if (vr.getAdministeringProvider() != null) {
		  i.addPerformer(performer(vr.getAdministeringProvider(),ADMINISTERING, ADMINISTERING_DISPLAY));
	  }
     return i;
  }

  private Immunization.ImmunizationPerformerComponent performer(ModelPerson person, String functionCode, String functionDisplay ) {
	  Immunization.ImmunizationPerformerComponent performer = new Immunization.ImmunizationPerformerComponent();
	  performer.setFunction(new CodeableConcept().addCoding(new Coding().setSystem(PERFORMER_FUNCTION_SYSTEM).setCode(functionCode).setDisplay(functionDisplay)));
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