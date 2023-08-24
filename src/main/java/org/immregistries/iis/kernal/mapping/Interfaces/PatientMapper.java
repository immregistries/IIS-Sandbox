package org.immregistries.iis.kernal.mapping.Interfaces;

import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;

public interface PatientMapper<Patient> {

	String MRN_SYSTEM = "AIRA-TEST";

	String MOTHER_MAIDEN_NAME = "http://hl7.org/fhir/StructureDefinition/patient-mothersMaidenName";
	String REGISTRY_STATUS_EXTENSION = "registryStatus";
	String REGISTRY_STATUS_INDICATOR = "registryStatusIndicator";
	String ETHNICITY_EXTENSION = "ethnicity";
	String ETHNICITY_SYSTEM = "http://terminology.hl7.org/CodeSystem/v3-Ethnicity";
	String RACE = "race";
	String RACE_SYSTEM = "https://terminology.hl7.org/2.0.0/CodeSystem-v3-Race.html";
	String PUBLICITY_EXTENSION = "publicity";
	String PUBLICITY_SYSTEM = "publicityIndicator";
	String PROTECTION_EXTENSION = "protection";
	String PROTECTION_SYSTEM = "protectionIndicator";
	String YES = "Y";
	String NO = "N";
	String MALE_SEX = "M";
	String FEMALE_SEX = "F";

	PatientReported getReportedWithMaster(Patient patient);

	PatientReported getReported(Patient patient);

	PatientMaster getMaster(Patient patient);

	/**
	 * Consvert local model patient information to FHIR Resource
	 * @param patientMaster any local patient record
	 * @return
	 */
	Patient getFhirResource(PatientMaster patientMaster);
}
