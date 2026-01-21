package org.immregistries.iis.kernal.controllers.rest;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.Bundle;
import org.immregistries.iis.kernal.logic.PatientMismoConversionService;
import org.immregistries.iis.kernal.mapping.requesters.FhirSearchRequester;
import org.immregistries.iis.kernal.security.CurrentTenantUtil;
import org.immregistries.mismo.match.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/patientMatchingDatasetConversion")
public class PatientMatchingDatasetConversionController {
	@Autowired
	private FhirContext fhirContext;
	@Autowired
	private FhirSearchRequester fhirSearchRequester;
	@Autowired
	private PatientMismoConversionService patientMismoConversionService;

	@PostMapping("/init")
	public String initBuilder() throws IOException {
		String tenantId = CurrentTenantUtil.getTenant().getOrganizationName();
		tenantId = tenantId.strip().replace("/", "");
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
					Patient patient = patientMismoConversionService.convertFromR5IncludingLink(fhirPatient);
					patientList.add(patient);
					break;
				}
			}
		} else if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R4)) {
			org.hl7.fhir.r4.model.Bundle bundle = fhirContext.newJsonParser().parseResource(org.hl7.fhir.r4.model.Bundle.class, stringBundle);
			for (org.hl7.fhir.r4.model.Bundle.BundleEntryComponent entry : bundle.getEntry()) {
				if (entry.getResource() instanceof org.hl7.fhir.r4.model.Patient) {
					org.hl7.fhir.r4.model.Patient fhirPatient = (org.hl7.fhir.r4.model.Patient) entry.getResource();
					Patient patient = patientMismoConversionService.convertFromR4IncludingLink(fhirPatient);
					patientList.add(patient);
				}
			}
		}

		String tenantId = CurrentTenantUtil.getTenant().getOrganizationName();
		tenantId = tenantId.strip().replace("/", "");
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
			IBundleProvider bundleProvider = fhirSearchRequester.searchRegularRecord(org.hl7.fhir.r5.model.Patient.class, new SearchParameterMap());
			for (IBaseResource iBaseResource : bundleProvider.getAllResources()) {
				if (iBaseResource instanceof org.hl7.fhir.r5.model.Patient) {
					Patient patient = patientMismoConversionService.convertFromR5IncludingLink((org.hl7.fhir.r5.model.Patient) iBaseResource);
					list.add(patient);
				}
			}
		} else if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R4)) {
			IBundleProvider bundleProvider = fhirSearchRequester.searchRegularRecord(org.hl7.fhir.r4.model.Patient.class, new SearchParameterMap());
			for (IBaseResource iBaseResource : bundleProvider.getAllResources()) {
				if (iBaseResource instanceof org.hl7.fhir.r4.model.Patient) {
					Patient patient = patientMismoConversionService.convertFromR4IncludingLink((org.hl7.fhir.r4.model.Patient) iBaseResource);
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
		//			.map(this::escapeSpecialCharacters)
		return String.join(",", line);
	}
}
