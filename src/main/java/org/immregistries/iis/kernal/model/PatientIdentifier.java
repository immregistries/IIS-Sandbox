package org.immregistries.iis.kernal.model;

public class PatientIdentifier {
	public static final String MRN_TYPE_SYSTEM = "http://terminology.hl7.org/CodeSystem/v2-0203";
	public static final String MRN_TYPE_VALUE = "MR";
	private String system = "";
	private String value = "";
	private String type = "";
	private String assignerReference = "";

	public PatientIdentifier() {
	}

	private PatientIdentifier(org.hl7.fhir.r5.model.Identifier identifierR5) {
		system = identifierR5.getSystem();
		value = identifierR5.getValue();
		type = identifierR5.getType().getCode(MRN_TYPE_SYSTEM);
	}

	private PatientIdentifier(org.hl7.fhir.r4.model.Identifier identifierR4) {
		system = identifierR4.getSystem();
		value = identifierR4.getValue();
		type = identifierR4.getType().getCodingFirstRep().getCode();
	}

	public String getSystem() {
		return system;
	}

	public void setSystem(String system) {
		this.system = system;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getAssignerReference() {
		return assignerReference;
	}

	public void setAssignerReference(String assignerReference) {
		this.assignerReference = assignerReference;
	}

	public org.hl7.fhir.r5.model.Identifier toR5() {
		org.hl7.fhir.r5.model.Identifier identifier = new org.hl7.fhir.r5.model.Identifier().setValue(value).setSystem(system);
		if (this.getType() != null) {
			identifier.setType(new org.hl7.fhir.r5.model.CodeableConcept(
				new org.hl7.fhir.r5.model.Coding(MRN_TYPE_SYSTEM, MRN_TYPE_VALUE, "")));
		}
		return identifier;
	}

	public org.hl7.fhir.r4.model.Identifier toR4() {
		org.hl7.fhir.r4.model.Identifier identifier = new org.hl7.fhir.r4.model.Identifier().setValue(value).setSystem(system);
		if (this.getType() != null) {
			identifier.setType(new org.hl7.fhir.r4.model.CodeableConcept(
				new org.hl7.fhir.r4.model.Coding(MRN_TYPE_SYSTEM, MRN_TYPE_VALUE, "")));
		}
		return identifier;
	}

	public static PatientIdentifier fromR5(org.hl7.fhir.r5.model.Identifier identifier) {
		return new PatientIdentifier(identifier);
	}

	public static PatientIdentifier fromR4(org.hl7.fhir.r4.model.Identifier identifier) {
		return new PatientIdentifier(identifier);
	}

	@Override
	public String toString() {
		return "PatientIdentifier{" +
			"system='" + system + '\'' +
			", value='" + value + '\'' +
			", type='" + type + '\'' +
			", assignerReference='" + assignerReference + '\'' +
			'}';
	}
}
