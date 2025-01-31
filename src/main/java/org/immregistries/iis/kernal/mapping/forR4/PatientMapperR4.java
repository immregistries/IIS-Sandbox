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
@Conditional(OnR4Condition.class)
public class PatientMapperR4 implements PatientMapper<Patient> {

	@Autowired
	private FhirRequesterR4 fhirRequests;

	public PatientReported localObjectReportedWithMaster(Patient p) {
		PatientReported patientReported = localObjectReported(p);
		if (!p.getId().isBlank() && p.getMeta().getTag(GOLDEN_SYSTEM_TAG, GOLDEN_RECORD) == null) {
			patientReported.setPatient(fhirRequests.readPatientMasterWithMdmLink(p.getId()));
		}
		return patientReported;
	}

	public PatientMaster localObject(Patient patient) {
		return localObjectReported(patient);
	}

	public PatientReported localObjectReported(Patient patient) {
		PatientReported patientReported = new PatientReported();
		if (StringUtils.isNotBlank(patient.getId())) {
			patientReported.setPatientId(new IdType(patient.getId()).getIdPart());
		}
		/*
		 * Updated date
		 */
		patientReported.setUpdatedDate(patient.getMeta().getLastUpdated());
		/*
		 * Identifiers
		 */
		for (Identifier identifier : patient.getIdentifier()) {
			patientReported.addBusinessIdentifier(BusinessIdentifier.fromR4(identifier));
		}
		/*
		 * Birth Date
		 */
		patientReported.setBirthDate(patient.getBirthDate());
		/*
		 * Managing organization
		 */
		patientReported.setManagingOrganizationId(StringUtils.defaultString(patient.getManagingOrganization().getReference()));
		/*
		 * Names
		 */
		List<PatientName> patientNames = new ArrayList<>(patient.getName().size());
		patientReported.setPatientNames(patientNames);
		for (HumanName name : patient.getName()) {
			patientNames.add(PatientName.fromR4(name));
		}
		/*
		 * Mother Maiden name
		 */
		Extension motherMaiden = patient.getExtensionByUrl(MOTHER_MAIDEN_NAME);
		if (motherMaiden != null) {
			patientReported.setMotherMaidenName(motherMaiden.getValue().toString());
		} else {
			patientReported.setMotherMaidenName(null);
		}
		/*
		 * Gender
		 */
		switch (patient.getGender()) {
			case MALE:
				patientReported.setSex(MALE_SEX);
				break;
			case FEMALE:
				patientReported.setSex(FEMALE_SEX);
				break;
			case OTHER:
			default:
				patientReported.setSex("");
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
				if (!patientReported.getRaces().contains(coding.getCode())) {
					patientReported.addRace(StringUtils.defaultString(coding.getCode()));
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
				patientReported.setEthnicity(MappingHelper.extensionGetCoding(ombExtension).getCode());
			} else if (detailedExtension != null) {
				patientReported.setEthnicity(MappingHelper.extensionGetCoding(detailedExtension).getCode());
			}
		} else {
			patientReported.setEthnicity(null);
		}
		/*
		 * Phone email
		 */
		for (ContactPoint telecom : patient.getTelecom()) {
			if (null != telecom.getSystem()) {
				if (telecom.getSystem().equals(ContactPoint.ContactPointSystem.PHONE)) {
					patientReported.addPhone(PatientPhone.fromR4(telecom));
				} else if (telecom.getSystem().equals(ContactPoint.ContactPointSystem.EMAIL)) {
					patientReported.setEmail(StringUtils.defaultString(telecom.getValue()));
				}
			}
		}
		/*
		 * Deceased
		 */
		if (null != patient.getDeceased()) {
			if (patient.getDeceased().isBooleanPrimitive()) {
				if (patient.getDeceasedBooleanType().booleanValue()) {
					patientReported.setDeathFlag(YES);
				} else {
					patientReported.setDeathFlag(NO);
				}
			}
			if (patient.getDeceased().isDateTime()) {
				patientReported.setDeathDate(patient.getDeceasedDateTimeType().getValue());
			}
		}
		/*
		 * Addresses
		 */
		for (Address address : patient.getAddress()) {
			patientReported.addAddress(PatientAddress.fromR4(address));
		}
		/*
		 * Multiple birth
		 */
		if (null != patient.getMultipleBirth()) {
			if (patient.getMultipleBirth().isBooleanPrimitive()) {
				if (patient.getMultipleBirthBooleanType().booleanValue()) {
					patientReported.setBirthFlag(YES);
				} else {
					patientReported.setBirthFlag(NO);
				}
			} else {
				patientReported.setBirthOrder(String.valueOf(patient.getMultipleBirthIntegerType()));
			}
		}
		/*
		 * Publicity indicator
		 */
		Extension publicity = patient.getExtensionByUrl(PUBLICITY_EXTENSION);
		if (publicity != null) {
			Coding value = MappingHelper.extensionGetCoding(publicity);
			patientReported.setPublicityIndicator(StringUtils.defaultString(value.getCode()));
			if (StringUtils.isNotBlank(value.getVersion())) {
				try {
					patientReported.setPublicityIndicatorDate(MappingHelper.sdf.parse(value.getVersion()));
				} catch (ParseException ignored) {
				}
			}
		} else {
			patientReported.setPublicityIndicator(null);
		}
		/*
		 * Protection
		 */
		Extension protection = patient.getExtensionByUrl(PROTECTION_EXTENSION);
		if (protection != null) {
			Coding value = MappingHelper.extensionGetCoding(protection);
			patientReported.setProtectionIndicator(StringUtils.defaultString(value.getCode()));
			if (StringUtils.isNotBlank(value.getVersion())) {
				try {
					patientReported.setProtectionIndicatorDate(MappingHelper.sdf.parse(value.getVersion()));
				} catch (ParseException ignored) {
				}
			}
		} else {
			patientReported.setProtectionIndicator(null);
		}
		/*
		 * Registry status
		 */
		Extension registry = patient.getExtensionByUrl(REGISTRY_STATUS_EXTENSION);
		if (registry != null) {
			Coding value = MappingHelper.extensionGetCoding(registry);
			patientReported.setRegistryStatusIndicator(StringUtils.defaultString(value.getCode()));
			if (StringUtils.isNotBlank(value.getVersion())) {
				try {
					patientReported.setRegistryStatusIndicatorDate(MappingHelper.sdf.parse(value.getVersion()));
				} catch (ParseException ignored) {
				}
			}
		} else {
			patientReported.setRegistryStatusIndicator(null);
		}
		/*
		 * Patient Contact / Guardian
		 */
		for (Patient.ContactComponent contactComponent : patient.getContact()) {
			PatientGuardian patientGuardian = new PatientGuardian();
			patientGuardian.setName(PatientName.fromR4(contactComponent.getName()));
			patientGuardian.setGuardianRelationship(contactComponent.getRelationshipFirstRep().getCodingFirstRep().getCode());
			patientReported.addPatientGuardian(patientGuardian);
		}
		return patientReported;
	}


	public Patient fhirResource(PatientMaster pm) {
		Patient p = new Patient();
		/*
		 * Updated Date
		 */
		p.getMeta().setLastUpdated(pm.getUpdatedDate());
		/*
		 * Business Identifiers
		 */
		for (BusinessIdentifier businessIdentifier : pm.getBusinessIdentifiers()) {
			p.addIdentifier(businessIdentifier.toR4());
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
			p.addName(patientName.toR4());
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
		for (PatientPhone patientPhone : pm.getPhones()) {
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
		for (PatientAddress patientAddress : pm.getAddresses()) {
			p.addAddress(patientAddress.toR4());
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