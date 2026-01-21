package org.immregistries.iis.kernal.model;

import java.util.Objects;

public class BusinessIdentifier extends IisMappedToFhir {
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

	public BusinessIdentifier(String value) {
		this.value = value;
	}

	public BusinessIdentifier(String system, String value) {
		this.system = system;
		this.value = value;
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
		if (o == null || getClass() != o.getClass())
			return false;
		BusinessIdentifier that = (BusinessIdentifier) o;
		return Objects.equals(system, that.system) && Objects.equals(value, that.value)
				&& Objects.equals(type, that.type) && Objects.equals(assignerReference, that.assignerReference);
	}

	@Override
	public int hashCode() {
		return Objects.hash(system, value, type, assignerReference);
	}
}
