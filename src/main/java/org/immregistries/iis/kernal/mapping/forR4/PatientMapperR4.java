package org.immregistries.iis.kernal.mapping.forR4;


import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.logic.CodeMapManager;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.mapping.interfaces.PatientMapper;
import org.immregistries.iis.kernal.mapping.internalClient.FhirRequesterR4;
import org.immregistries.iis.kernal.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static org.immregistries.iis.kernal.mapping.interfaces.ImmunizationMapper.RECORDED;
import static org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester.GOLDEN_RECORD;
import static org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester.GOLDEN_SYSTEM_TAG;


@Service
@Conditional(OnR4Condition.class)
public class PatientMapperR4 implements PatientMapper<Patient> {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private FhirRequesterR4 fhirRequests;

	public PatientReported localObjectReportedWithMaster(Patient p) {
		PatientReported patientReported = localObjectReported(p);
		if (!p.getId().isBlank() && p.getMeta().getTag(GOLDEN_SYSTEM_TAG, GOLDEN_RECORD) == null) {
			patientReported.setPatientMaster(fhirRequests.readPatientMasterWithMdmLink(p.getId()));
		}
		return patientReported;
	}

	public PatientMaster localObject(Patient patient) {
		PatientMaster patientMaster = new PatientMaster();
//		if (AbstractFhirRequester.isGoldenRecord(patient)) {
//			logger.info("Mapping refused for report as patient is golden");
//			return null;
//		}
		fillFromFhirResource(patientMaster, patient);
		return patientMaster;
	}

	public PatientReported localObjectReported(Patient patient) {
		PatientReported patientReported = new PatientReported();
//		if (!AbstractFhirRequester.isGoldenRecord(patient)) {
//			logger.info("Mapping refused for golden as Patient is reported");
//			return null;
//		}
		fillFromFhirResource(patientReported, patient);
		return patientReported;
	}

