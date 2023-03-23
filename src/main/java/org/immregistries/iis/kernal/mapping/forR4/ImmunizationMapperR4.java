package org.immregistries.iis.kernal.mapping.forR4;

import org.immregistries.iis.kernal.fhir.annotations.OnR4Condition;
import org.hl7.fhir.r4.model.*;
import org.immregistries.iis.kernal.mapping.Interfaces.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.model.ModelPerson;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.model.VaccinationReported;
import org.immregistries.iis.kernal.repository.FhirRequesterR4;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

@Service("ImmunizationMapperR4")
@Conditional(OnR4Condition.class)
public class ImmunizationMapperR4 implements ImmunizationMapper<Immunization> {
	@Autowired
	FhirRequesterR4 fhirRequests;
	public VaccinationReported getReportedWithMaster(Immunization i) {
		VaccinationReported vaccinationReported = getReported(i);
		VaccinationMaster vaccinationMaster = fhirRequests.searchVaccinationMaster(
			Immunization.IDENTIFIER.exactly().systemAndIdentifier(
				vaccinationReported.getVaccinationReportedExternalLinkSystem(),
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
//		vr.setVaccinationReportedExternalLink(MappingHelper.filterIdentifier(i.getIdentifier(),MappingHelper.VACCINATION_REPORTED).getValue());
		vr.setVaccinationReportedExternalLink(i.getIdentifierFirstRep().getValue()); // TODO
		if (i.getPatient() != null && i.getPatient().getReference() != null && !i.getPatient().getReference().isBlank()) {
			vr.setPatientReported(fhirRequests.readPatientReported(i.getPatient().getReference()));
//			vr.setPatientReported(fhirRequests.readPatientReported(i.getPatient().getReference().split("Patient/")[0]));
		}

		vr.setReportedDate(i.getRecorded());
		vr.setAdministeredDate(i.getOccurrenceDateTimeType().getValue());

		i.getVaccineCode().getCoding().forEach(coding -> {
			switch (coding.getSystem()) {
				case CVX: {
					vr.setVaccineCvxCode(coding.getCode());
					break;
				}
				case NDC: {
					vr.setVaccineNdcCode(coding.getCode());
					break;
				}
				case MVX: {
					vr.setVaccineMvxCode(coding.getCode());
					break;
				}
			}
		});

		vr.setVaccineMvxCode(i.getManufacturer().getIdentifier().getValue());

		vr.setAdministeredAmount(i.getDoseQuantity().getValue().toString());

		vr.setInformationSource(i.getReportOrigin().getCodingFirstRep().getCode());
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

		if (i.getLocation() != null && i.getLocation().getReference() != null && !i.getLocation().getReference().isBlank()) {
			vr.setOrgLocation(fhirRequests.readOrgLocation(i.getLocation().getReference()));
		}

		Extension informationSource = i.getExtensionByUrl(INFORMATION_SOURCE_EXTENSION);
		if (informationSource != null
			&& MappingHelper.extensionGetCoding(informationSource) != null
			&& MappingHelper.extensionGetCoding(informationSource).getSystem().equals(INFORMATION_SOURCE)) {
			vr.setEnteredBy(fhirRequests.readPractitionerPerson(MappingHelper.extensionGetCoding(informationSource).getCode()));
		}
//		if (i.getsou.getInformationSource() && i.getInformationSource().getReference() != null && !i.getInformationSourceReference().getReference().isBlank()) {
//			vr.setEnteredBy(fhirRequests.readPractitionerPerson(i.getInformationSourceReference().getReference()));
//		} else {
//		}
		for (Immunization.ImmunizationPerformerComponent performer : i.getPerformer()) {
			if (performer.getActor() != null && performer.getActor().getReference() != null && !performer.getActor().getReference().isBlank()) {
				switch (performer.getFunction().getCodingFirstRep().getCode()) {
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
	  vaccinationMaster.setExternalLink(i.getIdentifierFirstRep().getValue()); // TODO
	  vaccinationMaster.setAdministeredDate(i.getOccurrenceDateTimeType().getValue());
	  vaccinationMaster.setVaccineCvxCode(
		  i.getVaccineCode().getCoding().stream().filter(coding -> coding.getSystem().equals(CVX)).findFirst().orElse(new Coding()).getCode()
	  );
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
//	  i.addIdentifier(MappingHelper.getFhirIdentifier(MappingHelper.VACCINATION_REPORTED, vr.getVaccinationReportedExternalLink()));
	  i.setPatient(new Reference().setReference("Patient/" + vr.getPatientReported().getId()));
	  i.setRecorded(vr.getReportedDate());
	  i.getOccurrenceDateTimeType().setValue(vr.getAdministeredDate());

	  if (!vr.getVaccineCvxCode().isBlank()) {
		  i.getVaccineCode().addCoding().setCode(vr.getVaccineCvxCode()).setSystem(CVX);
	  }
	  if (!vr.getVaccineNdcCode().isBlank()) {
		  i.getVaccineCode().addCoding().setCode(vr.getVaccineNdcCode()).setSystem(NDC);
	  }
//	  i.setManufacturer(MappingHelper.getFhirReference(MappingHelper.ORGANISATION,MVX,vr.getVaccineMvxCode()));

	  if (vr.getAdministeredAmount() != null && !vr.getAdministeredAmount().isBlank()) {
		  i.setDoseQuantity(new Quantity().setValue(new BigDecimal(vr.getAdministeredAmount())));
	  }

	  Extension informationSource = new Extension(INFORMATION_SOURCE_EXTENSION);
	  informationSource.setValue(new Coding().setSystem(INFORMATION_SOURCE).setCode(vr.getInformationSource()));
//	  i.setInformationSource(new CodeableConcept(new Coding().setSystem(INFORMATION_SOURCE).setCode(vr.getInformationSource()))); // TODO change system name
	  i.setLotNumber(vr.getLotnumber());
	  i.setExpirationDate(vr.getExpirationDate());

	  if (vr.getActionCode().equals("D")) {
		  i.setStatus(Immunization.ImmunizationStatus.ENTEREDINERROR);
	  } else {
		  switch (vr.getCompletionStatus()) {
			  case "CP": {
				  i.setStatus(Immunization.ImmunizationStatus.COMPLETED);
				  break;
			  }
			  case "NA" :
			  case "PA" :
			  case "RE" : {
				  i.setStatus(Immunization.ImmunizationStatus.NOTDONE);
				  break;
			  }
			  case "" :
			  default: {
//					 i.setStatus(Immunization.ImmunizationStatus.NULL);
				  break;
			  }
		  }
	  }
	  i.setStatusReason(new CodeableConcept(new Coding(REFUSAL_REASON_CODE,vr.getRefusalReasonCode(),vr.getRefusalReasonCode())));
	  i.getSite().addCoding().setSystem(BODY_PART).setCode(vr.getBodySite());
	  i.getRoute().addCoding().setSystem(BODY_ROUTE).setCode(vr.getBodyRoute());
	  i.getFundingSource().addCoding().setSystem(FUNDING_SOURCE).setCode(vr.getFundingSource());
	  i.addProgramEligibility().addCoding().setSystem(FUNDING_ELIGIBILITY).setCode(vr.getFundingEligibility());


	  if (!vr.getOrgLocationId().isBlank()) {
		  i.setLocation(new Reference(MappingHelper.LOCATION + "/" + vr.getOrgLocationId()));
	  }

	  if (vr.getEnteredBy() != null) {
//		  i.setInformationSource(new Reference(MappingHelper.PRACTITIONER+"/" + vr.getEnteredBy().getPersonId())); TODO
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
	  Reference actor = null;
	  switch (person.getIdentifierTypeCode()) {
		  case MappingHelper.PRACTITIONER: {
			  actor = new Reference(MappingHelper.PRACTITIONER+"/"+person.getPersonId());
			  break;
		  }
//		  case MappingHelper.PERSON: { TODO
//			  actor = MappingHelper.getFhirReference(
//				  MappingHelper.PERSON,
//				  MappingHelper.PERSON_MODEL,
//				  person.getPersonId(),
//				  person.getPersonId());
//			  break;
//		  }
//		  default:{
//			  actor = MappingHelper.getFhirReference(MappingHelper.PRACTITIONER, person.getIdentifierTypeCode(), person.getPersonExternalLink(), person.getPersonId());
//		  }
	  }
	  performer.setActor(actor);
	  return performer;
  }


}