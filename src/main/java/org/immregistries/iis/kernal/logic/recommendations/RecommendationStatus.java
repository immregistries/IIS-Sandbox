package org.immregistries.iis.kernal.logic.recommendations;

public enum RecommendationStatus implements IisEnum {

	DUE(
		"due",
		"The patient is due for their next vaccination."
	),
	OVERDUE(
		"overdue",
		"The patient is considered overdue for their next vaccination."
	),
	IMMUNE(
		"immune",
		"The patient is immune to the target disease and further immunization against the disease is not likely to provide benefit."
	),
	CONTRAINDICATED(
		"contraindicated",
		"The patient is contraindicated for futher doses."
	),
	COMPLETE(
		"complete",
		"The patient is fully protected and no further doses are recommended."
	);

	public static final String SYSTEM = "http://terminology.hl7.org/CodeSystem/immunization-recommendation-status";
	private final String code;
	private final String label;

	RecommendationStatus(String code, String label) {
		this.code = code;
		this.label = label;
	}

	public String getCode() {
		return code;
	}

	public String getSystem() {
		return SYSTEM;
	}

	public String getLabel() {
		return label;
	}

	public org.hl7.fhir.r4.model.Coding toR4() {
		return new org.hl7.fhir.r4.model.Coding(this.getSystem(), this.getCode(), this.getLabel());
	}

	public org.hl7.fhir.r5.model.Coding toR5() {
		return new org.hl7.fhir.r5.model.Coding(this.getSystem(), this.getCode(), this.getLabel());
	}


	/**
	 * Helper method to find an enum by its string code.
	 */
	public static RecommendationStatus fromCode(String code) {
		for (RecommendationStatus status : RecommendationStatus.values()) {
			if (status.code.equalsIgnoreCase(code)) {
				return status;
			}
		}
		throw new IllegalArgumentException("Unknown code: " + code);
	}
}