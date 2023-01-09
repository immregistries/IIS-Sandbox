package org.immregistries.iis.kernal.mapping.forR5;


import ca.uhn.fhir.jpa.starter.annotations.OnR5Condition;
import org.hl7.fhir.r5.model.*;
import org.hl7.fhir.r5.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r5.model.Enumerations.AdministrativeGender;
import org.immregistries.iis.kernal.mapping.Interfaces.PatientMapper;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.repository.FhirRequesterR5;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.text.ParseException;

import static org.immregistries.iis.kernal.repository.FhirRequester.GOLDEN_SYSTEM_IDENTIFIER;


@Service
@Conditional(OnR5Condition.class)
public class PatientMapperR5 implements PatientMapper<Patient> {

	@Autowired
	FhirRequesterR5 fhirRequests;
	@Autowired
	RelatedPersonMapperR5 relatedPersonMapperR5;

	public PatientReported getReportedWithMaster(Patient p) {
		PatientReported patientReported = getReported(p);
		patientReported.setPatient(
			fhirRequests.searchPatientMaster(
				Patient.IDENTIFIER.exactly().systemAndIdentifier(patientReported.getPatientReportedAuthority(), patientReported.getPatientReportedExternalLink())
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
				if (telecom.getSystem().equals(ContactPointSystem.PHONE)) {
					patientReported.setPhone(telecom.getValue());
				} else if (telecom.getSystem().equals(ContactPointSystem.EMAIL)) {
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
			if (value.getVersion() != null && !value.getVersion().isBlank()) {
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
			if (value.getVersion() != null && !value.getVersion().isBlank()) {
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
			if (value.getVersion() != null && !value.getVersion().isBlank()) {
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
			relatedPersonMapperR5.fillGuardianInformation(patientReported, relatedPerson);
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
				p.setGender(AdministrativeGender.MALE);
				break;
			case FEMALE_SEX:
				p.setGender(AdministrativeGender.FEMALE);
				break;
			default:
				p.setGender(AdministrativeGender.OTHER);
				break;
		}

		//Race and ethnicity
		Extension raceExtension = p.addExtension();
		raceExtension.setUrl(RACE);
		CodeableConcept race = new CodeableConcept().setText(RACE_SYSTEM);
		raceExtension.setValue(race);
		if (pr.getRace() != null && !pr.getRace().isBlank()) {
			race.addCoding().setCode(pr.getRace());
		}
		if (pr.getRace2() != null && !pr.getRace2().isBlank()) {
			race.addCoding().setCode(pr.getRace2());
		}
		if (pr.getRace3() != null && !pr.getRace3().isBlank()) {
			race.addCoding().setCode(pr.getRace3());
		}
		if (pr.getRace4() != null && !pr.getRace4().isBlank()) {
			race.addCoding().setCode(pr.getRace4());
		}
		if (pr.getRace5() != null && !pr.getRace5().isBlank()) {
			race.addCoding().setCode(pr.getRace5());
		}
		if (pr.getRace6() != null && !pr.getRace6().isBlank()) {
			race.addCoding().setCode(pr.getRace6());
		}
		p.addExtension(ETHNICITY_EXTENSION, new Coding().setSystem(ETHNICITY_SYSTEM).setCode(pr.getEthnicity()));
		// telecom
		if (null != pr.getPhone()) {
			p.addTelecom().setSystem(ContactPointSystem.PHONE)
				.setValue(pr.getPhone());
		}
		if (null != pr.getEmail()) {
			p.addTelecom().setSystem(ContactPointSystem.EMAIL)
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

		if (pr.getBirthOrder() != null && !pr.getBirthOrder().isBlank()) {
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