package org.immregistries.iis.kernal.mapping.interfaces;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;

/**
 * Interface for mapping patient
 *
 * @param <Patient> FHIR Resource type
 */
public interface PatientMapper<Patient extends IBaseResource> extends IisFhirMapperMasterReported<PatientMaster, PatientReported, Patient> {

	String MRN_SYSTEM = "AIRA-TEST";

	String MOTHER_MAIDEN_NAME = "http://hl7.org/fhir/StructureDefinition/patient-mothersMaidenName";
	String SSN = "http://hl7.org/fhir/sid/us-ssn";
	String LINK_ID = "http://codi.mitre.org/link_id";

	String REGISTRY_STATUS_EXTENSION = "registryStatus";
	String REGISTRY_STATUS_INDICATOR = "http://terminology.hl7.org/ValueSet/v2-0441";
	String ETHNICITY_EXTENSION = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity";
	String ETHNICITY_EXTENSION_DETAILED = "detailed";
	String ETHNICITY_EXTENSION_OMB = "ombCategory";
	String ETHNICITY_EXTENSION_TEXT = "text";
	String ETHNICITY_SYSTEM = "urn:oid:2.16.840.1.113883.6.238";
	String RACE_EXTENSION = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-race";
	String RACE_EXTENSION_DETAILED = "detailed";
	String RACE_EXTENSION_OMB = "ombCategory";
	String RACE_EXTENSION_TEXT = "text";
	String RACE_SYSTEM = "urn:oid:2.16.840.1.113883.6.238";
	String RACE_SYSTEM_OMB = "http://hl7.org/fhir/us/core/ValueSet/omb-ethnicity-category";
	String RACE_SYSTEM_DETAILED = "http://hl7.org/fhir/us/core/ValueSet/detailed-race";

	String PUBLICITY_EXTENSION = "publicity";
	String PUBLICITY_SYSTEM = "http://terminology.hl7.org/ValueSet/v2-0215";
	String PROTECTION_EXTENSION = "protection";
	String PROTECTION_SYSTEM = "http://terminology.hl7.org/ValueSet/v2-0136";
	String YES = "Y";
	String NO = "N";
	String MALE_SEX = "M";
	String FEMALE_SEX = "F";

	String V_2_NAME_TYPE = "v2-name-type";
	String V_2_NAME_TYPE_SYSTEM = "http://terminology.hl7.org/CodeSystem/v2-0200";

	String RELATIONSHIP_SYSTEM = "";

	/**
	 * Translates from FHIR to reconstruct reported patient, fetching master patient for referencing
	 *
	 * @param patient FHIR patient resource
	 * @return Mapped internal model Patient as reported patient, with
	 */
	PatientReported localObjectReportedWithMaster(Patient patient);

	/**
	 * Translates from FHIR to reconstruct reported patient object
	 * @param patient FHIR patient Resource
	 * @return Mapped internal model Patient as reported patient
	 */
	PatientReported localObjectReported(Patient patient);

	/**
	 *
	 * @param patient FHIR patient Resource
	 * @return Mapped internal model patient as master patient
	 */
	PatientMaster localObject(Patient patient);

	/**
	 * Converts local model patient information to FHIR Resource
	 * @param patientMaster any local patient record
	 * @return
	 */
	Patient fhirResource(PatientMaster patientMaster);
}
