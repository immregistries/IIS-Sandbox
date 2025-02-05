package org.immregistries.iis.kernal.mapping.forR5;


import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.*;
import org.hl7.fhir.r5.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r5.model.Enumerations.AdministrativeGender;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.immregistries.iis.kernal.logic.CodeMapManager;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.mapping.interfaces.PatientMapper;
import org.immregistries.iis.kernal.mapping.internalClient.FhirRequesterR5;
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

import static org.immregistries.iis.kernal.mapping.internalClient.FhirRequester.GOLDEN_RECORD;
import static org.immregistries.iis.kernal.mapping.internalClient.FhirRequester.GOLDEN_SYSTEM_TAG;


@Service
@Conditional(OnR5Condition.class)
public class PatientMapperR5 implements PatientMapper<Patient> {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private FhirRequesterR5 fhirRequests;

	public PatientReported localObjectReportedWithMaster(Patient p) {
		PatientReported patientReported = localObjectReported(p);
		if (!p.getId().isBlank() && p.getMeta().getTag(GOLDEN_SYSTEM_TAG, GOLDEN_RECORD) == null) {
			patientReported.setPatientMaster(fhirRequests.readPatientMasterWithMdmLink(p.getId()));
		}
		return patientReported;
	}

	public PatientMaster localObject(Patient patient) {
		PatientMaster patientMaster = new PatientMaster();
//		if (FhirRequester.isGoldenRecord(patient)) {
//			logger.info("Mapping refused for report as patient is golden");
//			return null;
//		}
		fillFromFhirResource(patientMaster, patient);
		return localObjectReported(patient);
	}

