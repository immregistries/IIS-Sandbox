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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Conditional(OnR5Condition.class)
public class ImmunizationMapperR5 implements ImmunizationMapper<Immunization> {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private LocationMapperR5 locationMapper;
	@Autowired
	private FhirRequesterR5 fhirRequests;

	public VaccinationReported localObjectReportedWithMaster(Immunization i) {
		VaccinationReported vaccinationReported = this.localObjectReported(i);
		VaccinationMaster vaccinationMaster = fhirRequests.searchVaccinationMaster(
			new SearchParameterMap(Immunization.SP_IDENTIFIER, new TokenParam().setValue(vaccinationReported.getFillerBusinessIdentifier().getValue())));
		if (vaccinationMaster!= null) {
			vaccinationReported.setVaccinationMaster(vaccinationMaster);
		}
		return vaccinationReported;
	}

	public VaccinationReported localObjectReported(Immunization i) {
		VaccinationReported vaccinationReported = new VaccinationReported();
//		if (FhirRequester.isGoldenRecord(i)) {
//			logger.info("Mapping refused for report as patient is golden");
//			return null;
//		}
		fillFromFhirResource(vaccinationReported, i);
		return vaccinationReported;
	}

	public VaccinationMaster localObject(Immunization i) {
		VaccinationMaster vaccinationMaster = new VaccinationMaster();
//		if (!FhirRequester.isGoldenRecord(i)) {
//			logger.info("Mapping refused for golden as patient is report");
//			return null;
//		}
		fillFromFhirResource(vaccinationMaster, i);
		return vaccinationMaster;
	}

