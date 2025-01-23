package org.immregistries.iis.kernal.mapping.forR5;


import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.*;
import org.hl7.fhir.r5.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r5.model.Enumerations.AdministrativeGender;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.InternalClient.FhirRequesterR5;
import org.immregistries.iis.kernal.fhir.annotations.OnR5Condition;
import org.immregistries.iis.kernal.logic.CodeMapManager;
import org.immregistries.iis.kernal.mapping.Interfaces.PatientMapper;
import org.immregistries.iis.kernal.mapping.MappingHelper;
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

import static org.immregistries.iis.kernal.InternalClient.FhirRequester.GOLDEN_RECORD;
import static org.immregistries.iis.kernal.InternalClient.FhirRequester.GOLDEN_SYSTEM_TAG;


@Service
@Conditional(OnR5Condition.class)
public class PatientMapperR5 implements PatientMapper<Patient> {
	Logger logger = LoggerFactory.getLogger(PatientMapperR5.class);

	@Autowired
	FhirRequesterR5 fhirRequests;

	public PatientReported getReportedWithMaster(Patient p) {
		PatientReported patientReported = getReported(p);
		if (!p.getId().isBlank() && p.getMeta().getTag(GOLDEN_SYSTEM_TAG, GOLDEN_RECORD) == null) {
			patientReported.setPatient(fhirRequests.readPatientMasterWithMdmLink(p.getId()));
		}
		return patientReported;
	}

	public PatientReported getReported(Patient patient) {
		PatientReported patientReported = new PatientReported();
		fillFromFhirResource(patientReported, patient);
		return patientReported;
	}

	public PatientMaster getMaster(Patient patient) {
		return getReported(patient);
	}