	public PatientReported localObjectReported(Patient patient) {
		PatientReported patientReported = new PatientReported();
//		if (!FhirRequester.isGoldenRecord(patient)) {
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
		 * Identifiers
		 */
		for (Identifier identifier : patient.getIdentifier()) {
			localPatient.addBusinessIdentifier(BusinessIdentifier.fromR5(identifier));
		}
		/*
		 * Birth Date
		 */
		localPatient.setBirthDate(patient.getBirthDate());
		/*
		 * Managing organization
		 */
		localPatient.setManagingOrganizationId(StringUtils.defaultString(patient.getManagingOrganization().getReference()));
		/*
		 * Names
		 */
		List<PatientName> patientNames = new ArrayList<>(patient.getName().size());
		localPatient.setPatientNames(patientNames);
		for (HumanName name : patient.getName()) {
			patientNames.add(PatientName.fromR5(name));
		}
		/*
		 * Mother Maiden name
		 */
		if (patient.hasExtension(MOTHER_MAIDEN_NAME)) {
			Extension motherMaiden = patient.getExtensionByUrl(MOTHER_MAIDEN_NAME);
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
			for (Iterator<Extension> it = Stream.concat(raceExtension.getExtensionsByUrl(RACE_EXTENSION_OMB).stream(), raceExtension.getExtensionsByUrl(RACE_EXTENSION_DETAILED).stream()).iterator(); it.hasNext(); ) {
				Extension ext = it.next();
				Coding coding = MappingHelper.extensionGetCoding(ext);
				if (!localPatient.getRaces().contains(coding.getCode())) {
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
				if (telecom.getSystem().equals(ContactPointSystem.PHONE)) {
					localPatient.addPhone(PatientPhone.fromR5(telecom));
				} else if (telecom.getSystem().equals(ContactPointSystem.EMAIL)) {
					localPatient.setEmail(StringUtils.defaultString(telecom.getValue(), ""));
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
			localPatient.addAddress(PatientAddress.fromR5(address));
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
		if (patient.hasExtension(PUBLICITY_EXTENSION)) {
			Extension publicity = patient.getExtensionByUrl(PUBLICITY_EXTENSION);
			Coding value = MappingHelper.extensionGetCoding(publicity);
			localPatient.setPublicityIndicator(value.getCode());
			if (StringUtils.isNotBlank(value.getVersion())) {
				try {
					localPatient.setPublicityIndicatorDate(MappingHelper.sdf.parse(value.getVersion()));
				} catch (ParseException ignored) {
				}
			}
		} else {
			localPatient.setPublicityIndicator(null);
		}
		/*
		 * Protection
		 */
		if (patient.hasExtension(PROTECTION_EXTENSION)) {
			Extension protection = patient.getExtensionByUrl(PROTECTION_EXTENSION);
			Coding value = MappingHelper.extensionGetCoding(protection);
			localPatient.setProtectionIndicator(value.getCode());
			if (StringUtils.isNotBlank(value.getVersion())) {
				try {
					localPatient.setProtectionIndicatorDate(MappingHelper.sdf.parse(value.getVersion()));
				} catch (ParseException ignored) {
				}
			}
		} else {
			localPatient.setProtectionIndicator(null);
		}
		/*
		 * Registry status
		 */
		if (patient.hasExtension(REGISTRY_STATUS_EXTENSION)) {
			Extension registry = patient.getExtensionByUrl(REGISTRY_STATUS_EXTENSION);
			Coding value = MappingHelper.extensionGetCoding(registry);
			localPatient.setRegistryStatusIndicator(value.getCode());
			if (StringUtils.isNotBlank(value.getVersion())) {
				try {
					localPatient.setRegistryStatusIndicatorDate(MappingHelper.sdf.parse(value.getVersion()));
				} catch (ParseException ignored) {
				}
			}
		} else {
			localPatient.setRegistryStatusIndicator(null);
		}

		/*
		Patient Contact / Guardian
		 */
		for (Patient.ContactComponent contactComponent : patient.getContact()) {
			PatientGuardian patientGuardian = new PatientGuardian();
			patientGuardian.setName(PatientName.fromR5(contactComponent.getName()));
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
		 * Business Identifiers
		 */
		for (BusinessIdentifier businessIdentifier : pm.getBusinessIdentifiers()) {
			p.addIdentifier(businessIdentifier.toR5());
		}
		/*
		 * Managing Organization
		 */
		p.setManagingOrganization(new Reference(pm.getManagingOrganizationId()));
		/*
		 * Birth Date
		 */
		p.setBirthDate(pm.getBirthDate());
		/*
		 * Names
		 */
		for (PatientName patientName : pm.getPatientNames()) {
			p.addName(patientName.toR5());
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
				p.setGender(AdministrativeGender.MALE);
				break;
			case FEMALE_SEX:
				p.setGender(AdministrativeGender.FEMALE);
				break;
			default:
				p.setGender(AdministrativeGender.OTHER);
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
		for (PatientPhone patientPhone : pm.getPhones()) {
			p.addTelecom(patientPhone.toR5());
		}
		/*
		 * Email
		 */
		if (null != pm.getEmail()) {
			p.addTelecom().setSystem(ContactPointSystem.EMAIL).setValue(pm.getEmail());
		}
		/*
		 * Death
		 */
		if (pm.getDeathDate() != null) {
			p.setDeceased(new DateTimeType(pm.getDeathDate()));
		} else if (pm.getDeathFlag().equals(YES)) {
			p.setDeceased(new BooleanType(true));
		} else if (pm.getDeathFlag().equals(NO)) {
			p.setDeceased(new BooleanType(false));
		}
		/*
		 * Addresses
		 */
		for (PatientAddress patientAddress : pm.getAddresses()) {
			p.addAddress(patientAddress.toR5());
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
		 */
		if (pm.getPublicityIndicator() != null) {
			Extension publicity = p.addExtension();
			publicity.setUrl(PUBLICITY_EXTENSION);
			Coding publicityValue = new Coding().setSystem(PUBLICITY_SYSTEM).setCode(pm.getPublicityIndicator());
			publicity.setValue(publicityValue);
			if (pm.getPublicityIndicatorDate() != null) {
				publicityValue.setVersion(MappingHelper.sdf.format(pm.getPublicityIndicatorDate()));
			}
		}
		/*
		 * Protection
		 */
		if (pm.getProtectionIndicator() != null) {
			Extension protection = p.addExtension();
			protection.setUrl(PROTECTION_EXTENSION);
			Coding protectionValue = new Coding().setSystem(PROTECTION_SYSTEM).setCode(pm.getProtectionIndicator());
			protection.setValue(protectionValue);
			if (pm.getProtectionIndicatorDate() != null) {
				protectionValue.setVersion(MappingHelper.sdf.format(pm.getProtectionIndicatorDate()));
			}
		}
		/*
		 * Registry status
		 */
		if (pm.getRegistryStatusIndicator() != null) {
			Extension registryStatus = p.addExtension();
			registryStatus.setUrl(REGISTRY_STATUS_EXTENSION);
			Coding registryValue = new Coding().setSystem(REGISTRY_STATUS_INDICATOR).setCode(pm.getRegistryStatusIndicator());
			registryStatus.setValue(registryValue);
			if (pm.getRegistryStatusIndicatorDate() != null) {
				registryValue.setVersion(MappingHelper.sdf.format(pm.getRegistryStatusIndicatorDate()));
			}
		}
		/*
		 * Guardian next of kin
		 */
		for (PatientGuardian patientGuardian : pm.getPatientGuardians()) {
			Patient.ContactComponent contact = p.addContact();
			contact.setName(patientGuardian.getName().toR5());
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