	public void fillFromFhirResource(VaccinationMaster vr, Immunization i) {
		/*
		 * Id
		 */
		vr.setVaccinationId(StringUtils.defaultString(new IdType(i.getId()).getIdPart()));
		/*
		 * Updated date
		 */
		vr.setUpdatedDate(i.getMeta().getLastUpdated());
		/*
		 * Business identifier
		 */
		for (Identifier identifier : i.getIdentifier()) {
			vr.addBusinessIdentifier(BusinessIdentifier.fromR5(identifier));
		}
		/*
		 * Patient
		 */
		if (i.getPatient() != null && StringUtils.isNotBlank(i.getPatient().getReference())) {
			vr.setPatientReported(fhirRequests.readAsPatientReported(i.getPatient().getReference()));
		}
		/*
		 * Reported Date
		 */
		Extension recorded = i.getExtensionByUrl(RECORDED);
		if (recorded != null) {
			vr.setReportedDate(MappingHelper.extensionGetDate(recorded));
		} else {
			vr.setReportedDate(null);
		}
		/*
		 * Administered Date
		 */
		vr.setAdministeredDate(i.getOccurrenceDateTimeType().getValue());

		/*
		 * Vaccine Codes CVX NDC
		 * MVX ?
		 */
		vr.setVaccineCvxCode(i.getVaccineCode().getCode(CVX_SYSTEM));
		vr.setVaccineNdcCode(i.getVaccineCode().getCode(NDC_SYSTEM));
		vr.setVaccineMvxCode(i.getVaccineCode().getCode(MVX_SYSTEM));

		/*
		 * Manufacturer MVX
		 */
		vr.setVaccineMvxCode(i.getManufacturer().getReference().getIdentifier().getValue());

		/*
		 * Administered Amount
		 */
		if (i.getDoseQuantity().hasValue()) {
			vr.setAdministeredAmount(i.getDoseQuantity().getValue().toString());
		}


		/*
		 * Updated Date
		 */
		vr.setUpdatedDate(i.getMeta().getLastUpdated());
		/*
		 * Lot Number
		 */
		vr.setLotnumber(i.getLotNumber());
		/*
		 * Expiration Date
		 */
		vr.setExpirationDate(i.getExpirationDate());
		/*
		 * Status Action code
		 */
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
		/*
		 * Action Code extension to store exact value
		 */
		Extension actionCode = i.getExtensionByUrl(ACTION_CODE_EXTENSION);
		if (actionCode != null) {
			if (actionCode.hasValue()) {
				vr.setActionCode(MappingHelper.extensionGetCoding(actionCode).getCode());
			}
		} else {
			vr.setActionCode(null);
		}
		/*
		 * Refusal reason code
		 */
		if (i.getStatusReason().hasCoding()) {
			vr.setRefusalReasonCode(StringUtils.defaultString(i.getStatusReason().getCodingFirstRep().getCode()));
		}
		/*
		 * Injection Site
		 */
		if (i.getSite().hasCoding()) {
			vr.setBodySite(StringUtils.defaultString(i.getSite().getCodingFirstRep().getCode()));
		}
		/*
		 * Injection Route
		 */
		if (i.getRoute().hasCoding()) {
			vr.setBodyRoute(StringUtils.defaultString(i.getRoute().getCodingFirstRep().getCode()));
		}
		/*
		 * Funding Source
		 */
		if (i.getFundingSource().hasCoding()) {
			vr.setFundingSource(StringUtils.defaultString(i.getFundingSource().getCodingFirstRep().getCode()));
		}
		/*
		 * Funding, program eligibility
		 */
		if (i.getProgramEligibilityFirstRep().hasProgram() && i.getProgramEligibilityFirstRep().getProgram().hasCoding()) {
			vr.setFundingEligibility(i.getProgramEligibilityFirstRep().getProgram().getCodingFirstRep().getCode());
		}
		/*
		 * Location
		 */
		if (i.getLocation() != null && StringUtils.isNotBlank(i.getLocation().getReference())) {
			vr.setOrgLocation(fhirRequests.readAsOrgLocation(i.getLocation().getReference()));
		}
		/*
		 * Information Source
		 */
		if (i.getInformationSource().hasConcept() && i.getInformationSource().getConcept().hasCoding()) {
			vr.setInformationSource(i.getInformationSource().getConcept().getCode(INFORMATION_SOURCE));
		}
		/*
		 * Performers
		 * TODO choose where to get entering Practitioner between information source and Performer
		 */
		if (i.hasInformationSource() && i.getInformationSource().getReference() != null && StringUtils.isNotBlank(i.getInformationSource().getReference().getReference())) {
			vr.setEnteredBy(fhirRequests.readPractitionerAsPerson(i.getInformationSource().getReference().getReference()));
		}
		for (Immunization.ImmunizationPerformerComponent performer : i.getPerformer()) {
			if (performer.getActor() != null && StringUtils.isNotBlank(performer.getActor().getReference())) {
				switch (performer.getFunction().getCode(PERFORMER_FUNCTION_SYSTEM)) {
					case ADMINISTERING_VALUE: {
						vr.setAdministeringProvider(fhirRequests.readPractitionerAsPerson(performer.getActor().getReference()));
						break;
					}
					case ORDERING_VALUE: {
						vr.setOrderingProvider(fhirRequests.readPractitionerAsPerson(performer.getActor().getReference()));
						break;
					}
					case ENTERING_VALUE: {
						vr.setEnteredBy(fhirRequests.readPractitionerAsPerson(performer.getActor().getReference()));
						break;
					}
				}
			}
		}
	}

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
		  i.getVaccineCode().addCoding().setCode(vr.getVaccineCvxCode()).setSystem(CVX_SYSTEM);
	  }
	  /*
		* NDC
		*/
	  if (StringUtils.isNotBlank(vr.getVaccineNdcCode())) {
		  i.getVaccineCode().addCoding().setCode(vr.getVaccineNdcCode()).setSystem(NDC_SYSTEM);
	  }
	  /*
		* Manufacturer MVX
		*/
	  if (StringUtils.isNotBlank(vr.getVaccineMvxCode())) {
		  i.setManufacturer(MappingHelper.getFhirCodeableReferenceR5(MappingHelper.ORGANIZATION, MVX_SYSTEM, vr.getVaccineMvxCode()));
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
		  Coding coding = new Coding().setSystem(BODY_PART_SITE_SYSTEM).setCode(vr.getBodySite());
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
		  Coding coding = new Coding().setSystem(BODY_ROUTE_SYSTEM).setCode(vr.getBodyRoute());
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
		  Coding coding = new Coding().setSystem(FUNDING_SOURCE_SYSTEM).setCode(vr.getFundingSource());
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
		  i.addPerformer(performer(vr.getEnteredBy(), ENTERING_VALUE, ENTERING_DISPLAY));
	  }
	  /*
		* Ordering Performer
		*/
	  if (vr.getOrderingProvider() != null) {
		  i.addPerformer(performer(vr.getOrderingProvider(), ORDERING_VALUE, ORDERING_DISPLAY));
	  }
	  /*
		* Administering Performer
		*/
	  if (vr.getAdministeringProvider() != null) {
		  i.addPerformer(performer(vr.getAdministeringProvider(), ADMINISTERING_VALUE, ADMINISTERING_DISPLAY));
	  }
     return i;
  }

  private Immunization.ImmunizationPerformerComponent performer(ModelPerson person, String functionCode, String functionDisplay ) {
	  Immunization.ImmunizationPerformerComponent performer = new Immunization.ImmunizationPerformerComponent();
	  performer.setFunction(new CodeableConcept().addCoding(new Coding().setSystem(PERFORMER_FUNCTION_SYSTEM).setCode(functionCode).setDisplay(functionDisplay)));
	  Reference actor;
	  if (person.getIdentifierTypeCode().equals(MappingHelper.PRACTITIONER)) {
		  actor = new Reference(MappingHelper.PRACTITIONER + "/" + person.getPersonId());
	  } else {
		  actor = MappingHelper.getFhirReferenceR5(MappingHelper.PRACTITIONER, person.getIdentifierTypeCode(), person.getPersonExternalLink(), person.getPersonId());
	  }
	  performer.setActor(actor);
	  return performer;
  }


}