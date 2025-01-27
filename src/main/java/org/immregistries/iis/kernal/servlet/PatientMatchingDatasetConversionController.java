package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.util.ExtensionUtil;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.mapping.internalClient.FhirRequester;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.mismo.match.StringUtils;
import org.immregistries.mismo.match.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.immregistries.iis.kernal.mapping.interfaces.PatientMapper.*;

@RestController
@RequestMapping("/patientMatchingDatasetConversion")
public class PatientMatchingDatasetConversionController {
	@Autowired
	FhirContext fhirContext;
	@Autowired
	FhirRequester fhirRequester;

	@PostMapping("/init")
	public String initBuilder() throws IOException {
		String tenantId = ServletHelper.getTenant().getOrganizationName();
		tenantId.strip().replace("/","");
		File csvOutputFile = new File("./target/"+tenantId+".csv");
		csvOutputFile.createNewFile();
		FileWriter fileWriter = new FileWriter(csvOutputFile, false);
		try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
			initCsv(pw);
			pw.flush();
			pw.close();
			pw.close();
			fileWriter.close();
		}
		return "OK";
	}

	@PostMapping("")
	public String post(@RequestBody String stringBundle) throws IOException {
		List<Patient> patientList = new ArrayList<>(20);
		if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
			Bundle bundle = fhirContext.newJsonParser().parseResource(Bundle.class, stringBundle);
			for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
				if (entry.getResource() instanceof org.hl7.fhir.r5.model.Patient) {
					org.hl7.fhir.r5.model.Patient fhirPatient = (org.hl7.fhir.r5.model.Patient) entry.getResource();
					Patient patient = convertFromR5IncludingLink(fhirPatient);
					patientList.add(patient);
					break;
				}
			}
		} else if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R4)) {
			org.hl7.fhir.r4.model.Bundle bundle = fhirContext.newJsonParser().parseResource(org.hl7.fhir.r4.model.Bundle.class, stringBundle);
			for (org.hl7.fhir.r4.model.Bundle.BundleEntryComponent entry : bundle.getEntry()) {
				if (entry.getResource() instanceof org.hl7.fhir.r4.model.Patient) {
					org.hl7.fhir.r4.model.Patient fhirPatient = (org.hl7.fhir.r4.model.Patient) entry.getResource();
					Patient patient = convertFromR4IncludingLink(fhirPatient);
					patientList.add(patient);
				}
			}
		}

		String tenantId = ServletHelper.getTenant().getOrganizationName();
		tenantId.strip().replace("/","");
		File csvOutputFile = new File("./target/"+tenantId+".csv");
		FileWriter fileWriter = new FileWriter(csvOutputFile, true);
		try (PrintWriter pw = new PrintWriter(fileWriter)) {
			printCsvPatientList(patientList, pw);
			pw.flush();
			pw.close();
			fileWriter.close();
		}
		return "";
	}

	@GetMapping("/all")
	public void getFromFacility(HttpServletResponse resp) throws IOException {
		List<Patient> list = new ArrayList<>(20);
		if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
			IBundleProvider bundleProvider = fhirRequester.searchRegularRecord(org.hl7.fhir.r5.model.Patient.class, new SearchParameterMap());
			for (IBaseResource iBaseResource : bundleProvider.getAllResources()) {
				if (iBaseResource instanceof org.hl7.fhir.r5.model.Patient) {
					Patient patient = convertFromR5IncludingLink((org.hl7.fhir.r5.model.Patient) iBaseResource);
					list.add(patient);
				}
			}
		} else if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R4)) {
			IBundleProvider bundleProvider = fhirRequester.searchRegularRecord(org.hl7.fhir.r4.model.Patient.class, new SearchParameterMap());
			for (IBaseResource iBaseResource : bundleProvider.getAllResources()) {
				if (iBaseResource instanceof org.hl7.fhir.r4.model.Patient) {
					Patient patient = convertFromR4IncludingLink((org.hl7.fhir.r4.model.Patient) iBaseResource);
					list.add(patient);
				}
			}
		}
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		initCsv(out);
		printCsvPatientList(list, out);
		out.flush();
		out.close();
	}

	private void initCsv(PrintWriter printWriter) {
		String csvHeader =
			"EnterpriseID," +
				"LAST," +
				"FIRST," +
				"MIDDLE," +
				"SUFFIX," +
				"DOB," +
				"GENDER," +
				"SSN," +
				"ADDRESS1," +
				"ADDRESS2," +
				"ZIP," +
				"MOTHERS_MAIDEN_NAME," +
				"MRN," +
				"CITY," +
				"STATE," +
				"PHONE," +
				"PHONE2," +
				"EMAIL," +
				"ALIAS," +
				"LINK_ID";
		printWriter.println(csvHeader);
	}

	private void printCsvPatientList(List<Patient> patientList, PrintWriter printWriter) {
		patientList.stream()
			.map(this::patientCsvLine)
			.forEach(printWriter::println);
	}

	private String patientCsvLine(Patient patient) {
		String[] line = new String[]{
			patient.getValue("identifier"),
			patient.getNameLast(),
			patient.getNameFirst(),
			patient.getNameMiddle(),
			patient.getNameSuffix(),
			patient.getBirthDate(),
			patient.getGender(),
			patient.getSsn(),
			patient.getAddress1().getLine1(),
			patient.getAddress1().getLine2(),
			patient.getAddress1().getZip(),
			patient.getMotherMaidenName(),
			patient.getMrns(),
			patient.getAddress1().getCity(),
			patient.getAddress1().getState(),
			patient.getPhone(),
			"", //phone 2
			patient.getValue("email"),
			patient.getNameAlias(),
			"" // LinkWith
		};
		if (patient.getLinkWith() != null) {
			line[line.length-1] = patient.getLinkWith().getValue("identifier");
		}
		return Stream.of(line)
//			.map(this::escapeSpecialCharacters)
			.collect(Collectors.joining(","));
	}

	public Patient convertFromR5(org.hl7.fhir.r5.model.Patient patient) {
		Patient mismo = new Patient();
		org.hl7.fhir.r5.model.Identifier identifier = MappingHelper.filterIdentifierR5(patient.getIdentifier(), "http://codi.mitre.org");
		if (identifier != null) {
			mismo.setValue("identifier",identifier.getValue());
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
		mismo.setGender(patient.getGender().toCode());

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

		if(patient.hasMultipleBirthBooleanType()) {
			mismo.setBirthOrder(patient.getMultipleBirthBooleanType().getValueAsString());
		} else if (patient.hasMultipleBirthIntegerType()) {
			mismo.setBirthOrder(patient.getMultipleBirthIntegerType().getValueAsString());
		}
		return mismo;
	}


	private Patient convertFromR4(org.hl7.fhir.r4.model.Patient patient) {
		Patient mismo = new Patient();
		org.hl7.fhir.r4.model.Identifier identifier = MappingHelper.filterIdentifierR4(patient.getIdentifier(), "http://codi.mitre.org");
		if (identifier != null) {
			mismo.setValue("identifier",identifier.getValue());
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
		mismo.setGender(patient.getGender().toCode());

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

		if(patient.hasMultipleBirthBooleanType()) {
			mismo.setBirthOrder(patient.getMultipleBirthBooleanType().getValueAsString());
		} else if (patient.hasMultipleBirthIntegerType()) {
			mismo.setBirthOrder(patient.getMultipleBirthIntegerType().getValueAsString());
		}
		return mismo;
	}

	private Patient convertFromR4IncludingLink(org.hl7.fhir.r4.model.Patient patient) {
		Patient mismo = convertFromR4(patient);
		org.hl7.fhir.r4.model.Identifier linkedWith = MappingHelper.filterIdentifierR4(patient.getIdentifier(), LINK_ID);
		if (linkedWith != null) {
			Patient link = new Patient();
			link.setValue("identifier",linkedWith.getValue());
			mismo.setLinkWith(link);
//			IBundleProvider bundleProvider = fhirRequester.searchRegularRecord(org.hl7.fhir.r4.model.Patient.class, new SearchParameterMap("identifier", new TokenParam().setSystem(LINK_ID).setValue(linkedWith.getValue())));
//			if (!bundleProvider.isEmpty()) {
//				mismo.setLinkWith(convertFromR4((org.hl7.fhir.r4.model.Patient) bundleProvider.getAllResources().get(0)));
//			}
		}
		return mismo;
	}

	private Patient convertFromR5IncludingLink(org.hl7.fhir.r5.model.Patient patient) {
		Patient mismo = convertFromR5(patient);
		org.hl7.fhir.r5.model.Identifier linkedWith = MappingHelper.filterIdentifierR5(patient.getIdentifier(), LINK_ID);
		if (linkedWith != null) {
			Patient link = new Patient();
			link.setValue("identifier",linkedWith.getValue());
			mismo.setLinkWith(link);
//			IBundleProvider bundleProvider = fhirRequester.searchRegularRecord(org.hl7.fhir.r5.model.Patient.class, new SearchParameterMap("identifier", new TokenParam().setSystem(LINK_ID).setValue(linkedWith.getValue())));
//			if (!bundleProvider.isEmpty()) {
//				mismo.setLinkWith(convertFromR5((org.hl7.fhir.r5.model.Patient) bundleProvider.getAllResources().get(0)));
//			}
		}
		return mismo;
	}
}
