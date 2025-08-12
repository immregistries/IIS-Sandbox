package org.immregistries.iis.kernal.logic;

import ca.uhn.fhir.util.ExtensionUtil;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.ContactPoint;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.mismo.match.StringUtils;
import org.immregistries.mismo.match.model.Patient;
import org.springframework.stereotype.Service;

import static org.immregistries.iis.kernal.mapping.interfaces.PatientMapper.LINK_ID;
import static org.immregistries.iis.kernal.mapping.interfaces.PatientMapper.MOTHER_MAIDEN_NAME;

@Service
public class PatientMismoConversionService {

	public Patient convertFromR5(org.hl7.fhir.r5.model.Patient patient) {
		Patient mismo = new Patient();
		org.hl7.fhir.r5.model.Identifier identifier = MappingHelper.filterIdentifierR5(patient.getIdentifier(), "http://codi.mitre.org");
		if (identifier != null) {
			mismo.setValue("identifier", identifier.getValue());
		}
		if (patient.hasName()) {
			org.hl7.fhir.r5.model.HumanName humanName = patient.getNameFirstRep();
			mismo.setNameFirst(humanName.getGivenAsSingleString());

			/**
			 * Checking if hyphenated last name
			 */
			String[] family = humanName.getFamily().split("-");
			if (family.length > 1) {
				mismo.setNameLast(family[0]);
				mismo.setNameLastHyph(family[1]);
			} else {
				mismo.setNameLast(humanName.getFamily());
			}

			if (humanName.getGiven().size() > 1) {
				mismo.setNameMiddle(humanName.getGiven().get(1).getValue());
			}
			mismo.setNameSuffix(humanName.getSuffixAsSingleString());
			mismo.setNameAlias(humanName.getNameAsSingleString());
		}

		mismo.setBirthDate(patient.getBirthDateElement().asStringValue());
		if (patient.getGender() != null) {
			mismo.setGender(patient.getGender().toCode());
		}

		org.hl7.fhir.r5.model.Identifier ssn = MappingHelper.filterIdentifierTypeR5(patient.getIdentifier(), "SS");
		if (ssn != null) {
			mismo.setSsn(ssn.getValue());
		}


		if (patient.hasAddress()) {
			org.hl7.fhir.r5.model.Address address = patient.getAddressFirstRep();
			mismo.setAddressCity(address.getCity());
			mismo.setAddressState(address.getState());
			mismo.setAddressZip(address.getPostalCode());
			if (address.hasLine()) {
				mismo.setAddressStreet1(address.getLine().get(0).getValue());
				if (address.getLine().size() > 1) {
					mismo.setAddressStreet2(address.getLine().get(1).getValue());
				}
			}
//			mismo.setAddressStreet1Alt(address.get);

			if (patient.getAddress().size() > 1) {
				org.hl7.fhir.r5.model.Address address2 = patient.getAddress().get(1);
				mismo.setAddress2City(address2.getCity());
				mismo.setAddress2State(address2.getState());
				mismo.setAddress2Zip(address2.getPostalCode());
				if (address2.hasLine()) {
					mismo.setAddress2Street1(address2.getLine().get(0).getValue());
					if (address2.getLine().size() > 1) {
						mismo.setAddress2Street2(address2.getLine().get(1).getValue());
					}
				}
			}
		}
		IBaseExtension motherMaiden = ExtensionUtil.getExtensionByUrl(patient, MOTHER_MAIDEN_NAME);
		if (motherMaiden != null) {
			mismo.setMotherMaidenName(motherMaiden.getValue().toString());
		}

		org.hl7.fhir.r5.model.Identifier mrn = MappingHelper.filterIdentifierTypeR5(patient.getIdentifier(), "MR");
		if (mrn != null) {
			mismo.setMrns(mrn.getValue());
		}
		for (org.hl7.fhir.r5.model.ContactPoint telecom : patient.getTelecom()) {
			if (null != telecom.getSystem()) {
				if (telecom.getSystem().equals(ContactPoint.ContactPointSystem.PHONE) && StringUtils.isNotEmpty(mismo.getPhone())) {
					mismo.setPhone(telecom.getValue());
				} else if (telecom.getSystem().equals(ContactPoint.ContactPointSystem.PHONE) && StringUtils.isNotEmpty(mismo.getPhone())) {
					mismo.setValue("phone2", telecom.getValue());
				} else if (telecom.getSystem().equals(ContactPoint.ContactPointSystem.EMAIL)) {
					mismo.setValue("email", telecom.getValue());
				}
			}
		}

		if (patient.hasMultipleBirthBooleanType()) {
			mismo.setBirthOrder(patient.getMultipleBirthBooleanType().getValueAsString());
		} else if (patient.hasMultipleBirthIntegerType()) {
			mismo.setBirthOrder(patient.getMultipleBirthIntegerType().getValueAsString());
		}
		return mismo;
	}


	public Patient convertFromFhir(IBaseResource patient) {
		if (patient instanceof org.hl7.fhir.r4.model.Patient) {
			return convertFromR4((org.hl7.fhir.r4.model.Patient) patient);
		} else if (patient instanceof org.hl7.fhir.r5.model.Patient) {
			return convertFromR5((org.hl7.fhir.r5.model.Patient) patient);
		} else {
			return null; // TODO throw exception
		}
	}