	public void fillFromFhirResource(PatientMaster pm, Patient p) {
		if (StringUtils.isNotBlank(p.getId())) {
			pm.setPatientId(new IdType(p.getId()).getIdPart());
		}
		pm.setUpdatedDate(p.getMeta().getLastUpdated());
		for (Identifier identifier : p.getIdentifier()) {
			pm.addPatientIdentifier(PatientIdentifier.fromR5(identifier));
		}
		pm.setBirthDate(p.getBirthDate());
		pm.setManagingOrganizationId(StringUtils.defaultString(p.getManagingOrganization().getReference()));
		/*
		Name
		 */
		List<PatientName> patientNames = new ArrayList<>(p.getName().size());
		pm.setPatientNames(patientNames);
		for (HumanName name : p.getName()) {
			patientNames.add(PatientName.fromR5(name));
		}

		if (p.hasExtension(MOTHER_MAIDEN_NAME)) {
			Extension motherMaiden = p.getExtensionByUrl(MOTHER_MAIDEN_NAME);
			pm.setMotherMaidenName(motherMaiden.getValue().toString());
		}
		switch (p.getGender()) {
			case MALE:
				pm.setSex(MALE_SEX);
				break;
			case FEMALE:
				pm.setSex(FEMALE_SEX);
				break;
			case OTHER:
			default:
				pm.setSex("");
				break;
		}

		Extension raceExtension = p.getExtensionByUrl(RACE_EXTENSION);
		if (raceExtension != null) {
			for (Iterator<Extension> it = Stream.concat(raceExtension.getExtensionsByUrl(RACE_EXTENSION_OMB).stream(), raceExtension.getExtensionsByUrl(RACE_EXTENSION_DETAILED).stream()).iterator(); it.hasNext(); ) {
				Extension ext = it.next();
				Coding coding = MappingHelper.extensionGetCoding(ext);
				if (!pm.getRaces().contains(coding.getCode())) {
					pm.addRace(StringUtils.defaultString(coding.getCode()));
				}
			}
		}
		Extension ethnicityExtension = p.getExtensionByUrl(ETHNICITY_EXTENSION);
		if (ethnicityExtension != null) {
			Extension ombExtension = ethnicityExtension.getExtensionByUrl(ETHNICITY_EXTENSION_OMB);
			Extension detailedExtension = ethnicityExtension.getExtensionByUrl(ETHNICITY_EXTENSION_DETAILED);
			if (ombExtension != null) {
				pm.setEthnicity(MappingHelper.extensionGetCoding(ombExtension).getCode());
			} else if (detailedExtension != null) {
				pm.setEthnicity(MappingHelper.extensionGetCoding(detailedExtension).getCode());
			}
		}


		for (ContactPoint telecom : p.getTelecom()) {
			if (null != telecom.getSystem()) {
				if (telecom.getSystem().equals(ContactPointSystem.PHONE)) {
					pm.addPhone(PatientPhone.fromR5(telecom));
				} else if (telecom.getSystem().equals(ContactPointSystem.EMAIL)) {
					pm.setEmail(StringUtils.defaultString(telecom.getValue(), ""));
				}
			}
		}

		if (null != p.getDeceased()) {
			if (p.getDeceased().isBooleanPrimitive()) {
				if (p.getDeceasedBooleanType().booleanValue()) {
					pm.setDeathFlag(YES);
				} else {
					pm.setDeathFlag(NO);
				}
			}
			if (p.getDeceased().isDateTime()) {
				pm.setDeathDate(p.getDeceasedDateTimeType().getValue());
			}
		}
		/*
		Addresses
		 */
		for (Address address : p.getAddress()) {
			pm.addAddress(PatientAddress.fromR5(address));
		}

		if (null != p.getMultipleBirth()) {
			if (p.getMultipleBirth().isBooleanPrimitive()) {
				if (p.getMultipleBirthBooleanType().booleanValue()) {
					pm.setBirthFlag(YES);
				} else {
					pm.setBirthFlag(NO);
				}
			} else {
				pm.setBirthOrder(String.valueOf(p.getMultipleBirthIntegerType()));
			}
		}

		if (p.hasExtension(PUBLICITY_EXTENSION)) {
			Extension publicity = p.getExtensionByUrl(PUBLICITY_EXTENSION);
			Coding value = MappingHelper.extensionGetCoding(publicity);
			pm.setPublicityIndicator(value.getCode());
			if (StringUtils.isNotBlank(value.getVersion())) {
				try {
					pm.setPublicityIndicatorDate(MappingHelper.sdf.parse(value.getVersion()));
				} catch (ParseException ignored) {
				}
			}
		}
		if (p.hasExtension(PROTECTION_EXTENSION)) {
			Extension protection = p.getExtensionByUrl(PROTECTION_EXTENSION);
			Coding value = MappingHelper.extensionGetCoding(protection);
			pm.setProtectionIndicator(value.getCode());
			if (StringUtils.isNotBlank(value.getVersion())) {
				try {
					pm.setProtectionIndicatorDate(MappingHelper.sdf.parse(value.getVersion()));
				} catch (ParseException ignored) {
				}
			}
		}
		if (p.hasExtension(REGISTRY_STATUS_EXTENSION)) {
			Extension registry = p.getExtensionByUrl(REGISTRY_STATUS_EXTENSION);
			Coding value = MappingHelper.extensionGetCoding(registry);
			pm.setRegistryStatusIndicator(value.getCode());
			if (StringUtils.isNotBlank(value.getVersion())) {
				try {
					pm.setRegistryStatusIndicatorDate(MappingHelper.sdf.parse(value.getVersion()));
				} catch (ParseException ignored) {
				}
			}
		}

		/*
		Patient Contact / Guardian
		 */
		for (Patient.ContactComponent contactComponent : p.getContact()) {
			PatientGuardian patientGuardian = new PatientGuardian();
			patientGuardian.setName(PatientName.fromR5(contactComponent.getName()));
			patientGuardian.setGuardianRelationship(contactComponent.getRelationshipFirstRep().getCodingFirstRep().getCode());
			pm.addPatientGuardian(patientGuardian);
		}
	}

