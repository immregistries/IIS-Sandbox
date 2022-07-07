package org.immregistries.iis.kernal.mapping;

import ca.uhn.fhir.jpa.patch.FhirPatch;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;

import java.util.List;

public class MappingHelper {

	public  static Reference getFhirReference(String fhirType, String dbType, String identifier) {
		return new Reference(
//			fhirType + "?identifier=" + dbType + "|" + identifier
		).setType(fhirType)
			.setIdentifier(getFhirIdentifier(dbType,identifier));
	}

	public  static Identifier getFhirIdentifier(String dbType, String identifier) {
		return new Identifier()
				.setSystem(dbType)
				.setValue(identifier);
	}

	public  static Identifier filterIdentifier(List<Identifier> identifiers, String system) {
		return identifiers.stream().filter(identifier -> identifier.getSystem().equals(system)).findAny().get();
	}

	public  static Coding filterCodeableConcept(CodeableConcept concept, String system) {
		return filterCodingList(concept.getCoding(), system);
	}
	public  static Coding filterCodingList(List<Coding> codings, String system) {
		return codings.stream().filter(coding -> coding.getSystem().equals(system)).findAny().get();
	}

//	public  static void savePatientReported(IGenericClient fhirClient, PatientMaster pm, PatientReported pr) {
//		Patient patient = PatientHandler.getFhirPatient(pm,pr);
//		try {
//			fhirClient.update().resource(patient).conditional().where(
//					Patient.IDENTIFIER.exactly().systemAndIdentifier("PatientReported",pr.getPatientReportedId()))
//				.execute();
//		} catch (ResourceNotFoundException e){
//			fhirClient.create().resource(patient)
//				.execute();
//		}
//	}
//	public  static void savePatientMaster(IGenericClient fhirClient, PatientMaster pm) {
//		Patient patient = PatientHandler.getFhirPatient(pm,null);
//		try {
//			fhirClient.update().resource(patient).conditional().where(
//					Patient.IDENTIFIER.exactly().systemAndIdentifier("PatientMaster",pm.getPatientId()))
//				.execute();
//		} catch (ResourceNotFoundException e){
//			fhirClient.create().resource(patient)
//				.execute();
//		}
//	}

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
