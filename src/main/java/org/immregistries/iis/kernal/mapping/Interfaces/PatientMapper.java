package org.immregistries.iis.kernal.mapping.Interfaces;

import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;

public interface PatientMapper<Patient> {

	String MRN_SYSTEM = "sabbia:mrns";

	String MOTHER_MAIDEN_NAME = "http://hl7.org/fhir/StructureDefinition/patient-mothersMaidenName";
	String REGISTRY_STATUS_EXTENSION = "registryStatus";
	String REGISTRY_STATUS_INDICATOR = "registryStatusIndicator";
	String ETHNICITY_EXTENSION = "ethnicity";
	String ETHNICITY_SYSTEM = "ethnicity";
	String RACE = "race";
	String RACE_SYSTEM = "race";
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

	Patient getFhirResource(PatientReported pr);
}
