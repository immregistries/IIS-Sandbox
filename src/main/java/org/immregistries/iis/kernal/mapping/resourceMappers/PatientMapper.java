package org.immregistries.iis.kernal.mapping.resourceMappers;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.mapping.internalClient.FhirReadRequester;
import org.immregistries.iis.kernal.mapping.internalClient.FhirRequesterUtil;
import org.immregistries.iis.kernal.model.IisPatient;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Interface for mapping patient
 *
 * @param <Patient> FHIR Resource type
 */
public abstract class PatientMapper<Patient extends IAnyResource>
	implements IisResourceMasterReportedMapper<PatientMaster, PatientReported, IisPatient, Patient> {

	public String fhirType() {
		return PATIENT;
	}

	public static final String PATIENT = "Patient";

	public Class<IisPatient> localType() {
		return IisPatient.class;
	}

	public Class<PatientMaster> localMasterType() {
		return PatientMaster.class;
	}

	public Class<PatientReported> localReportedType() {
		return PatientReported.class;
	}

	public static final String MRN_SYSTEM = "AIRA-TEST";

	public static final String MOTHER_MAIDEN_NAME = "http://hl7.org/fhir/StructureDefinition/patient-mothersMaidenName";
	public static final String SSN = "http://hl7.org/fhir/sid/us-ssn";
	public static final String LINK_ID = "http://codi.mitre.org/link_id";

	public static final String REGISTRY_STATUS_EXTENSION = "registryStatus";
	public static final String REGISTRY_STATUS_INDICATOR = "http://terminology.hl7.org/ValueSet/v2-0441";
	public static final String ETHNICITY_EXTENSION = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity";
	public static final String ETHNICITY_EXTENSION_DETAILED = "detailed";
	public static final String ETHNICITY_EXTENSION_OMB = "ombCategory";
	public static final String ETHNICITY_EXTENSION_TEXT = "text";
	public static final String ETHNICITY_SYSTEM = "urn:oid:2.16.840.1.113883.6.238";
	public static final String ETHNICITY_SYSTEM_OMB = "http://hl7.org/fhir/us/core/ValueSet/omb-ethnicity-category";
	public static final String ETHNICITY_SYSTEM_DETAILED = "http://hl7.org/fhir/us/core/ValueSet/detailed-ethnicity";

	public static final String RACE_EXTENSION = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-race";
	public static final String RACE_EXTENSION_DETAILED = "detailed";
	public static final String RACE_EXTENSION_OMB = "ombCategory";
	public static final String RACE_EXTENSION_TEXT = "text";
	public static final String RACE_SYSTEM = "urn:oid:2.16.840.1.113883.6.238";
	public static final String RACE_SYSTEM_OMB = "http://hl7.org/fhir/us/core/ValueSet/omb-race-category";
	public static final String RACE_SYSTEM_DETAILED = "http://hl7.org/fhir/us/core/ValueSet/detailed-race";

	public static final String PUBLICITY_EXTENSION = "publicity";
	public static final String PUBLICITY_SYSTEM = "http://terminology.hl7.org/ValueSet/v2-0215";
	public static final String PROTECTION_EXTENSION = "protection";
	public static final String PROTECTION_SYSTEM = "http://terminology.hl7.org/ValueSet/v2-0136";
	public static final String YES = "Y";
	public static final String NO = "N";
	public static final String MALE_SEX = "M";
	public static final String FEMALE_SEX = "F";
	public static final String UNKNOWN_SEX = "U";
	public static final String OTHER_SEX = "O";

	public static final String V_2_NAME_TYPE = "v2-name-type";
	public static final String V_2_NAME_TYPE_SYSTEM = "http://terminology.hl7.org/CodeSystem/v2-0200";

	public static final String RELATIONSHIP_SYSTEM = "";

	@Autowired
	FhirReadRequester fhirReadRequester;

	/**
	 * Translates from FHIR to reconstruct reported patient, fetching master patient
	 * for referencing
	 *
	 * @param patient FHIR patient resource
	 * @return Mapped internal model Patient as reported patient, with
	 */
	public PatientReported localObjectReportedWithMaster(Patient patient) {
		PatientReported patientReported = localObjectReported(patient);
		if (FhirRequesterUtil.isNotGoldenRecord(patient)) {
			patientReported.setMasterRecord(fhirReadRequester.readPatientMasterWithMdmLink(patient.getId()));
		}
		return patientReported;
	}


	/**
	 *
	 * @param patient FHIR patient Resource
	 * @return Mapped internal model patient as master patient
	 */
	public IisPatient localObject(Patient patient) {
		IisPatient iisPatient = new IisPatient();
		fillFromFhirResource(iisPatient, patient);
		return iisPatient;
	}

	/**
	 * Translates from FHIR to reconstruct master patient object
	 *
	 * @param patient FHIR patient Resource
	 * @return Mapped internal model Patient as Master patient
	 */
	public PatientMaster localObjectMaster(Patient patient) {
		PatientMaster patientMaster = new PatientMaster();
		if (FhirRequesterUtil.isGoldenRecord(patient)) {
			return null;
		}
		fillFromFhirResource(patientMaster, patient);
		return patientMaster;
	}

	/**
	 * Translates from FHIR to reconstruct reported patient object
	 *
	 * @param patient FHIR patient Resource
	 * @return Mapped internal model Patient as reported patient
	 */
	public PatientReported localObjectReported(Patient patient) {
		PatientReported patientReported = new PatientReported();
		if (FhirRequesterUtil.isNotGoldenRecord(patient)) {
			return null;
		}
		fillFromFhirResource(patientReported, patient);
		return patientReported;
	}

	/**
	 * Converts local model patient information to FHIR Resource
	 *
	 * @param iisPatient any local patient record
	 * @return
	 */
	abstract public Patient fhirResource(IisPatient iisPatient);


	public abstract void fillFromFhirResource(IisPatient localPatient, Patient patient);

}
