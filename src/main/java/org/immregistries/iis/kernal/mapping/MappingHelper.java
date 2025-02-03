package org.immregistries.iis.kernal.mapping;


import org.apache.commons.lang3.StringUtils;
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
	public static Reference getFhirReferenceR5(String fhirType, String dbType, String identifier) {
		if (StringUtils.isBlank(identifier)) {
			return null;
		} else {
			return new Reference()
				.setType(fhirType)
				.setIdentifier(getFhirIdentifierR5(dbType, identifier));
		}
	}

	public static Reference getFhirReferenceR5(String fhirType, String dbType, String identifier, String fhirId) {
		return new Reference(fhirType + "/" + fhirId)
			.setType(fhirType)
			.setIdentifier(getFhirIdentifierR5(dbType, identifier));
	}

	public static CodeableReference getFhirCodeableReferenceR5(String fhirType, String dbType, String identifier) {
		if (StringUtils.isBlank(identifier)) {
			return null;
		} else {
			return new CodeableReference(getFhirReferenceR5(fhirType, dbType, identifier));
		}
	}

	public static org.hl7.fhir.r4.model.Reference getFhirReferenceR4(String fhirType, String dbType, String identifier, String fhirId) {
		if (StringUtils.isBlank(identifier)) {
			return null;
		} else {
			return new org.hl7.fhir.r4.model.Reference(fhirType + "/" + fhirId)
				.setType(fhirType)
				.setIdentifier(getFhirIdentifierR4(dbType, identifier));
		}
	}

	public static org.hl7.fhir.r4.model.Reference getFhirReferenceR4(String fhirType, String dbType, String identifier) {
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

	public static Identifier getFhirIdentifierR5(String dbType, String identifier) {
		return new Identifier()
			.setSystem(dbType)
			.setValue(identifier);
	}

	public static org.hl7.fhir.r4.model.Identifier getFhirIdentifierR4(String dbType, String identifier) {
		return new org.hl7.fhir.r4.model.Identifier()
			.setSystem(dbType)
			.setValue(identifier);
	}

	public static String identifierToString(List<Identifier> identifiers) {
		if (identifiers.size() > 0) {
			return identifiers.get(0).getSystem() + "|" + identifiers.get(0).getValue();
		} else {
			return "";
		}
	}

//	public  static CanonicalIdentifier filterIdentifier(List<CanonicalIdentifier> identifiers, String system) {
//		return identifiers.stream().filter(identifier -> identifier.getSystemElement().getValue() != null && identifier.getSystemElement().getValue().equals(system)).findFirst().orElse(identifiers.get(0));
//	}

	public static Identifier filterIdentifierR5(List<Identifier> identifiers, String system) {
		return identifiers.stream().filter(identifier -> identifier.getSystem() != null && identifier.getSystem().equals(system)).findFirst().orElse(null);
	}

	public static org.hl7.fhir.r4.model.Identifier filterIdentifierR4(List<org.hl7.fhir.r4.model.Identifier> identifiers, String system) {
		return identifiers.stream().filter(identifier -> identifier.getSystem() != null && identifier.getSystem().equals(system)).findFirst().orElse(null);
	}

	public static Identifier filterIdentifierTypeR5(List<Identifier> identifiers, String type) {
		return identifiers.stream().filter(identifier -> identifier.hasType() && identifier.getType().hasCoding() && identifier.getType().getCodingFirstRep().getCode().equals(type)).findFirst().orElse(null);
	}

	public static org.hl7.fhir.r4.model.Identifier filterIdentifierTypeR4(List<org.hl7.fhir.r4.model.Identifier> identifiers, String type) {
		return identifiers.stream().filter(identifier -> identifier.hasType() && identifier.getType().hasCoding() && identifier.getType().getCodingFirstRep().getCode().equals(type)).findFirst().orElse(null);
	}

	public static org.hl7.fhir.r4.model.Coding filterCodeableConceptR4(org.hl7.fhir.r4.model.CodeableConcept concept, String system) {
		return filterCodingListR4(concept.getCoding(), system);
	}

	public static org.hl7.fhir.r4.model.Coding filterCodingListR4(List<org.hl7.fhir.r4.model.Coding> codings, String system) {
		return codings.stream().filter(coding -> coding.getSystem().equals(system)).findFirst().orElse(null);
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

}
