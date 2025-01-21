package org.immregistries.iis.kernal.mapping.forR4;


import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.InternalClient.FhirRequesterR4;
import org.immregistries.iis.kernal.fhir.annotations.OnR4Condition;
import org.immregistries.iis.kernal.logic.CodeMapManager;
import org.immregistries.iis.kernal.mapping.Interfaces.PatientMapper;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.model.*;
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
@Conditional(OnR4Condition.class)
public class PatientMapperR4 implements PatientMapper<Patient> {

	@Autowired
	FhirRequesterR4 fhirRequests;
	@Autowired
	RelatedPersonMapperR4 relatedPersonMapperR4;

	public PatientReported getReportedWithMaster(Patient p) {
		PatientReported patientReported = getReported(p);
		if (!p.getId().isBlank() && p.getMeta().getTag(GOLDEN_SYSTEM_TAG,GOLDEN_RECORD) == null) {
			patientReported.setPatient(fhirRequests.readPatientMasterWithMdmLink(p.getId()));
		}
		return patientReported;
	}

	public void fillFromFhirResource(PatientMaster pm, Patient p) {
		pm.setPatientId(new IdType(p.getId()).getIdPart());
//		pm.setExternalLink(p.getIdentifierFirstRep().getValue());
		for (Identifier identifier : p.getIdentifier()) {
			pm.addPatientIdentifier(PatientIdentifier.fromR4(identifier));
		}
		pm.setUpdatedDate(p.getMeta().getLastUpdated());

//		pm.setPatientReportedAuthority(p.getIdentifierFirstRep().getSystem());
		pm.setBirthDate(p.getBirthDate());
		pm.setManagingOrganizationId(p.getManagingOrganization().getId());
		// Name
		List<PatientName> patientNames = new ArrayList<>(p.getName().size());
		pm.setPatientNames(patientNames);
		for (HumanName name : p.getName()) {
			patientNames.add(new PatientName(name));
		}

		Extension motherMaiden = p.getExtensionByUrl(MOTHER_MAIDEN_NAME);
		if (motherMaiden != null) {
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

		if (p.hasExtension(RACE_EXTENSION)) {
			for (Iterator<Extension> it = Stream.concat(p.getExtensionsByUrl(RACE_EXTENSION_OMB).stream(), p.getExtensionsByUrl(RACE_EXTENSION_DETAILED).stream()).iterator(); it.hasNext(); ) {
				Extension ext = it.next();
				Coding coding = MappingHelper.extensionGetCoding(ext);
				pm.addRace(coding.getCode());
			}
		}
		Extension ethnicityExtension = p.getExtensionByUrl(ETHNICITY_EXTENSION);
		if (ethnicityExtension != null) {
			Extension ombExtention = p.getExtensionByUrl(ETHNICITY_EXTENSION_OMB);
			Extension detailedExtention = p.getExtensionByUrl(ETHNICITY_EXTENSION_DETAILED);
			if (ombExtention != null) {
				pm.setEthnicity(MappingHelper.extensionGetCoding(ombExtention).getCode());
			} else if (detailedExtention != null) {
				pm.setEthnicity(MappingHelper.extensionGetCoding(detailedExtention).getCode());
			}
		}

		for (ContactPoint telecom : p.getTelecom()) {
			if (null != telecom.getSystem()) {
				if (telecom.getSystem().equals(ContactPoint.ContactPointSystem.PHONE)) {
					pm.addPhone(PatientPhone.fromR4(telecom));
				} else if (telecom.getSystem().equals(ContactPoint.ContactPointSystem.EMAIL)) {
					pm.setEmail(telecom.getValue());
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
		// Address
		for (Address address : p.getAddress()) {
			pm.addAddress(PatientAddress.fromR4(address));
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

		Extension publicity = p.getExtensionByUrl(PUBLICITY_EXTENSION);
		if (publicity != null) {
			Coding value = MappingHelper.extensionGetCoding(publicity);
			pm.setPublicityIndicator(value.getCode());
			if (StringUtils.isNotBlank(value.getVersion())) {
				try {
					pm.setPublicityIndicatorDate(MappingHelper.sdf.parse(value.getVersion()));
				} catch (ParseException e) {
//					throw new RuntimeException(e);
				}
			}
		}
		Extension protection = p.getExtensionByUrl(PROTECTION_EXTENSION);
		if (protection != null) {
			Coding value = MappingHelper.extensionGetCoding(protection);
			pm.setProtectionIndicator(value.getCode());
			if (StringUtils.isNotBlank(value.getVersion())) {
				try {
					pm.setProtectionIndicatorDate(MappingHelper.sdf.parse(value.getVersion()));
				} catch (ParseException e) {
//					throw new RuntimeException(e);
				}
			}
		}
		Extension registry = p.getExtensionByUrl(REGISTRY_STATUS_EXTENSION);
		if (registry != null) {
			Coding value = MappingHelper.extensionGetCoding(registry);
			pm.setRegistryStatusIndicator(value.getCode());
			if (StringUtils.isNotBlank(value.getVersion())) {
				try {
					pm.setRegistryStatusIndicatorDate(MappingHelper.sdf.parse(value.getVersion()));
				} catch (ParseException e) {
//				throw new RuntimeException(e);
				}
			}
		}

		// pm.setRegistryStatusIndicator(p.getActive());
		// Patient Contact / Guardian
//		RelatedPerson relatedPerson = fhirRequests.searchRelatedPerson(
//			new SearchParameterMap(RelatedPerson.SP_PATIENT,new ReferenceParam(pm.getPatientId()))
//			.add(RelatedPerson.SP_PATIENT,new ReferenceParam(pm.getExternalLink())));
//		if (relatedPerson != null) {
//			relatedPersonMapperR4.fillGuardianInformation(pm, relatedPerson);
//		}
		for (Patient.ContactComponent contactComponent : p.getContact()) {
			PatientGuardian patientGuardian = new PatientGuardian();
			patientGuardian.setName(new PatientName(contactComponent.getName()));
			patientGuardian.setGuardianRelationship(contactComponent.getRelationshipFirstRep().getText());
		}
	}

	public PatientReported getReported(Patient patient) {
		PatientReported patientReported = new PatientReported();
		fillFromFhirResource(patientReported,patient);
		return patientReported;
	}
	public PatientMaster getMaster(Patient patient) {
		PatientMaster patientMaster = new PatientMaster();
		fillFromFhirResource(patientMaster,patient);
		return patientMaster;
	}

	public Patient getFhirResource(PatientMaster pm) {
		Patient p = new Patient();

//		p.addIdentifier(new Identifier()
//			.setSystem(pm.getPatientReportedAuthority())
//			.setValue(pm.getExternalLink())
//			.setType(
//				new CodeableConcept(new Coding()
//					.setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
//					.setCode(pm.getPatientReportedType()))));
		for (PatientIdentifier patientIdentifier : pm.getPatientIdentifiers()) {
			p.addIdentifier(patientIdentifier.toR4());
		}
		p.setManagingOrganization(new Reference(pm.getManagingOrganizationId()));
		p.setBirthDate(pm.getBirthDate());
		for (PatientName patientName : pm.getPatientNames()) {
			p.addName(patientName.toR4());
		}

		Extension motherMaidenName = p.addExtension()
			.setUrl(MOTHER_MAIDEN_NAME)
			.setValue(new StringType(pm.getMotherMaidenName()));

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

		//Race and ethnicity
		Extension raceExtension = p.addExtension();
		raceExtension.setUrl(RACE_EXTENSION);
		CodeableConcept races = new CodeableConcept().setText(RACE_SYSTEM);
		raceExtension.setValue(races);
		for (String value : pm.getRaces()) {
			if (StringUtils.isNotBlank(value)) {
				Coding coding = races.addCoding().setCode(value).setSystem(RACE_SYSTEM);
				Code code = CodeMapManager.getCodeMap().getCodeForCodeset(CodesetType.PATIENT_RACE, value);
				if (code != null) {
					coding.setDisplay(code.getLabel());
				}
			}
		}
		p.addExtension(ETHNICITY_EXTENSION, new Coding().setSystem(ETHNICITY_SYSTEM).setCode(pm.getEthnicity()));
		// telecom
		for (PatientPhone patientPhone : pm.getPhones()) {
			p.addTelecom(patientPhone.toR4());
		}
		if (null != pm.getEmail()) {
			p.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL)
				.setValue(pm.getEmail());
		}


		if (pm.getDeathDate() != null) {
			p.setDeceased(new DateType(pm.getDeathDate()));
		} else if (pm.getDeathFlag().equals(YES)) {
			p.setDeceased(new BooleanType(true));
		} else if (pm.getDeathFlag().equals(NO)) {
			p.setDeceased(new BooleanType(false));
		}

		for (PatientAddress patientAddress : pm.getAddresses()) {
			p.addAddress(patientAddress.toR4());
		}

		if (StringUtils.isNotBlank(pm.getBirthOrder())) {
			p.setMultipleBirth(new IntegerType().setValue(Integer.parseInt(pm.getBirthOrder())));
		} else if (pm.getBirthFlag().equals(YES)) {
			p.setMultipleBirth(new BooleanType(true));
		}

		Extension publicity = p.addExtension();
		publicity.setUrl(PUBLICITY_EXTENSION);
		Coding publicityValue = new Coding()
			.setSystem(PUBLICITY_SYSTEM)
			.setCode(pm.getPublicityIndicator());
		publicity.setValue(publicityValue);
		if (pm.getPublicityIndicatorDate() != null) {
			publicityValue.setVersion(pm.getPublicityIndicatorDate().toString());
		}

		Extension protection = p.addExtension();
		protection.setUrl(PROTECTION_EXTENSION);
		Coding protectionValue = new Coding()
			.setSystem(PROTECTION_SYSTEM)
			.setCode(pm.getProtectionIndicator());
		protection.setValue(protectionValue);
		if (pm.getProtectionIndicatorDate() != null) {
			protectionValue.setVersion(pm.getProtectionIndicatorDate().toString());
		}

		Extension registryStatus = p.addExtension();
		registryStatus.setUrl(REGISTRY_STATUS_EXTENSION);
		Coding registryValue = new Coding()
			.setSystem(REGISTRY_STATUS_INDICATOR)
			.setCode(pm.getRegistryStatusIndicator());
		registryStatus.setValue(registryValue);
		if (pm.getRegistryStatusIndicatorDate() != null) {
			registryValue.setVersion(pm.getRegistryStatusIndicatorDate().toString());
		}

		for (PatientGuardian patientGuardian : pm.getPatientGuardians()) {
			Patient.ContactComponent contact = p.addContact();
			HumanName contactName = new HumanName();
			contact.setName(contactName);
			contact.addRelationship().setText(patientGuardian.getGuardianRelationship());
			contactName.setFamily(patientGuardian.getName().getNameLast());
			contactName.addGivenElement().setValue(patientGuardian.getName().getNameFirst());
			contactName.addGivenElement().setValue(patientGuardian.getName().getNameMiddle());
		}
		return p;
	}

}