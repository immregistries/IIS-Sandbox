package org.immregistries.iis.kernal.model;


import ca.uhn.fhir.rest.param.TokenParam;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public class BusinessIdentifier extends AbstractDiffable<BusinessIdentifier> {
	public static final String IDENTIFIER_TYPE_SYSTEM = "http://terminology.hl7.org/CodeSystem/v2-0203";
	public static final String MRN_TYPE_VALUE = "MR";
	public static final String PT_TYPE_VALUE = "PT";
	public static final String FILLER_TYPE_VALUE = "FILL";
	public static final String PLACER_TYPE_VALUE = "PLAC";
	private String system = "";
	private String value = "";
	private String type = "";
	/**
	 * Currently unused
	 */
	private String assignerReference = "";

	public BusinessIdentifier() {
	}

	private BusinessIdentifier(org.hl7.fhir.r5.model.Identifier identifierR5) {
		system = identifierR5.getSystem();
		value = identifierR5.getValue();
		type = identifierR5.getType().getCode(IDENTIFIER_TYPE_SYSTEM);
	}

	private BusinessIdentifier(org.hl7.fhir.r4.model.Identifier identifierR4) {
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
				new org.hl7.fhir.r5.model.Coding(IDENTIFIER_TYPE_SYSTEM, this.getType(), "")));
		}
		return identifier;
	}

	public org.hl7.fhir.r4.model.Identifier toR4() {
		org.hl7.fhir.r4.model.Identifier identifier = new org.hl7.fhir.r4.model.Identifier().setValue(value).setSystem(system);
		if (this.getType() != null) {
			identifier.setType(new org.hl7.fhir.r4.model.CodeableConcept(
				new org.hl7.fhir.r4.model.Coding(IDENTIFIER_TYPE_SYSTEM, this.getType(), "")));
		}
		return identifier;
	}

	/**
	 * Converts to token param with System and value
	 *
	 * @return tokenParam
	 */
	public TokenParam asTokenParam() {
		TokenParam tokenParam = new TokenParam();
		if (StringUtils.isNotBlank(value)) {
			tokenParam.setValue(this.value);
			if (StringUtils.isNotBlank(this.system)) {
				tokenParam.setSystem(this.system);
			}
//		tokenParam.setModifier(TokenParamModifier.OF_TYPE).; TODO TYPE

			return tokenParam;
		}
		return null;
	}

	public static BusinessIdentifier fromR5(org.hl7.fhir.r5.model.Identifier identifier) {
		return new BusinessIdentifier(identifier);
	}

	public static BusinessIdentifier fromR4(org.hl7.fhir.r4.model.Identifier identifier) {
		return new BusinessIdentifier(identifier);
	}

	@Override
	public String toString() {
		return "BusinessIdentifier{" +
			"system='" + system + '\'' +
			", value='" + value + '\'' +
			", type='" + type + '\'' +
			", assignerReference='" + assignerReference + '\'' +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		BusinessIdentifier that = (BusinessIdentifier) o;
		return Objects.equals(system, that.system) && Objects.equals(value, that.value) && Objects.equals(type, that.type) && Objects.equals(assignerReference, that.assignerReference);
	}

	@Override
	public int hashCode() {
		return Objects.hash(system, value, type, assignerReference);
	}
}
