package org.immregistries.iis.kernal.mapping.mappers.resources.r5;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.*;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.immregistries.iis.kernal.logic.CodeMapManagerService;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.mapping.mappers.fields.r5.BusinessIdentifierMapperR5;
import org.immregistries.iis.kernal.mapping.mappers.fields.r5.ModelReferenceMapperR5;
import org.immregistries.iis.kernal.mapping.mappers.resources.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.requesters.FhirReadRequester;
import org.immregistries.iis.kernal.model.BusinessIdentifier;
import org.immregistries.iis.kernal.model.IisVaccination;
import org.immregistries.iis.kernal.model.ModelPerson;
import org.immregistries.iis.kernal.model.ModelReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Conditional(OnR5Condition.class)
public class ImmunizationMapperR5 extends ImmunizationMapper<Immunization> implements IR5Mapper<IisVaccination, Immunization> {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private LocationMapperR5 locationMapper;
	@Autowired
	private FhirReadRequester fhirReadRequester;
	@Autowired
	private CodeMapManagerService codeMapManagerService;
	@Autowired
	private BusinessIdentifierMapperR5 businessIdentifierMapper;
	@Autowired
	private ModelReferenceMapperR5 modelReferenceMapperR5;

	public void fillFromFhirResource(IisVaccination vr, Immunization i) {
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
			vr.addBusinessIdentifier(businessIdentifierMapper.localObject(identifier));
		}
		/*
		 * Patient
		 */
		if (i.getPatient() != null && StringUtils.isNotBlank(i.getPatient().getReference())) {
			vr.setPatientReported(fhirReadRequester.readAsPatientReported(i.getPatient().getReference()));
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
		vr.setVaccineCvxCode(StringUtils.defaultString(i.getVaccineCode().getCode(CVX_SYSTEM)));
		vr.setVaccineNdcCode(StringUtils.defaultString(i.getVaccineCode().getCode(NDC_SYSTEM)));
		vr.setVaccineMvxCode(StringUtils.defaultString(i.getVaccineCode().getCode(MVX_SYSTEM)));
		/*
		 * if only one code specified without system
		 */
		if (i.getVaccineCode().getCoding().size() == 1
				&& StringUtils.isBlank(i.getVaccineCode().getCodingFirstRep().getSystem())) {
			vr.setVaccineCvxCode(StringUtils.defaultString(i.getVaccineCode().getCodingFirstRep().getCode()));
		}

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
			switch (i.getStatus()) {
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
				} // Could also be NA or PA
				case NULL:
				default:
					vr.setCompletionStatus("");
					break;
			}
		}
		/*
		 * Completion Status Code extension to store exact value
		 */
		Extension completionStatusExtension = i.getExtensionByUrl(COMPLETION_STATUS_EXTENSION);
		if (completionStatusExtension != null) {
			if (completionStatusExtension.hasValue()) {
				vr.setCompletionStatus(MappingHelper.extensionGetCoding(completionStatusExtension).getCode());
			}
		} else {
			vr.setCompletionStatus(null);
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
		if (i.getProgramEligibilityFirstRep().hasProgram()
				&& i.getProgramEligibilityFirstRep().getProgram().hasCoding()) {
			vr.setFundingEligibility(i.getProgramEligibilityFirstRep().getProgram().getCodingFirstRep().getCode());
		}
		/*
		 * Location
		 */
		if (i.getLocation() != null && StringUtils.isNotBlank(i.getLocation().getReference())) {
			vr.setOrgLocation(fhirReadRequester.readAsOrgLocation(i.getLocation().getReference()));
		}
		/*
		 * Information Source
		 */
		if (i.getInformationSource().hasConcept() && i.getInformationSource().getConcept().hasCoding()) {
			vr.setInformationSource(i.getInformationSource().getConcept().getCodingFirstRep().getCode());
		}
		/*
		 * Performers
		 * TODO choose where to get entering Practitioner between information source and
		 * Performer
		 */
		if (i.hasInformationSource() && i.getInformationSource().getReference() != null
				&& StringUtils.isNotBlank(i.getInformationSource().getReference().getReference())) {
			vr.setEnteredBy(
					fhirReadRequester.readPractitionerAsPerson(i.getInformationSource().getReference().getReference()));
		}
		for (Immunization.ImmunizationPerformerComponent performer : i.getPerformer()) {
			if (performer.getActor() != null && StringUtils.isNotBlank(performer.getActor().getReference())) {
				switch (performer.getFunction().getCode(PERFORMER_FUNCTION_SYSTEM)) {
					case ADMINISTERING_VALUE: {
						vr.setAdministeringProvider(
								fhirReadRequester.readPractitionerAsPerson(performer.getActor().getReference()));
						break;
					}
					case ORDERING_VALUE: {
						vr.setOrderingProvider(
								fhirReadRequester.readPractitionerAsPerson(performer.getActor().getReference()));
						break;
					}
					case ENTERING_VALUE: {
						vr.setEnteredBy(
								fhirReadRequester.readPractitionerAsPerson(performer.getActor().getReference()));
						break;
					}
				}
			}
		}
	}

	public Immunization fhirObject(IisVaccination vr) {
		Immunization i = new Immunization();
		/*
		 * Id
		 */
		i.setId(StringUtils.defaultString(vr.getVaccinationId()));
		/*
		 * Last updated Date
		 */
		i.getMeta().setLastUpdated(vr.getUpdatedDate());
		/*
		 * Identifiers
		 */
		for (BusinessIdentifier businessIdentifier : vr.getBusinessIdentifiers()) {
			i.addIdentifier(businessIdentifierMapper.fhirObject(businessIdentifier));
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
			i.setManufacturer(MappingHelper.getFhirCodeableReferenceR5(MappingHelper.ORGANIZATION, MVX_SYSTEM,
					vr.getVaccineMvxCode()));
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
			i.addExtension().setUrl(ACTION_CODE_EXTENSION)
					.setValue(new Coding().setCode(vr.getActionCode()).setSystem(ACTION_CODE_SYSTEM));
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
						// i.setStatus(Immunization.ImmunizationStatusCodes..NULL);
						break;
					}
				}
			}
		}
		if (vr.getCompletionStatus() != null) {
			Extension completionStatusExtension = i.addExtension().setUrl(COMPLETION_STATUS_EXTENSION);
			if (StringUtils.isNotBlank(vr.getCompletionStatus())) {
				completionStatusExtension
						.setValue(new Coding().setCode(vr.getCompletionStatus()).setSystem(COMPLETION_STATUS_SYSTEM));
			}
		}
		/*
		 * Status Reason
		 */
		if (vr.getRefusalReasonCode() != null) {
			Coding coding = new Coding().setSystem(REFUSAL_REASON_CODE).setCode(vr.getRefusalReasonCode());
			Code code = codeMapManagerService.getCodeMap().getCodeForCodeset(CodesetType.VACCINATION_REFUSAL,
					vr.getRefusalReasonCode());
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
			Code code = codeMapManagerService.getCodeMap().getCodeForCodeset(CodesetType.BODY_SITE, vr.getBodySite());
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
			Code code = codeMapManagerService.getCodeMap().getCodeForCodeset(CodesetType.BODY_ROUTE, vr.getBodyRoute());
			if (code != null) {
				coding.setDisplay(code.getLabel());
			}
			i.getRoute().addCoding(coding);
		}
		/*
		 * Funding Source
		 */
		if (StringUtils.isNotBlank(vr.getFundingSource())) {
			Coding coding = new Coding().setSystem(FUNDING_SOURCE_SYSTEM).setCode(vr.getFundingSource());
			Code code = codeMapManagerService.getCodeMap().getCodeForCodeset(CodesetType.VACCINATION_FUNDING_SOURCE,
					vr.getFundingSource());
			if (code != null) {
				coding.setDisplay(code.getLabel());
			}
			i.getFundingSource().addCoding(coding);
		}
		/*
		 * Program Funding Eligibility
		 */
		if (StringUtils.isNotBlank(vr.getFundingEligibility())) {
			Coding coding = new Coding().setSystem(FUNDING_ELIGIBILITY).setCode(vr.getFundingEligibility());
			Code code = codeMapManagerService.getCodeMap().getCodeForCodeset(CodesetType.FINANCIAL_STATUS_CODE,
					vr.getFundingEligibility());
			if (code != null) {
				coding.setDisplay(code.getLabel());
			}
			i.addProgramEligibility().setProgram(new CodeableConcept(coding));
		}
		/*
		 * Location
		 */
		Location location = locationMapper.fhirObject(vr.getOrgLocation()); // Should have been saved in
																				// Event/MessageHandler
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
			Coding coding = new Coding().setSystem(INFORMATION_SOURCE).setCode(vr.getInformationSource()); // TODO
																											// change
																											// system
																											// name
			Code code = codeMapManagerService.getCodeMap().getCodeForCodeset(CodesetType.VACCINATION_INFORMATION_SOURCE,
					vr.getInformationSource());
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
			// CodeableReference informationSource = i.getInformationSource();
			// if (informationSource == null) {
			// informationSource = new CodeableReference();
			// i.setInformationSource(informationSource);
			// }
			// informationSource.setReference(new Reference(MappingHelper.PRACTITIONER + "/"
			// + vr.getEnteredBy().getPersonId()));
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

	@Override
	public ModelReference extractPatientReference(Immunization immunization) {
		return modelReferenceMapperR5.localObject(immunization.getPatient());
	}

	private Immunization.ImmunizationPerformerComponent performer(ModelPerson person, String functionCode,
			String functionDisplay) {
		Immunization.ImmunizationPerformerComponent performer = new Immunization.ImmunizationPerformerComponent();
		performer.setFunction(new CodeableConcept().addCoding(
				new Coding().setSystem(PERFORMER_FUNCTION_SYSTEM).setCode(functionCode).setDisplay(functionDisplay)));
		Reference actor;
		if (person.getIdentifierTypeCode().equals(MappingHelper.PRACTITIONER)) {
			actor = new Reference(MappingHelper.PRACTITIONER + "/" + person.getPersonId());
		} else {
			actor = MappingHelper.getFhirReferenceR5(MappingHelper.PRACTITIONER, person.getIdentifierTypeCode(),
					person.getPersonExternalLink(), person.getPersonId());
		}
		performer.setActor(actor);
		return performer;
	}

}