	public Patient getFhirResource(PatientMaster pm) {
		Patient p = new Patient();

		p.getMeta().setLastUpdated(pm.getUpdatedDate());
		p.setId(pm.getPatientId());
		for (PatientIdentifier patientIdentifier : pm.getPatientIdentifiers()) {
			p.addIdentifier(patientIdentifier.toR5());
		}
		p.setManagingOrganization(new Reference(pm.getManagingOrganizationId()));
		p.setBirthDate(pm.getBirthDate());
		for (PatientName patientName : pm.getPatientNames()) {
			p.addName(patientName.toR5());
		}

		Extension motherMaidenName = p.addExtension().setUrl(MOTHER_MAIDEN_NAME).setValue(new StringType(pm.getMotherMaidenName()));

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

		/*
		 * Ethnicity
		 */
		Extension ethnicityExtension = p.addExtension().setUrl(ETHNICITY_EXTENSION);
		if (StringUtils.isNotBlank(pm.getEthnicity())) {
			Coding coding = new Coding().setCode(pm.getEthnicity()).setSystem(RACE_SYSTEM);
			Code code = CodeMapManager.getCodeMap().getCodeForCodeset(CodesetType.PATIENT_ETHNICITY, pm.getEthnicity());
			if (code != null) {
				coding.setDisplay(code.getLabel());
				ethnicityExtension.addExtension(ETHNICITY_EXTENSION_OMB, coding);
			} else {
				ethnicityExtension.addExtension(ETHNICITY_EXTENSION_DETAILED, coding);
			}
			ethnicityExtension.addExtension(ETHNICITY_EXTENSION_TEXT, new StringType(pm.getEthnicity()));
		}
		/*
		Phone
		 */
		for (PatientPhone patientPhone : pm.getPhones()) {
			p.addTelecom(patientPhone.toR5());
		}
		/*
		Email
		 */
		if (null != pm.getEmail()) {
			p.addTelecom().setSystem(ContactPointSystem.EMAIL).setValue(pm.getEmail());
		}


		if (pm.getDeathDate() != null) {
			p.setDeceased(new DateTimeType(pm.getDeathDate()));
		} else if (pm.getDeathFlag().equals(YES)) {
			p.setDeceased(new BooleanType(true));
		} else if (pm.getDeathFlag().equals(NO)) {
			p.setDeceased(new BooleanType(false));
		}

		for (PatientAddress patientAddress : pm.getAddresses()) {
			p.addAddress(patientAddress.toR5());
		}

		if (StringUtils.isNotBlank(pm.getBirthOrder())) {
			p.setMultipleBirth(new IntegerType().setValue(Integer.parseInt(pm.getBirthOrder())));
		} else if (pm.getBirthFlag().equals(YES)) {
			p.setMultipleBirth(new BooleanType(true));
		}

		Extension publicity = p.addExtension();
		publicity.setUrl(PUBLICITY_EXTENSION);
		Coding publicityValue = new Coding().setSystem(PUBLICITY_SYSTEM).setCode(pm.getPublicityIndicator());
		publicity.setValue(publicityValue);
		if (pm.getPublicityIndicatorDate() != null) {
			publicityValue.setVersion(MappingHelper.sdf.format(pm.getPublicityIndicatorDate()));
		}

		Extension protection = p.addExtension();
		protection.setUrl(PROTECTION_EXTENSION);
		Coding protectionValue = new Coding().setSystem(PROTECTION_SYSTEM).setCode(pm.getProtectionIndicator());
		protection.setValue(protectionValue);
		if (pm.getProtectionIndicatorDate() != null) {
			protectionValue.setVersion(MappingHelper.sdf.format(pm.getProtectionIndicatorDate()));
		}

		Extension registryStatus = p.addExtension();
		registryStatus.setUrl(REGISTRY_STATUS_EXTENSION);
		Coding registryValue = new Coding().setSystem(REGISTRY_STATUS_INDICATOR).setCode(pm.getRegistryStatusIndicator());
		registryStatus.setValue(registryValue);
		if (pm.getRegistryStatusIndicatorDate() != null) {
			registryValue.setVersion(MappingHelper.sdf.format(pm.getRegistryStatusIndicatorDate()));
		}

		for (PatientGuardian patientGuardian : pm.getPatientGuardians()) {
			Patient.ContactComponent contact = p.addContact();
			HumanName contactName = new HumanName();
			contact.setName(contactName);
			contact.addRelationship().setText(patientGuardian.getGuardianRelationship()).addCoding().setCode(patientGuardian.getGuardianRelationship());
			contactName.setFamily(patientGuardian.getName().getNameLast());
			contactName.addGivenElement().setValue(patientGuardian.getName().getNameFirst());
			contactName.addGivenElement().setValue(patientGuardian.getName().getNameMiddle());
		}
		return p;
	}


}