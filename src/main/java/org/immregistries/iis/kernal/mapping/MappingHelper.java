package org.immregistries.iis.kernal.mapping;


import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r5.model.*;

import java.text.SimpleDateFormat;
import java.util.List;

public class MappingHelper {

	public static final String PATIENT = "Patient";
	public static final String IMMUNIZATION = "Immunization";
	public static final String OBSERVATION = "Observation";
	public static final String ORGANISATION = "Organisation";
	public static final String LOCATION = "Location";
	public static final String PERSON = "Person";

	public static final String PERSON_MODEL = "Person";
	public static final String PATIENT_REPORTED = "PatientReported";
	public static final String PATIENT_MASTER = "PatientMaster";
	public static final String VACCINATION_MASTER = "VaccinationMaster";
	public static final String VACCINATION_REPORTED = "VaccinationReported";
	public static final String OBSERVATION_MASTER = "ObservationMaster";
	public static final String OBSERVATION_REPORTED = "ObservationReported";
	public static final String ORG_LOCATION = "OrgLocation";
	public static final SimpleDateFormat sdf = new SimpleDateFormat("E MMM dd HH:mm:ss yyyy");

	//TODO choose system id or not
	public  static Reference getFhirReference(String fhirType, String dbType, String identifier) {
//		if (identifier == null || identifier.equals("") ) {
//			throw new RuntimeException();
//		}
		return new Reference()
			.setType(fhirType)
			.setIdentifier(getFhirIdentifier(dbType,identifier));
	}

	public  static Identifier getFhirIdentifier(String dbType, String identifier) {
		return new Identifier()
				.setSystem(dbType)
				.setValue(identifier);
	}

	public  static Identifier filterIdentifier(List<Identifier> identifiers, String system) {
		return identifiers.stream().filter(identifier -> identifier.getSystem().equals(system)).findFirst().get();
	}

	public  static Coding filterCodeableConcept(CodeableConcept concept, String system) {
		return filterCodingList(concept.getCoding(), system);
	}
	public  static Coding filterCodingList(List<Coding> codings, String system) {
		return codings.stream().filter(coding -> coding.getSystem().equals(system)).findFirst().get();
	}

	public static IBaseParameters resourceToPatch(Resource resource) {
		Parameters patch = new Parameters();
//		Parameters.ParametersParameterComponent operation = patch.addParameter();
//		operation.setName("operation");
//		operation
//			.addPart()
//			.setName("type")
//			.setValue(new CodeType("delete"));
//		operation
//			.addPart()
//			.setName("path")
//			.setValue(new StringType("Patient.identifier[0]"));
		return patch;
	}


}
