package org.immregistries.iis.kernal.mapping.mappers.resources.r4;

import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.mapping.mappers.fields.r4.BusinessIdentifierMapperR4;
import org.immregistries.iis.kernal.mapping.mappers.fields.r4.ModelReferenceMapperR4;
import org.immregistries.iis.kernal.mapping.mappers.resources.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.requesters.FhirSaveRequesterR4;
import org.immregistries.iis.kernal.model.BusinessIdentifier;
import org.immregistries.iis.kernal.model.IisVaccination;
import org.immregistries.iis.kernal.model.ModelPerson;
import org.immregistries.iis.kernal.model.ModelReference;
import org.immregistries.iis.kernal.services.CodeMapManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service("ImmunizationMapperR4")
@Conditional(OnR4Condition.class)
public class ImmunizationMapperR4 extends ImmunizationMapper<Immunization> implements IR4ResourceMapper<IisVaccination, Immunization> {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private FhirSaveRequesterR4 fhirSaveRequesterR4;
	@Autowired
	private CodeMapManagerService codeMapManagerService;
	@Autowired
	private BusinessIdentifierMapperR4 businessIdentifierMapperR4;
	@Autowired
	private ModelReferenceMapperR4 modelReferenceMapperR4;

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
			vr.addBusinessIdentifier(businessIdentifierMapperR4.localObject(identifier));
		}
		/*
		 * Patient
		 */
		if (i.getPatient() != null && StringUtils.isNotBlank(i.getPatient().getReference())) {
			String id = i.getPatient().getReference();
			vr.setPatientReported(fhirSaveRequesterR4.fhirReadRequester.readAsPatientReported(id));
		}
		/*
		 * Reported Date
		 */
		vr.setReportedDate(i.getRecorded());
		/*
		 * Administered Date
		 */
		vr.setAdministeredDate(i.getOccurrenceDateTimeType().getValue());
		/*
		 * Vaccine Codes CVX NDC
		 * MVX ?
		 */
		i.getVaccineCode().getCoding().forEach(coding -> {
			switch (coding.getSystem()) {
				case CVX_SYSTEM: {
					vr.setVaccineCvxCode(StringUtils.defaultString(coding.getCode()));
					break;
				}
				case NDC_SYSTEM: {
					vr.setVaccineNdcCode(StringUtils.defaultString(coding.getCode()));
					break;
				}
				case MVX_SYSTEM: {
					vr.setVaccineMvxCode(StringUtils.defaultString(coding.getCode()));
					break;
				}
			}
		});
		/*
		 * if only one code specified without system, considered cvx , TODO remove or
		 * make flavor ?
		 */
		if (i.getVaccineCode().getCoding().size() == 1
				&& StringUtils.isBlank(i.getVaccineCode().getCodingFirstRep().getSystem())) {
			vr.setVaccineCvxCode(StringUtils.defaultString(i.getVaccineCode().getCodingFirstRep().getCode()));
		}
		/*
		 * Manufacturer MVX TODO establish priority
		 */
		if (i.hasManufacturer()) {
			vr.setVaccineMvxCode(i.getManufacturer().getIdentifier().getValueElement().getValueNotNull());
		}
		/*
		 * Administered Amount
		 */
		if (i.getDoseQuantity().hasValue()) {
			vr.setAdministeredAmount(i.getDoseQuantity().getValue().toString());
		}
		/*
		 * Information Source
		 */
		if (i.getReportOrigin().hasCoding()) {
			vr.setInformationSource(StringUtils.defaultString(i.getReportOrigin().getCodingFirstRep().getCode()));
		}
		/*
		 * Lot Number
		 */
		vr.setLotnumber(StringUtils.defaultString(i.getLotNumber()));
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
		if (i.getProgramEligibilityFirstRep().hasCoding()) {
			vr.setFundingEligibility(
					StringUtils.defaultString(i.getProgramEligibilityFirstRep().getCodingFirstRep().getCode()));
		}
		/*
		 * Location
		 */
		if (i.getLocation() != null && StringUtils.isNotBlank(i.getLocation().getReference())) {
			String id = i.getLocation().getReference();
			vr.setOrgLocation(fhirSaveRequesterR4.fhirReadRequester.readAsOrgLocation(id));
		}
		/*
		 * Performers
		 */
		for (Immunization.ImmunizationPerformerComponent performer : i.getPerformer()) {
			if (performer.getActor() != null && StringUtils.isNotBlank(performer.getActor().getReference())
					&& performer.getActor().getReferenceElement().getResourceType().equals("Practitioner")) {
				switch (performer.getFunction().getCodingFirstRep().getCode()) {
					case ADMINISTERING_VALUE: {
						String id = performer.getActor().getReference();
						vr.setAdministeringProvider(fhirSaveRequesterR4.fhirReadRequester.readPractitionerAsPerson(id));
						break;
					}
					case ORDERING_VALUE: {
						String id = performer.getActor().getReference();
						vr.setOrderingProvider(fhirSaveRequesterR4.fhirReadRequester.readPractitionerAsPerson(id));
						break;
					}
					case ENTERING_VALUE: {
						String id = performer.getActor().getReference();
						vr.setEnteredBy(fhirSaveRequesterR4.fhirReadRequester.readPractitionerAsPerson(id));
						break;
					}
				}
			}
		}
	}

	@Override
	public Class<Immunization> fhirType() {
		return Immunization.class;
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
			i.addIdentifier(businessIdentifierMapperR4.fhirObject(businessIdentifier));
		}
		/*
		 * Patient
		 */
		i.setPatient(new Reference().setReference("Patient/" + vr.getPatientReported().getPatientId()));
		/*
		 * Recorded Date
		 */
		i.setRecorded(vr.getReportedDate());
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
			i.setManufacturer(new Reference()
					.setIdentifier(new Identifier().setSystem(MVX_SYSTEM).setValue(vr.getVaccineMvxCode())));
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
				.setValue(new Coding().setCode(vr.getActionCode()).setSystem(ACTION_CODE_TABLE));
			if (vr.getActionCode().equals("D")) {
				i.setStatus(Immunization.ImmunizationStatus.ENTEREDINERROR);
			} else {
				switch (vr.getCompletionStatus()) {
					case "CP": {
						i.setStatus(Immunization.ImmunizationStatus.COMPLETED);
						break;
					}
					case "NA":
					case "PA":
					case "RE": {
						i.setStatus(Immunization.ImmunizationStatus.NOTDONE);
						break;
					}
					case "":
					default: {
						// i.setStatus(Immunization.ImmunizationStatus.NULL);
						break;
					}
				}
			}
		}
		if (vr.getCompletionStatus() != null) {
			Extension completionStatusExtension = i.addExtension().setUrl(COMPLETION_STATUS_EXTENSION);
			if (StringUtils.isNotBlank(vr.getCompletionStatus())) {
				completionStatusExtension
					.setValue(new Coding().setCode(vr.getCompletionStatus()).setSystem(COMPLETION_STATUS_TABLE));
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
			i.addProgramEligibility().addCoding(coding);
		}
		/*
		 * Location
		 */
		if (!vr.getOrgLocationId().isBlank()) {
			i.setLocation(new Reference(MappingHelper.LOCATION + "/" + vr.getOrgLocationId()));
		}
		/*
		 * Information Source / Report Origin
		 */
		if (vr.getInformationSource() != null) {
			Coding coding = new Coding().setSystem(INFORMATION_SOURCE).setCode(vr.getInformationSource());
			Code code = codeMapManagerService.getCodeMap().getCodeForCodeset(CodesetType.VACCINATION_INFORMATION_SOURCE,
					vr.getInformationSource());
			if (code != null) {
				coding.setDisplay(code.getLabel());
			}
			i.setReportOrigin(new CodeableConcept(coding));
		}
		/*
		 * Entering Performer
		 */
		if (vr.getEnteredBy() != null) {
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
		return modelReferenceMapperR4.localObject(immunization.getPatient());
	}

	public Immunization.ImmunizationPerformerComponent performer(ModelPerson person, String functionCode,
			String functionDisplay) {
		Immunization.ImmunizationPerformerComponent performer = new Immunization.ImmunizationPerformerComponent();
		performer.setFunction(new CodeableConcept().addCoding(
				new Coding().setSystem(PERFORMER_FUNCTION_SYSTEM).setCode(functionCode).setDisplay(functionDisplay)));
		Reference actor;
		if (person.getIdentifierTypeCode().equals(MappingHelper.PRACTITIONER)) {
			actor = new Reference(MappingHelper.PRACTITIONER + "/" + person.getPersonId());
		} else {
			actor = MappingHelper.getFhirReferenceR4(MappingHelper.PRACTITIONER, person.getIdentifierTypeCode(),
					person.getPersonExternalLink(), person.getPersonId());
		}
		performer.setActor(actor);
		return performer;
	}

}