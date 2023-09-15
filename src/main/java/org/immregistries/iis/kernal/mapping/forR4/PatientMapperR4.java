package org.immregistries.iis.kernal.mapping.forR4;


import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.fhir.annotations.OnR4Condition;
import org.hl7.fhir.r4.model.*;
import org.immregistries.iis.kernal.mapping.Interfaces.PatientMapper;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.InternalClient.FhirRequesterR4;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.text.ParseException;

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
		pm.setExternalLink(p.getIdentifierFirstRep().getValue());
		pm.setUpdatedDate(p.getMeta().getLastUpdated());

		pm.setPatientReportedAuthority(p.getIdentifierFirstRep().getSystem());
		pm.setBirthDate(p.getBirthDate());
		pm.setManagingOrganizationId(p.getManagingOrganization().getId());
		// Name
		HumanName name = p.getNameFirstRep();
		pm.setNameLast(name.getFamily());
		if (name.getGiven().size() > 0) {
			pm.setNameFirst(name.getGiven().get(0).getValueNotNull());
		}
		if (name.getGiven().size() > 1) {
			pm.setNameMiddle(name.getGiven().get(1).getValueNotNull());
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
		int raceNumber = 0;
		CodeableConcept races = MappingHelper.extensionGetCodeableConcept(p.getExtensionByUrl(RACE));
		for (Coding coding : races.getCoding()) {
			raceNumber++;
			switch (raceNumber) {
				case 1: {
					pm.setRace(coding.getCode());
				}
				case 2: {
					pm.setRace2(coding.getCode());
				}
				case 3: {
					pm.setRace3(coding.getCode());
				}
				case 4:{
					pm.setRace4(coding.getCode());
				}
				case 5:{
					pm.setRace5(coding.getCode());
				}
				case 6:{
					pm.setRace6(coding.getCode());
				}
			}
		}
		if (p.getExtensionByUrl(ETHNICITY_EXTENSION) != null) {
			Coding ethnicity = MappingHelper.extensionGetCoding(p.getExtensionByUrl(ETHNICITY_EXTENSION));
			pm.setEthnicity(ethnicity.getCode());
		}

		for (ContactPoint telecom : p.getTelecom()) {
			if (null != telecom.getSystem()) {
				if (telecom.getSystem().equals(ContactPoint.ContactPointSystem.PHONE)) {
					pm.setPhone(telecom.getValue());
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
		Address address = p.getAddressFirstRep();
		if (address.getLine().size() > 0) {
			pm.setAddressLine1(address.getLine().get(0).getValueNotNull());
		}
		if (address.getLine().size() > 1) {
			pm.setAddressLine2(address.getLine().get(1).getValueNotNull());
		}
		pm.setAddressCity(address.getCity());
		pm.setAddressState(address.getState());
		pm.setAddressZip(address.getPostalCode());
		pm.setAddressCountry(address.getCountry());
		pm.setAddressCountyParish(address.getDistrict());

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
		RelatedPerson relatedPerson = fhirRequests.searchRelatedPerson(
			new SearchParameterMap(RelatedPerson.SP_PATIENT,new ReferenceParam(pm.getPatientId()))
			.add(RelatedPerson.SP_PATIENT,new ReferenceParam(pm.getExternalLink())));
		if (relatedPerson != null) {
			relatedPersonMapperR4.fillGuardianInformation(pm, relatedPerson);
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

		p.addIdentifier(new Identifier()
			.setSystem(pm.getPatientReportedAuthority())
			.setValue(pm.getExternalLink())
			.setType(
				new CodeableConcept(new Coding()
					.setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
					.setCode(pm.getPatientReportedType()))));
		p.setManagingOrganization(new Reference(pm.getManagingOrganizationId()));
		p.setBirthDate(pm.getBirthDate());
		if (p.getNameFirstRep() != null) {
			HumanName name = p.addName()
				.setFamily(pm.getNameLast())
				.addGiven(pm.getNameFirst())
				.addGiven(pm.getNameMiddle());
//			   .setUse(HumanName.NameUse.USUAL);
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
		raceExtension.setUrl(RACE);
		CodeableConcept race = new CodeableConcept().setText(RACE_SYSTEM);
		raceExtension.setValue(race);
		if (StringUtils.isNotBlank(pm.getRace())) {
			race.addCoding().setCode(pm.getRace());
		}
		if (StringUtils.isNotBlank(pm.getRace2())) {
			race.addCoding().setCode(pm.getRace2());
		}
		if (StringUtils.isNotBlank(pm.getRace3())) {
			race.addCoding().setCode(pm.getRace3());
		}
		if (StringUtils.isNotBlank(pm.getRace4())) {
			race.addCoding().setCode(pm.getRace4());
		}
		if (StringUtils.isNotBlank(pm.getRace5())) {
			race.addCoding().setCode(pm.getRace5());
		}
		if (StringUtils.isNotBlank(pm.getRace6())) {
			race.addCoding().setCode(pm.getRace6());
		}
		p.addExtension(ETHNICITY_EXTENSION, new Coding().setSystem(ETHNICITY_SYSTEM).setCode(pm.getEthnicity()));
		// telecom
		if (null != pm.getPhone()) {
			p.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE)
				.setValue(pm.getPhone());
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

		p.addAddress().addLine(pm.getAddressLine1())
			.addLine(pm.getAddressLine2())
			.setCity(pm.getAddressCity())
			.setCountry(pm.getAddressCountry())
			.setState(pm.getAddressState())
			.setDistrict(pm.getAddressCountyParish())
			.setPostalCode(pm.getAddressZip());

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

		Patient.ContactComponent contact = p.addContact();
		HumanName contactName = new HumanName();
		contact.setName(contactName);
		contact.addRelationship().setText(pm.getGuardianRelationship());
		contactName.setFamily(pm.getGuardianLast());
		contactName.addGivenElement().setValue(pm.getGuardianFirst());
		contactName.addGivenElement().setValue(pm.getGuardianMiddle());
		return p;
	}

}