	private void fillFromFhirResource(PatientMaster localPatient, Patient patient) {
		if (StringUtils.isNotBlank(patient.getId())) {
			localPatient.setPatientId(new IdType(patient.getId()).getIdPart());
		}
		/*
		 * Updated date
		 */
		localPatient.setUpdatedDate(patient.getMeta().getLastUpdated());
		/*
		 * Reported Date
		 */
		Extension recorded = patient.getExtensionByUrl(RECORDED);
		if (recorded != null) {
			localPatient.setReportedDate(MappingHelper.extensionGetDate(recorded));
		} else {
			localPatient.setReportedDate(null);
		}
		/*
		 * Identifiers
		 */
		for (Identifier identifier : patient.getIdentifier()) {
			localPatient.addBusinessIdentifier(BusinessIdentifier.fromR4(identifier));
		}
		/*
		 * Birth Date
		 */
		localPatient.setBirthDate(patient.getBirthDate());
		/*
		 * Managing organization
		 */
		if (patient.hasManagingOrganization() && patient.getManagingOrganization().hasReference()) {
			localPatient.setManagingOrganizationId(StringUtils.defaultString(patient.getManagingOrganization().getReference()));
		}
		/*
		 * Names
		 */
		List<ModelName> modelNames = new ArrayList<>(patient.getName().size());
		localPatient.setPatientNames(modelNames);
		for (HumanName name : patient.getName()) {
			modelNames.add(ModelName.fromR4(name));
		}
		/*
		 * Mother Maiden name
		 */
		Extension motherMaiden = patient.getExtensionByUrl(MOTHER_MAIDEN_NAME);
		if (motherMaiden != null) {
			localPatient.setMotherMaidenName(motherMaiden.getValue().toString());
		} else {
			localPatient.setMotherMaidenName(null);
		}
		/*
		 * Gender
		 */
		switch (patient.getGender()) {
			case MALE:
				localPatient.setSex(MALE_SEX);
				break;
			case FEMALE:
				localPatient.setSex(FEMALE_SEX);
				break;
			case OTHER:
			default:
				localPatient.setSex("");
				break;
		}
		/*
		 * Races
		 */
		Extension raceExtension = patient.getExtensionByUrl(RACE_EXTENSION);
		if (raceExtension != null) {
			Stream<Extension> raceExtensionsStream = Stream.concat(raceExtension.getExtensionsByUrl(RACE_EXTENSION_OMB).stream(), raceExtension.getExtensionsByUrl(RACE_EXTENSION_DETAILED).stream());
			for (Iterator<Extension> it = raceExtensionsStream.iterator(); it.hasNext(); ) {
				Extension ext = it.next();
				Coding coding = MappingHelper.extensionGetCoding(ext);
				if (coding != null && !localPatient.getRaces().contains(coding.getCode())) {
					localPatient.addRace(StringUtils.defaultString(coding.getCode()));
				}
			}
		}
		/*
		 * Ethnicity
		 */
		Extension ethnicityExtension = patient.getExtensionByUrl(ETHNICITY_EXTENSION);
		if (ethnicityExtension != null) {
			Extension ombExtension = ethnicityExtension.getExtensionByUrl(ETHNICITY_EXTENSION_OMB);
			Extension detailedExtension = ethnicityExtension.getExtensionByUrl(ETHNICITY_EXTENSION_DETAILED);
			if (ombExtension != null) {
				localPatient.setEthnicity(MappingHelper.extensionGetCoding(ombExtension).getCode());
			} else if (detailedExtension != null) {
				localPatient.setEthnicity(MappingHelper.extensionGetCoding(detailedExtension).getCode());
			}
		} else {
			localPatient.setEthnicity(null);
		}
		/*
		 * Phone email
		 */
		for (ContactPoint telecom : patient.getTelecom()) {
			if (null != telecom.getSystem()) {
				if (telecom.getSystem().equals(ContactPoint.ContactPointSystem.PHONE)) {
					localPatient.addPhone(ModelPhone.fromR4(telecom));
				} else if (telecom.getSystem().equals(ContactPoint.ContactPointSystem.EMAIL)) {
					localPatient.setEmail(StringUtils.defaultString(telecom.getValue()));
				}
			}
		}
		/*
		 * Deceased
		 */
		if (null != patient.getDeceased()) {
			if (patient.getDeceased().isBooleanPrimitive()) {
				if (patient.getDeceasedBooleanType().booleanValue()) {
					localPatient.setDeathFlag(YES);
				} else {
					localPatient.setDeathFlag(NO);
				}
			}
			if (patient.getDeceased().isDateTime()) {
				localPatient.setDeathDate(patient.getDeceasedDateTimeType().getValue());
			}
		}
		/*
		 * Addresses
		 */
		for (Address address : patient.getAddress()) {
			localPatient.addAddress(ModelAddress.fromR4(address));
		}
		/*
		 * Multiple birth
		 */
		if (null != patient.getMultipleBirth()) {
			if (patient.getMultipleBirth().isBooleanPrimitive()) {
				if (patient.getMultipleBirthBooleanType().booleanValue()) {
					localPatient.setBirthFlag(YES);
				} else {
					localPatient.setBirthFlag(NO);
				}
			} else {
				localPatient.setBirthOrder(String.valueOf(patient.getMultipleBirthIntegerType()));
			}
		}
		/*
		 * Publicity indicator
		 */
		Extension publicity = patient.getExtensionByUrl(PUBLICITY_EXTENSION);
		if (publicity != null) {
			Coding value = MappingHelper.extensionGetCoding(publicity);
			localPatient.setPublicityIndicator(StringUtils.defaultString(value.getCode()));
			if (StringUtils.isNotBlank(value.getVersion())) {
				try {
					localPatient.setPublicityIndicatorDate(MappingHelper.FHIR_SDF.parse(value.getVersion()));
				} catch (ParseException ignored) {
				}
			}
		} else {
			localPatient.setPublicityIndicator(null);
		}
		/*
		 * Protection
		 */
		Extension protection = patient.getExtensionByUrl(PROTECTION_EXTENSION);
		if (protection != null) {
			Coding value = MappingHelper.extensionGetCoding(protection);
			localPatient.setProtectionIndicator(StringUtils.defaultString(value.getCode()));
			if (StringUtils.isNotBlank(value.getVersion())) {
				try {
					localPatient.setProtectionIndicatorDate(MappingHelper.FHIR_SDF.parse(value.getVersion()));
				} catch (ParseException ignored) {
				}
			}
		} else {
			localPatient.setProtectionIndicator(null);
		}
		/*
		 * Registry status
		 */
		Extension registry = patient.getExtensionByUrl(REGISTRY_STATUS_EXTENSION);
		if (registry != null) {
			Coding value = MappingHelper.extensionGetCoding(registry);
			localPatient.setRegistryStatusIndicator(StringUtils.defaultString(value.getCode()));
			if (StringUtils.isNotBlank(value.getVersion())) {
				try {
					localPatient.setRegistryStatusIndicatorDate(MappingHelper.FHIR_SDF.parse(value.getVersion()));
				} catch (ParseException ignored) {
				}
			}
		} else {
			localPatient.setRegistryStatusIndicator(null);
		}
		/*
		 * Patient Contact / Guardian
		 */
		for (Patient.ContactComponent contactComponent : patient.getContact()) {
			PatientGuardian patientGuardian = new PatientGuardian();
			patientGuardian.setName(ModelName.fromR4(contactComponent.getName()));
			patientGuardian.setGuardianRelationship(contactComponent.getRelationshipFirstRep().getCodingFirstRep().getCode());
			localPatient.addPatientGuardian(patientGuardian);
		}
	}


