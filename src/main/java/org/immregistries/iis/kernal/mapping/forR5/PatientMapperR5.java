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
	private static final String REGISTRY_STATUS_EXTENSION = "registryStatus";
	private static final String REGISTRY_STATUS_INDICATOR = "registryStatusIndicator";
	private static final String ETHNICITY_EXTENSION = "ethnicity";
	private static final String ETHNICITY_SYSTEM = "ethnicity";
	private static final String RACE = "race";
	private static final String RACE_SYSTEM = "race";
	private static final String PUBLICITY_EXTENSION = "publicity";
	private static final String PUBLICITY_SYSTEM = "publicityIndicator";
	private static final String PROTECTION_EXTENSION = "protection";
	private static final String PROTECTION_SYSTEM = "protectionIndicator";
	private static final String YES = "Y";
	private static final String NO = "N";

	public PatientReported getReportedWithMaster(Patient p) {
		PatientReported patientReported = getReported(p);
		patientReported.setPatient(
			fhirRequests.searchPatientMaster(
				Patient.IDENTIFIER.exactly().systemAndIdentifier(patientReported.getPatientReportedAuthority(),patientReported.getPatientReportedExternalLink())
			));
		return patientReported;
	}
	public PatientReported getReported(Patient p) {
		PatientReported patientReported = new PatientReported();
		patientReported.setPatientReportedId(new IdType(p.getId()).getIdPart());
//		patientReported.setPatientReportedExternalLink(MappingHelper.filterIdentifier(p.getIdentifier(), MRN_SYSTEM).getValue()); TODO see if only use MRN
		patientReported.setPatientReportedExternalLink(p.getIdentifierFirstRep().getValue());
		patientReported.setUpdatedDate(p.getMeta().getLastUpdated());

		patientReported.setPatientReportedAuthority(p.getIdentifierFirstRep().getSystem());
		patientReported.setPatientBirthDate(p.getBirthDate());
		patientReported.setManagingOrganizationId(p.getManagingOrganization().getId());
		// Name
		HumanName name = p.getNameFirstRep();
		patientReported.setPatientNameLast(name.getFamily());
		if (name.getGiven().size() > 0) {
			patientReported.setPatientNameFirst(name.getGiven().get(0).getValueNotNull());
		}
		if (name.getGiven().size() > 1) {
			patientReported.setPatientNameMiddle(name.getGiven().get(1).getValueNotNull());
		}

//		patientReported.setPatientMotherMaiden(); TODO
		switch (p.getGender()) {
			case MALE:
				patientReported.setPatientSex("M");
				break;
			case FEMALE:
				patientReported.setPatientSex("F");
				break;
			case OTHER:
			default:
				patientReported.setPatientSex("");
				break;
		}
		int raceNumber = 0;
		for (Coding coding: p.getExtensionByUrl(RACE).getValueCodeableConcept().getCoding()) {
			raceNumber++;
			switch (raceNumber) {
				case 1:{
					patientReported.setPatientRace(coding.getCode());
				}
				case 2:{
					patientReported.setPatientRace2(coding.getCode());
				}
				case 3:{
					patientReported.setPatientRace3(coding.getCode());
				}
				case 4:{
					patientReported.setPatientRace4(coding.getCode());
				}
				case 5:{
					patientReported.setPatientRace5(coding.getCode());
				}
				case 6:{
					patientReported.setPatientRace6(coding.getCode());
				}
			}
		}
		if (p.getExtensionByUrl(ETHNICITY_EXTENSION) != null) {
			patientReported.setPatientEthnicity(p.getExtensionByUrl(ETHNICITY_EXTENSION).getValueCodeType().getValue());
		}

		for (ContactPoint telecom : p.getTelecom()) {
			if (null != telecom.getSystem()) {
				if (telecom.getSystem().equals(ContactPointSystem.PHONE)) {
					patientReported.setPatientPhone(telecom.getValue());
				} else if (telecom.getSystem().equals(ContactPointSystem.EMAIL)) {
					patientReported.setPatientEmail(telecom.getValue());
				}
			}
		}

		if (null != p.getDeceased()) {
			if (p.getDeceased().isBooleanPrimitive()) {
				if (p.getDeceasedBooleanType().booleanValue()) {
					patientReported.setPatientDeathFlag(YES);
				} else {
					patientReported.setPatientDeathFlag(NO);
				}
			}
			if (p.getDeceased().isDateTime()) {
				patientReported.setPatientDeathDate(p.getDeceasedDateTimeType().getValue());
			}
		}
		// Address
		Address address = p.getAddressFirstRep();
		if (address.getLine().size() > 0) {
			patientReported.setPatientAddressLine1(address.getLine().get(0).getValueNotNull());
		}
		if (address.getLine().size() > 1) {
			patientReported.setPatientAddressLine2(address.getLine().get(1).getValueNotNull());
		}
		patientReported.setPatientAddressCity(address.getCity());
		patientReported.setPatientAddressState(address.getState());
		patientReported.setPatientAddressZip(address.getPostalCode());
		patientReported.setPatientAddressCountry(address.getCountry());
		patientReported.setPatientAddressCountyParish(address.getDistrict());

		if (null != p.getMultipleBirth()) {
			if (p.getMultipleBirth().isBooleanPrimitive()) {
				if (p.getMultipleBirthBooleanType().booleanValue()) {
					patientReported.setPatientBirthFlag(YES);
				} else {
					patientReported.setPatientBirthFlag(NO);
				}
			} else {
				patientReported.setPatientBirthOrder(String.valueOf(p.getMultipleBirthIntegerType()));
			}
		}

		Extension publicity = p.getExtensionByUrl(PUBLICITY_EXTENSION);
		if (publicity != null) {
			patientReported.setPublicityIndicator(publicity.getValueCoding().getCode());
			if (publicity.getValueCoding().getVersion() != null && !publicity.getValueCoding().getVersion().isBlank() ) {
				try {
					patientReported.setPublicityIndicatorDate(MappingHelper.sdf.parse(publicity.getValueCoding().getVersion()));
				} catch (ParseException e) {
//					throw new RuntimeException(e);
				}
			}
		}
		Extension protection = p.getExtensionByUrl(PROTECTION_EXTENSION);
		if (protection != null) {
			patientReported.setProtectionIndicator(protection.getValueCoding().getCode());
			if (protection.getValueCoding().getVersion() != null && !protection.getValueCoding().getVersion().isBlank()) {
				try {
					patientReported.setProtectionIndicatorDate(MappingHelper.sdf.parse(protection.getValueCoding().getVersion()));
				} catch (ParseException e) {
//					throw new RuntimeException(e);
				}
			}
		}
		Extension registry = p.getExtensionByUrl(REGISTRY_STATUS_EXTENSION);
		if (registry != null) {
			patientReported.setRegistryStatusIndicator(registry.getValueCoding().getCode());
			if (registry.getValueCoding().getVersion() != null && !registry.getValueCoding().getVersion().isBlank()){
				try {
					patientReported.setRegistryStatusIndicatorDate(MappingHelper.sdf.parse(registry.getValueCoding().getVersion()));
				} catch (ParseException e) {
//				throw new RuntimeException(e);
				}
			}
		}

		// patientReported.setRegistryStatusIndicator(p.getActive());
		// Patient Contact / Guardian
		RelatedPerson relatedPerson = fhirRequests.searchRelatedPerson(RelatedPerson.PATIENT.hasAnyOfIds(patientReported.getPatientReportedId(),patientReported.getPatientReportedExternalLink()));
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

		p.addIdentifier(MappingHelper.getFhirIdentifier(
			pr.getPatientReportedAuthority(),
			pr.getPatientReportedExternalLink()));
		p.setManagingOrganization(new Reference(pr.getManagingOrganizationId()));
		p.setBirthDate(pr.getPatientBirthDate());
		if (p.getNameFirstRep() != null) {
			HumanName name = p.addName()
				.setFamily(pr.getPatientNameLast())
				.addGiven(pr.getPatientNameFirst())
				.addGiven(pr.getPatientNameMiddle());
//			   .setUse(HumanName.NameUse.USUAL);
		}

//			p.addName().setUse(HumanName.NameUse.MAIDEN).setFamily(pr.getPatientMotherMaiden()); TODO
		switch (pr.getPatientSex()) {
			case "M":
				p.setGender(AdministrativeGender.MALE);
				break;
			case "F":
				p.setGender(AdministrativeGender.FEMALE);
				break;
			default:
				p.setGender(AdministrativeGender.OTHER);
				break;
		}

		//Race and ethnicity
		Extension raceExtension =  p.addExtension();
		raceExtension.setUrl(RACE);
		CodeableConcept race = new CodeableConcept().setText(RACE_SYSTEM);
		raceExtension.setValue(race);
		if (pr.getPatientRace() != null && !pr.getPatientRace().isBlank()) {
			race.addCoding().setCode(pr.getPatientRace());
		}
		if (pr.getPatientRace2() != null && !pr.getPatientRace2().isBlank()) {
			race.addCoding().setCode(pr.getPatientRace2());
		}
		if (pr.getPatientRace3() != null && !pr.getPatientRace3().isBlank()) {
			race.addCoding().setCode(pr.getPatientRace3());
		}
		if (pr.getPatientRace4() != null && !pr.getPatientRace4().isBlank()) {
			race.addCoding().setCode(pr.getPatientRace4());
		}
		if (pr.getPatientRace5() != null && !pr.getPatientRace5().isBlank()) {
			race.addCoding().setCode(pr.getPatientRace5());
		}
		if (pr.getPatientRace6() != null && !pr.getPatientRace6().isBlank()) {
			race.addCoding().setCode(pr.getPatientRace6());
		}
		p.addExtension(ETHNICITY_EXTENSION,new CodeType().setSystem(ETHNICITY_SYSTEM).setValue(pr.getPatientEthnicity()));
		// telecom
		if (null != pr.getPatientPhone()) {
			p.addTelecom().setSystem(ContactPointSystem.PHONE)
				.setValue(pr.getPatientPhone());
		}
		if (null != pr.getPatientEmail()) {
			p.addTelecom().setSystem(ContactPointSystem.EMAIL)
				.setValue(pr.getPatientEmail());
		}


		if (pr.getPatientDeathDate() != null) {
			p.setDeceased(new DateType(pr.getPatientDeathDate()));
		} else if (pr.getPatientDeathFlag().equals(YES)) {
			p.setDeceased(new BooleanType(true));
		} else if (pr.getPatientDeathFlag().equals(NO)) {
			p.setDeceased(new BooleanType(false));
		}

		p.addAddress().addLine(pr.getPatientAddressLine1())
			.addLine(pr.getPatientAddressLine2())
			.setCity(pr.getPatientAddressCity())
			.setCountry(pr.getPatientAddressCountry())
			.setState(pr.getPatientAddressState())
			.setDistrict(pr.getPatientAddressCountyParish())
			.setPostalCode(pr.getPatientAddressZip());

		if (pr.getPatientBirthOrder() != null && !pr.getPatientBirthOrder().isBlank()) {
			p.setMultipleBirth(new IntegerType().setValue(Integer.parseInt(pr.getPatientBirthOrder())));
		} else if (pr.getPatientBirthFlag().equals(YES)) {
			p.setMultipleBirth(new BooleanType(true));
		}

		Extension publicity =  p.addExtension();
		publicity.setUrl(PUBLICITY_EXTENSION);
		publicity.setValue(
			new Coding().setSystem(PUBLICITY_SYSTEM)
				.setCode(pr.getPublicityIndicator()));
		if (pr.getPublicityIndicatorDate() != null) {
			publicity.getValueCoding().setVersion(pr.getPublicityIndicatorDate().toString());
		}
		Extension protection =  p.addExtension();
		protection.setUrl(PROTECTION_EXTENSION);
		protection.setValue(
			new Coding().setSystem(PROTECTION_SYSTEM)
				.setCode(pr.getProtectionIndicator()));
		if (pr.getProtectionIndicatorDate() != null) {
			protection.getValueCoding().setVersion(pr.getProtectionIndicatorDate().toString());
		}

		Extension registryStatus =  p.addExtension();
		registryStatus.setUrl(REGISTRY_STATUS_EXTENSION);
		registryStatus.setValue(
			new Coding().setSystem(REGISTRY_STATUS_INDICATOR)
				.setCode(pr.getRegistryStatusIndicator()));
		if (pr.getRegistryStatusIndicatorDate() != null) {
			registryStatus.getValueCoding().setVersion(pr.getRegistryStatusIndicatorDate().toString());
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