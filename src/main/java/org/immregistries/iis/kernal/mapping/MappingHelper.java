package org.immregistries.iis.kernal.mapping;


import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r5.model.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MappingHelper {

	public static final String PATIENT = "Patient";
	public static final String IMMUNIZATION = "Immunization";
	public static final String OBSERVATION = "Observation";
	public static final String ORGANIZATION = "Organization";
	public static final String LOCATION = "Location";
	public static final String PERSON = "Person";
	public static final String PRACTITIONER = "Practitioner";

	public static final SimpleDateFormat sdf = new SimpleDateFormat("E MMM dd HH:mm:ss yyyy");

	//TODO choose system id or not
	public static Reference getFhirReference(String fhirType, String dbType, String identifier) {
		if (StringUtils.isBlank(identifier)) {
			return null;
		} else {
			return new Reference()
				.setType(fhirType)
				.setIdentifier(getFhirIdentifier(dbType, identifier));
		}
	}

	public static CodeableReference getFhirCodeableReference(String fhirType, String dbType, String identifier) {
		if (StringUtils.isBlank(identifier)) {
			return null;
		} else {
			return new CodeableReference(getFhirReference(fhirType, dbType, identifier));
		}
	}

	public static org.hl7.fhir.r4.model.Reference getFhirR4Reference(String fhirType, String dbType, String identifier) {
		if (StringUtils.isBlank(identifier)) {
			return null;
		} else {
			return new org.hl7.fhir.r4.model.Reference()
				.setType(fhirType)
				.setIdentifier(new org.hl7.fhir.r4.model.Identifier()
					.setSystem(dbType)
					.setValue(identifier));
		}
	}
	public  static Reference getFhirReference(String fhirType, String dbType, String identifier, String fhirId) {
		return new Reference(fhirType + "/" + fhirId)
			.setType(fhirType)
			.setIdentifier(getFhirIdentifier(dbType,identifier));
	}

	public  static Identifier getFhirIdentifier(String dbType, String identifier) {
		return new Identifier()
				.setSystem(dbType)
				.setValue(identifier);
	}

	public  static org.hl7.fhir.r4.model.Identifier getFhirR4Identifier(String dbType, String identifier) {
		return new org.hl7.fhir.r4.model.Identifier()
				.setSystem(dbType)
				.setValue(identifier);
	}

	public  static String identifierToString(List<Identifier> identifiers) {
		if (identifiers.size() > 0) {
			return identifiers.get(0).getSystem() + "|" + identifiers.get(0).getValue();
		} else {
			return "";
		}
	}

	public  static Identifier filterIdentifier(List<Identifier> identifiers, String system) {
		return identifiers.stream().filter(identifier -> identifier.getSystem() != null && identifier.getSystem().equals(system)).findFirst().orElse(identifiers.get(0));
	}

	public static org.hl7.fhir.r4.model.Identifier filterR4Identifier(List<org.hl7.fhir.r4.model.Identifier> identifiers, String system) {
		return identifiers.stream().filter(identifier -> identifier.getSystem() != null && identifier.getSystem().equals(system)).findFirst().orElse(identifiers.get(0));
	}

	public static Coding filterCodeableConcept(CodeableConcept concept, String system) {
		return filterCodingList(concept.getCoding(), system);
	}

	public static Coding filterCodingList(List<Coding> codings, String system) {
		return codings.stream().filter(coding -> coding.getSystem().equals(system)).findFirst().get();
	}

	public static CodeableConcept extensionGetCodeableConcept(Extension extension) {
		return extension.getValueCodeableConcept();
	}

	public static org.hl7.fhir.r4.model.CodeableConcept extensionGetCodeableConcept(org.hl7.fhir.r4.model.Extension extension) {
		return extension.castToCodeableConcept(extension.getValue());
	}

	public static Coding extensionGetCoding(Extension extension) {
		return extension.getValueCoding();
	}
//	public static String extensionGetCode(Extension extension) {
//		return extension.getValueCodeType().getValue();
//	}

	public static org.hl7.fhir.r4.model.Coding extensionGetCoding(org.hl7.fhir.r4.model.Extension extension) {
		return extension.castToCoding(extension.getValue());
	}

	public static Date extensionGetDate(Extension extension) {
		return extension.getValueDateType().getValue();
	}

	public static Date extensionGetDate(org.hl7.fhir.r4.model.Extension extension) {
		return extension.castToDate(extension.getValue()).getValue();
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
