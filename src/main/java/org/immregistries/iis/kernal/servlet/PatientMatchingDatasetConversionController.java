package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.context.FhirContext;

import ca.uhn.fhir.mdm.util.IdentifierUtil;
import ca.uhn.fhir.util.ExtensionUtil;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.r5.model.*;
//import org.hl7.fhir.r5.model.Patient;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.mismo.match.StringUtils;
import org.immregistries.mismo.match.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.immregistries.iis.kernal.mapping.Interfaces.PatientMapper.*;

@RestController
@RequestMapping("patientMatchingDatasetConversion")
public class PatientMatchingDatasetConversionController {
	@Autowired
	FhirContext fhirContext;



	public String post(@RequestBody String stringBundle) {
		Bundle bundle = fhirContext.newJsonParser().parseResource(Bundle.class, stringBundle);
		for (Bundle.BundleEntryComponent entry: bundle.getEntry()) {
			if (entry.getResource() instanceof org.hl7.fhir.r5.model.Patient) {
				org.hl7.fhir.r5.model.Patient fhir = (org.hl7.fhir.r5.model.Patient) entry.getResource();
			}
		}
		return "";
	}



	private Patient convert(org.hl7.fhir.r5.model.Patient patient) {
		Patient mismo = new Patient();
		mismo.setPatientId(Math.toIntExact(patient.getIdElement().getIdPartAsLong()));


		if (patient.hasName()) {
			HumanName humanName = patient.getNameFirstRep();
			mismo.setNameFirst(humanName.getGivenAsSingleString());
			String[] family = humanName.getFamily().split("-");
			if (family.length > 1){
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
		mismo.setGender(patient.getGender().toCode());

		Identifier ssn = MappingHelper.filterIdentifier(patient.getIdentifier(),SSN);
		if (ssn != null) {
			mismo.setSsn(ssn.getValue());
		}


		if (patient.hasAddress()) {
			Address address = patient.getAddressFirstRep();
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
				Address address2 = patient.getAddress().get(1);
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
		IBaseExtension motherMaiden = ExtensionUtil.getExtensionByUrl(patient,MOTHER_MAIDEN_NAME);
		if (motherMaiden != null) {
			mismo.setMotherMaidenName(motherMaiden.getValue().toString());
		}
//
//		IBaseExtension linkedWith = ExtensionUtil.getExtensionByUrl(patient,LINK_ID);
//		if (linkedWith != null) {
//			mismo.setLinkWith(linkedWith.getValue().toString());
//		}

		Identifier mrn = MappingHelper.filterIdentifier(patient.getIdentifier(),""); //TODO change
		if (mrn != null) {
			mismo.setMrns(mrn.getValue());
		}
		for (ContactPoint telecom : patient.getTelecom()) {
			if (null != telecom.getSystem()) {
				if (telecom.getSystem().equals(ContactPoint.ContactPointSystem.PHONE) && StringUtils.isNotEmpty(mismo.getPhone())){
					mismo.setPhone(telecom.getValue());
				} else if (telecom.getSystem().equals(ContactPoint.ContactPointSystem.PHONE) && StringUtils.isNotEmpty(mismo.getPhone())){
					mismo.setValue("phone2",telecom.getValue());
				} else if (telecom.getSystem().equals(ContactPoint.ContactPointSystem.EMAIL)) {
					mismo.setValue("email",telecom.getValue());
				}
			}
		}

		mismo.setBirthOrder(patient.getMultipleBirthIntegerType().getValueAsString());

		return mismo;
	}

//	private Patient convertFromMaster(PatientMaster pm) {
//		Patient m = new Patient();
//		m.setNameFirst(pm.getNameFirst());
//		m.setNameLast(pm.getNameLast());
//		m.setNameMiddle(pm.getNameMiddle());
//		m.setNameMiddle(pm.getName());
//
//	}



}