	public Patient fhirResource(PatientMaster pm) {
		Patient p = new Patient();
		p.setId(pm.getPatientId());
		/*
		 * Updated Date
		 */
		p.getMeta().setLastUpdated(pm.getUpdatedDate());
		/*
		 * Recorded Date
		 */
		if (pm.getReportedDate() != null) {
			p.addExtension()
				.setUrl(RECORDED)
				.setValue(new DateType(pm.getReportedDate()));
		}
		/*
		 * Business Identifiers
		 */
		for (BusinessIdentifier businessIdentifier : pm.getBusinessIdentifiers()) {
			p.addIdentifier(businessIdentifier.toR4());
		}
		/*
		 * Managing Organization
		 */
		if (StringUtils.isNotBlank(pm.getManagingOrganizationId())) {
			p.setManagingOrganization(new Reference(pm.getManagingOrganizationId()));
		}
		/*
		 * Birth Date
		 */
		p.setBirthDate(pm.getBirthDate());
		/*
		 * Names
		 */
		for (ModelName modelName : pm.getPatientNames()) {
			p.addName(modelName.toR4());
		}
		/*
		 * Mother Maiden Name
		 */
		if (pm.getMotherMaidenName() != null) {
			p.addExtension().setUrl(MOTHER_MAIDEN_NAME).setValue(new StringType(pm.getMotherMaidenName()));
		}
		/*
		 * Sex Gender
		 */
		switch (pm.getSex()) {
			case MALE_SEX:
				p.setGender(Enumerations.AdministrativeGender.MALE);
				break;
			case FEMALE_SEX:
				p.setGender(Enumerations.AdministrativeGender.FEMALE);
				break;
			default:
				p.setGender(Enumerations.AdministrativeGender.OTHER);
				break;
		}
		/*
		 * Race
		 */
		if (!pm.getRaces().isEmpty()) {
			Extension raceExtension = p.addExtension();
			raceExtension.setUrl(RACE_EXTENSION);
			StringBuilder raceText = new StringBuilder();
			for (String value : pm.getRaces()) {
				if (StringUtils.isNotBlank(value)) {
					Coding coding = new Coding().setCode(value).setSystem(RACE_SYSTEM);
					Code code = CodeMapManager.getCodeMap().getCodeForCodeset(CodesetType.PATIENT_RACE, value);
					/*
					 * Added to OMB extension if code recognised
					 * TODO make sure this is using the right codeset
					 */
					if (code != null) {
						coding.setDisplay(code.getLabel());
						raceExtension.addExtension(RACE_EXTENSION_OMB, coding);
					} else {
						raceExtension.addExtension(RACE_EXTENSION_DETAILED, coding);
					}
					raceText.append(value).append(" ");
				}
			}
			raceExtension.addExtension(RACE_EXTENSION_TEXT, new StringType(raceText.toString()));
		}
		/*
		 * Ethnicity
		 */
		if (pm.getEthnicity() != null) {
			Extension ethnicityExtension = p.addExtension().setUrl(ETHNICITY_EXTENSION);
			if (StringUtils.isNotBlank(pm.getEthnicity())) {
				Coding coding = new Coding().setCode(pm.getEthnicity()).setSystem(ETHNICITY_SYSTEM);
				Code code = CodeMapManager.getCodeMap().getCodeForCodeset(CodesetType.PATIENT_ETHNICITY, pm.getEthnicity());
				/*
				 * Added to OMB extension if code recognised
				 */
				if (code != null) {
					coding.setDisplay(code.getLabel());
					ethnicityExtension.addExtension(ETHNICITY_EXTENSION_OMB, coding);
				} else {
					ethnicityExtension.addExtension(ETHNICITY_EXTENSION_DETAILED, coding);
				}
				ethnicityExtension.addExtension(ETHNICITY_EXTENSION_TEXT, new StringType(pm.getEthnicity()));
			}
		}
		/*
		 * Phone
		 */
		for (ModelPhone patientPhone : pm.getPhones()) {
			p.addTelecom(patientPhone.toR4());
		}
		/*
		 * Email
		 */
		if (null != pm.getEmail()) {
			p.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue(pm.getEmail());
		}
		/*
		 * Death
		 */
		if (pm.getDeathDate() != null) {
			p.setDeceased(new DateTimeType(pm.getDeathDate()));
		} else if (StringUtils.isNotBlank(pm.getDeathFlag())) {
			if (pm.getDeathFlag().equals(YES)) {
				p.setDeceased(new BooleanType(true));
			} else if (pm.getDeathFlag().equals(NO)) {
				p.setDeceased(new BooleanType(false));
			}
		}
		/*
		 * Addresses
		 */
		for (ModelAddress modelAddress : pm.getAddresses()) {
			p.addAddress(modelAddress.toR4());
		}
		/*
		 * Birth Order
		 */
		if (StringUtils.isNotBlank(pm.getBirthOrder())) {
			p.setMultipleBirth(new IntegerType().setValue(Integer.parseInt(pm.getBirthOrder())));
		} else if (pm.getBirthFlag().equals(YES)) {
			p.setMultipleBirth(new BooleanType(true));
		}
		/*
		 * Publicity
		 * Rule used for indicators: null value means no added extension, blank value means adding extension but no value/coding
		 */
		if (pm.getPublicityIndicator() != null) {
			Extension publicity = p.addExtension();
			publicity.setUrl(PUBLICITY_EXTENSION);
			if (StringUtils.isNotBlank(pm.getPublicityIndicator())) {
				Coding publicityValue = new Coding().setSystem(PUBLICITY_SYSTEM).setCode(pm.getPublicityIndicator());
				publicity.setValue(publicityValue);
				if (pm.getPublicityIndicatorDate() != null) {
					publicityValue.setVersion(MappingHelper.FHIR_SDF.format(pm.getPublicityIndicatorDate()));
				}
			}
		}
		/*
		 * Protection
		 */
		if (pm.getProtectionIndicator() != null) {
			Extension protection = p.addExtension();
			protection.setUrl(PROTECTION_EXTENSION);
			if (StringUtils.isNotBlank(pm.getProtectionIndicator())) {
				Coding protectionValue = new Coding().setSystem(PROTECTION_SYSTEM).setCode(pm.getProtectionIndicator());
				protection.setValue(protectionValue);
				if (pm.getProtectionIndicatorDate() != null) {
					protectionValue.setVersion(MappingHelper.FHIR_SDF.format(pm.getProtectionIndicatorDate()));
				}
			}
		}
		/*
		 * Registry status
		 */
		if (pm.getRegistryStatusIndicator() != null) {
			Extension registryStatus = p.addExtension();
			registryStatus.setUrl(REGISTRY_STATUS_EXTENSION);
			if (StringUtils.isNotBlank(pm.getRegistryStatusIndicator())) {
				Coding registryValue = new Coding().setSystem(REGISTRY_STATUS_INDICATOR).setCode(pm.getRegistryStatusIndicator());
				registryStatus.setValue(registryValue);
				if (pm.getRegistryStatusIndicatorDate() != null) {
					registryValue.setVersion(MappingHelper.FHIR_SDF.format(pm.getRegistryStatusIndicatorDate()));
				}
			}
		}
		/*
		 * Guardian next of kin
		 */
		for (PatientGuardian patientGuardian : pm.getPatientGuardians()) {
			Patient.ContactComponent contact = p.addContact();
			contact.setName(patientGuardian.getName().toR4());
			Coding coding = new Coding().setSystem(RELATIONSHIP_SYSTEM).setCode(patientGuardian.getGuardianRelationship());
			Code code = CodeMapManager.getCodeMap().getCodeForCodeset(CodesetType.PERSON_RELATIONSHIP, patientGuardian.getGuardianRelationship());
			if (code != null) {
				coding.setDisplay(code.getLabel());
				contact.addRelationship()
					.setText(patientGuardian.getGuardianRelationship())
					.addCoding(coding);
			} else {
				contact.addRelationship()
					.setText(patientGuardian.getGuardianRelationship());
			}
		}
		return p;
	}

}