package org.immregistries.iis.kernal.mapping.forR4;


import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.fhir.annotations.OnR4Condition;
import org.hl7.fhir.r4.model.*;
import org.immregistries.iis.kernal.mapping.Interfaces.PatientMapper;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.repository.FhirRequesterR4;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.text.ParseException;

import static org.immregistries.iis.kernal.repository.FhirRequester.GOLDEN_SYSTEM_IDENTIFIER;


@Service
@Conditional(OnR4Condition.class)
public class PatientMapperR4 implements PatientMapper<Patient> {

	@Autowired
	FhirRequesterR4 fhirRequests;
	@Autowired
	RelatedPersonMapperR4 relatedPersonMapperR4;

	public PatientReported getReportedWithMaster(Patient p) {
		PatientReported patientReported = getReported(p);
		patientReported.setPatient(
			fhirRequests.searchPatientMaster(
				Patient.IDENTIFIER.exactly().systemAndIdentifier(patientReported.getPatientReportedAuthority(), patientReported.getPatientReportedExternalLink()) // TODO change to get from mdm
			));
		return patientReported;
	}

	public PatientReported getReported(Patient p) {
		PatientReported patientReported = new PatientReported();
		patientReported.setId(new IdType(p.getId()).getIdPart());
		patientReported.setPatientReportedExternalLink(p.getIdentifierFirstRep().getValue());
		patientReported.setUpdatedDate(p.getMeta().getLastUpdated());

		patientReported.setPatientReportedAuthority(p.getIdentifierFirstRep().getSystem());
		patientReported.setBirthDate(p.getBirthDate());
		patientReported.setManagingOrganizationId(p.getManagingOrganization().getId());
		// Name
		HumanName name = p.getNameFirstRep();
		patientReported.setNameLast(name.getFamily());
		if (name.getGiven().size() > 0) {
			patientReported.setNameFirst(name.getGiven().get(0).getValueNotNull());
		}
		if (name.getGiven().size() > 1) {
			patientReported.setNameMiddle(name.getGiven().get(1).getValueNotNull());
		}

		Extension motherMaiden = p.getExtensionByUrl(MOTHER_MAIDEN_NAME);
		if (motherMaiden != null) {
			patientReported.setMotherMaidenName(motherMaiden.getValue().toString());
		}
		switch (p.getGender()) {
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
		int raceNumber = 0;
		CodeableConcept races = MappingHelper.extensionGetCodeableConcept(p.getExtensionByUrl(RACE));
		for (Coding coding : races.getCoding()) {
			raceNumber++;
			switch (raceNumber) {
				case 1: {
					patientReported.setRace(coding.getCode());
				}
				case 2: {
					patientReported.setRace2(coding.getCode());
				}
				case 3: {
					patientReported.setRace3(coding.getCode());
				}
				case 4:{
					patientReported.setRace4(coding.getCode());
				}
				case 5:{
					patientReported.setRace5(coding.getCode());
				}
				case 6:{
					patientReported.setRace6(coding.getCode());
				}
			}
		}
		if (p.getExtensionByUrl(ETHNICITY_EXTENSION) != null) {
			Coding ethnicity = MappingHelper.extensionGetCoding(p.getExtensionByUrl(ETHNICITY_EXTENSION));
			patientReported.setEthnicity(ethnicity.getCode());
		}

		for (ContactPoint telecom : p.getTelecom()) {
			if (null != telecom.getSystem()) {
				if (telecom.getSystem().equals(ContactPoint.ContactPointSystem.PHONE)) {
					patientReported.setPhone(telecom.getValue());
				} else if (telecom.getSystem().equals(ContactPoint.ContactPointSystem.EMAIL)) {
					patientReported.setEmail(telecom.getValue());
				}
			}
		}

		if (null != p.getDeceased()) {
			if (p.getDeceased().isBooleanPrimitive()) {
				if (p.getDeceasedBooleanType().booleanValue()) {
					patientReported.setDeathFlag(YES);
				} else {
					patientReported.setDeathFlag(NO);
				}
			}
			if (p.getDeceased().isDateTime()) {
				patientReported.setDeathDate(p.getDeceasedDateTimeType().getValue());
			}
		}
		// Address
		Address address = p.getAddressFirstRep();
		if (address.getLine().size() > 0) {
			patientReported.setAddressLine1(address.getLine().get(0).getValueNotNull());
		}
		if (address.getLine().size() > 1) {
			patientReported.setAddressLine2(address.getLine().get(1).getValueNotNull());
		}
		patientReported.setAddressCity(address.getCity());
		patientReported.setAddressState(address.getState());
		patientReported.setAddressZip(address.getPostalCode());
		patientReported.setAddressCountry(address.getCountry());
		patientReported.setAddressCountyParish(address.getDistrict());

		if (null != p.getMultipleBirth()) {
			if (p.getMultipleBirth().isBooleanPrimitive()) {
				if (p.getMultipleBirthBooleanType().booleanValue()) {
					patientReported.setBirthFlag(YES);
				} else {
					patientReported.setBirthFlag(NO);
				}
			} else {
				patientReported.setBirthOrder(String.valueOf(p.getMultipleBirthIntegerType()));
			}
		}

		Extension publicity = p.getExtensionByUrl(PUBLICITY_EXTENSION);
		if (publicity != null) {
			Coding value = MappingHelper.extensionGetCoding(publicity);
			patientReported.setPublicityIndicator(value.getCode());
			if (StringUtils.isNotBlank(value.getVersion())) {
				try {
					patientReported.setPublicityIndicatorDate(MappingHelper.sdf.parse(value.getVersion()));
				} catch (ParseException e) {
//					throw new RuntimeException(e);
				}
			}
		}
		Extension protection = p.getExtensionByUrl(PROTECTION_EXTENSION);
		if (protection != null) {
			Coding value = MappingHelper.extensionGetCoding(protection);
			patientReported.setProtectionIndicator(value.getCode());
			if (StringUtils.isNotBlank(value.getVersion())) {
				try {
					patientReported.setProtectionIndicatorDate(MappingHelper.sdf.parse(value.getVersion()));
				} catch (ParseException e) {
//					throw new RuntimeException(e);
				}
			}
		}
		Extension registry = p.getExtensionByUrl(REGISTRY_STATUS_EXTENSION);
		if (registry != null) {
			Coding value = MappingHelper.extensionGetCoding(registry);
			patientReported.setRegistryStatusIndicator(value.getCode());
			if (StringUtils.isNotBlank(value.getVersion())) {
				try {
					patientReported.setRegistryStatusIndicatorDate(MappingHelper.sdf.parse(value.getVersion()));
				} catch (ParseException e) {
//				throw new RuntimeException(e);
				}
			}
		}

		// patientReported.setRegistryStatusIndicator(p.getActive());
		// Patient Contact / Guardian
		RelatedPerson relatedPerson = fhirRequests.searchRelatedPerson(RelatedPerson.PATIENT.hasAnyOfIds(patientReported.getId(), patientReported.getPatientReportedExternalLink()));
		if (relatedPerson != null) {
			relatedPersonMapperR4.fillGuardianInformation(patientReported, relatedPerson);
		}
		return patientReported;
	}

	public PatientMaster getMaster(Patient p) {
		PatientMaster patientMaster = new PatientMaster();
		patientMaster.setPatientExternalLink(p.getIdentifier().stream().filter(identifier -> !identifier.getSystem().equals(GOLDEN_SYSTEM_IDENTIFIER)).findFirst().orElse(new Identifier()).getValue()); //TODO deal with MDM Identifiers
		patientMaster.setPatientNameFirst(p.getNameFirstRep().getGiven().get(0).getValue());
		if (p.getNameFirstRep().getGiven().size() > 1) {
			patientMaster.setPatientNameMiddle(p.getNameFirstRep().getGiven().get(1).getValue());
		}
		patientMaster.setPatientNameLast(p.getNameFirstRep().getFamily());
		patientMaster.setPatientExternalLink(p.getIdentifierFirstRep().getValue());
//	  patientMaster.setPatientAddressFrag();
		return patientMaster;
	}

	public Patient getFhirResource(PatientReported pr) {
		Patient p = new Patient();

		p.addIdentifier(new Identifier()
			.setSystem(pr.getPatientReportedAuthority())
			.setValue(pr.getPatientReportedExternalLink())
			.setType(
				new CodeableConcept(new Coding()
					.setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
					.setCode(pr.getPatientReportedType()))));
		p.setManagingOrganization(new Reference(pr.getManagingOrganizationId()));
		p.setBirthDate(pr.getBirthDate());
		if (p.getNameFirstRep() != null) {
			HumanName name = p.addName()
				.setFamily(pr.getNameLast())
				.addGiven(pr.getNameFirst())
				.addGiven(pr.getNameMiddle());
//			   .setUse(HumanName.NameUse.USUAL);
		}

		Extension motherMaidenName = p.addExtension()
			.setUrl(MOTHER_MAIDEN_NAME)
			.setValue(new StringType(pr.getMotherMaidenName()));

		switch (pr.getSex()) {
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
		raceExtension.setUrl(RACE);
		CodeableConcept race = new CodeableConcept().setText(RACE_SYSTEM);
		raceExtension.setValue(race);
		if (StringUtils.isNotBlank(pr.getRace())) {
			race.addCoding().setCode(pr.getRace());
		}
		if (StringUtils.isNotBlank(pr.getRace2())) {
			race.addCoding().setCode(pr.getRace2());
		}
		if (StringUtils.isNotBlank(pr.getRace3())) {
			race.addCoding().setCode(pr.getRace3());
		}
		if (StringUtils.isNotBlank(pr.getRace4())) {
			race.addCoding().setCode(pr.getRace4());
		}
		if (StringUtils.isNotBlank(pr.getRace5())) {
			race.addCoding().setCode(pr.getRace5());
		}
		if (StringUtils.isNotBlank(pr.getRace6())) {
			race.addCoding().setCode(pr.getRace6());
		}
		p.addExtension(ETHNICITY_EXTENSION, new Coding().setSystem(ETHNICITY_SYSTEM).setCode(pr.getEthnicity()));
		// telecom
		if (null != pr.getPhone()) {
			p.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE)
				.setValue(pr.getPhone());
		}
		if (null != pr.getEmail()) {
			p.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL)
				.setValue(pr.getEmail());
		}


		if (pr.getDeathDate() != null) {
			p.setDeceased(new DateType(pr.getDeathDate()));
		} else if (pr.getDeathFlag().equals(YES)) {
			p.setDeceased(new BooleanType(true));
		} else if (pr.getDeathFlag().equals(NO)) {
			p.setDeceased(new BooleanType(false));
		}

		p.addAddress().addLine(pr.getAddressLine1())
			.addLine(pr.getAddressLine2())
			.setCity(pr.getAddressCity())
			.setCountry(pr.getAddressCountry())
			.setState(pr.getAddressState())
			.setDistrict(pr.getAddressCountyParish())
			.setPostalCode(pr.getAddressZip());

		if (StringUtils.isNotBlank(pr.getBirthOrder())) {
			p.setMultipleBirth(new IntegerType().setValue(Integer.parseInt(pr.getBirthOrder())));
		} else if (pr.getBirthFlag().equals(YES)) {
			p.setMultipleBirth(new BooleanType(true));
		}

		Extension publicity = p.addExtension();
		publicity.setUrl(PUBLICITY_EXTENSION);
		Coding publicityValue = new Coding()
			.setSystem(PUBLICITY_SYSTEM)
			.setCode(pr.getPublicityIndicator());
		publicity.setValue(publicityValue);
		if (pr.getPublicityIndicatorDate() != null) {
			publicityValue.setVersion(pr.getPublicityIndicatorDate().toString());
		}

		Extension protection = p.addExtension();
		protection.setUrl(PROTECTION_EXTENSION);
		Coding protectionValue = new Coding()
			.setSystem(PROTECTION_SYSTEM)
			.setCode(pr.getProtectionIndicator());
		protection.setValue(protectionValue);
		if (pr.getProtectionIndicatorDate() != null) {
			protectionValue.setVersion(pr.getProtectionIndicatorDate().toString());
		}

		Extension registryStatus = p.addExtension();
		registryStatus.setUrl(REGISTRY_STATUS_EXTENSION);
		Coding registryValue = new Coding()
			.setSystem(REGISTRY_STATUS_INDICATOR)
			.setCode(pr.getRegistryStatusIndicator());
		registryStatus.setValue(registryValue);
		if (pr.getRegistryStatusIndicatorDate() != null) {
			registryValue.setVersion(pr.getRegistryStatusIndicatorDate().toString());
		}

		Patient.ContactComponent contact = p.addContact();
		HumanName contactName = new HumanName();
		contact.setName(contactName);
		contact.addRelationship().setText(pr.getGuardianRelationship());
		contactName.setFamily(pr.getGuardianLast());
		contactName.addGivenElement().setValue(pr.getGuardianFirst());
		contactName.addGivenElement().setValue(pr.getGuardianMiddle());
		return p;
	}

}