	public Patient convertFromR4(org.hl7.fhir.r4.model.Patient patient) {
		Patient mismo = new Patient();
		org.hl7.fhir.r4.model.Identifier identifier = MappingHelper.filterIdentifierR4(patient.getIdentifier(), "http://codi.mitre.org");
		if (identifier != null) {
			mismo.setValue("identifier", identifier.getValue());
		}
		if (patient.hasName()) {
			org.hl7.fhir.r4.model.HumanName humanName = patient.getNameFirstRep();
			mismo.setNameFirst(humanName.getGivenAsSingleString());
			if (humanName.hasFamily()) {
				String[] family = humanName.getFamily().split("-");
				if (family.length > 1) {
					mismo.setNameLast(family[0]);
					mismo.setNameLastHyph(family[1]);
				} else {
					mismo.setNameLast(humanName.getFamily());
				}
			}
			if (humanName.getGiven().size() > 1) {
				mismo.setNameMiddle(humanName.getGiven().get(1).getValue());
			}
			mismo.setNameSuffix(humanName.getSuffixAsSingleString());
			mismo.setNameAlias(humanName.getNameAsSingleString());
		}

		mismo.setBirthDate(patient.getBirthDateElement().asStringValue());
		if (patient.getGender() != null) {
			mismo.setGender(patient.getGender().toCode());
		}

		org.hl7.fhir.r4.model.Identifier ssn = MappingHelper.filterIdentifierTypeR4(patient.getIdentifier(), "SS");
		if (ssn != null) {
			mismo.setSsn(ssn.getValue());
		}


		if (patient.hasAddress()) {
			org.hl7.fhir.r4.model.Address address = patient.getAddressFirstRep();
			mismo.setAddressCity(address.getCity());
			mismo.setAddressState(address.getState());
			mismo.setAddressZip(address.getPostalCode());
			if (address.hasLine()) {
				mismo.setAddressStreet1(address.getLine().get(0).getValue());
				if (address.getLine().size() > 1) {
					mismo.setAddressStreet2(address.getLine().get(1).getValue());
				}
			}
//			mismo.setAddressStreet1Alt(address.get);

			if (patient.getAddress().size() > 1) {
				org.hl7.fhir.r4.model.Address address2 = patient.getAddress().get(1);
				mismo.setAddress2City(address2.getCity());
				mismo.setAddress2State(address2.getState());
				mismo.setAddress2Zip(address2.getPostalCode());
				if (address2.hasLine()) {
					mismo.setAddress2Street1(address2.getLine().get(0).getValue());
					if (address2.getLine().size() > 1) {
						mismo.setAddress2Street2(address2.getLine().get(1).getValue());
					}
				}
			}
		}
		IBaseExtension motherMaiden = ExtensionUtil.getExtensionByUrl(patient, MOTHER_MAIDEN_NAME);
		if (motherMaiden != null) {
			mismo.setMotherMaidenName(motherMaiden.getValue().toString());
		}

		org.hl7.fhir.r4.model.Identifier mrn = MappingHelper.filterIdentifierTypeR4(patient.getIdentifier(), "MR");
		if (mrn != null) {
			mismo.setMrns(mrn.getValue());
		}
		for (org.hl7.fhir.r4.model.ContactPoint telecom : patient.getTelecom()) {
			if (null != telecom.getSystem()) {
				if (telecom.getSystem().equals(ContactPoint.ContactPointSystem.PHONE) && StringUtils.isNotEmpty(mismo.getPhone())) {
					mismo.setPhone(telecom.getValue());
				} else if (telecom.getSystem().equals(ContactPoint.ContactPointSystem.PHONE) && StringUtils.isNotEmpty(mismo.getPhone())) {
					mismo.setValue("phone2", telecom.getValue());
				} else if (telecom.getSystem().equals(ContactPoint.ContactPointSystem.EMAIL)) {
					mismo.setValue("email", telecom.getValue());
				}
			}
		}

		if (patient.hasMultipleBirthBooleanType()) {
			mismo.setBirthOrder(patient.getMultipleBirthBooleanType().getValueAsString());
		} else if (patient.hasMultipleBirthIntegerType()) {
			mismo.setBirthOrder(patient.getMultipleBirthIntegerType().getValueAsString());
		}
		return mismo;
	}

	public Patient convertFromR4IncludingLink(org.hl7.fhir.r4.model.Patient patient) {
		Patient mismo = convertFromR4(patient);
		org.hl7.fhir.r4.model.Identifier linkedWith = MappingHelper.filterIdentifierR4(patient.getIdentifier(), LINK_ID);
		if (linkedWith != null) {
			Patient link = new Patient();
			link.setValue("identifier", linkedWith.getValue());
			mismo.setLinkWith(link);
//			IBundleProvider bundleProvider = fhirRequester.searchRegularRecord(org.hl7.fhir.r4.model.Patient.class, new SearchParameterMap("identifier", new TokenParam().setSystem(LINK_ID).setValue(linkedWith.getValue())));
//			if (!bundleProvider.isEmpty()) {
//				mismo.setLinkWith(convertFromR4((org.hl7.fhir.r4.model.Patient) bundleProvider.getAllResources().get(0)));
//			}
		}
		return mismo;
	}

	public Patient convertFromR5IncludingLink(org.hl7.fhir.r5.model.Patient patient) {
		Patient mismo = convertFromR5(patient);
		org.hl7.fhir.r5.model.Identifier linkedWith = MappingHelper.filterIdentifierR5(patient.getIdentifier(), LINK_ID);
		if (linkedWith != null) {
			Patient link = new Patient();
			link.setValue("identifier", linkedWith.getValue());
			mismo.setLinkWith(link);
//			IBundleProvider bundleProvider = fhirRequester.searchRegularRecord(org.hl7.fhir.r5.model.Patient.class, new SearchParameterMap("identifier", new TokenParam().setSystem(LINK_ID).setValue(linkedWith.getValue())));
//			if (!bundleProvider.isEmpty()) {
//				mismo.setLinkWith(convertFromR5((org.hl7.fhir.r5.model.Patient) bundleProvider.getAllResources().get(0)));
//			}
		}
		return mismo;
	}
}
