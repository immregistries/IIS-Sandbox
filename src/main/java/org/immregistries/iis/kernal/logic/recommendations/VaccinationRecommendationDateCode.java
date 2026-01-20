package org.immregistries.iis.kernal.logic.recommendations;

public enum VaccinationRecommendationDateCode implements IisEnum {
	EARLIEST("30981-5", "Earliest date to give"),
	DUE("30980-7", "Date vaccine due"),
	LATEST("59777-3", "Latest date to give immunization"),
	OVERDUE("59778-1", "Date when overdue for immunization");

	private final String code;
	private final String label;

	VaccinationRecommendationDateCode(String code, String label) {
		this.code = code;
		this.label = label;
	}


	public String getCode() {
		return code;
	}

	public String getLabel() {
		return label;
	}

	public String getSystem() {
		return "http://hl7.org/fhir/ValueSet/immunization-recommendation-date-criterion";
	}

	public org.hl7.fhir.r4.model.Coding toR4() {
		return new org.hl7.fhir.r4.model.Coding(this.getSystem(), this.getCode(), this.getLabel());
	}

	public org.hl7.fhir.r5.model.Coding toR5() {
		return new org.hl7.fhir.r5.model.Coding(this.getSystem(), this.getCode(), this.getLabel());
	}

	public static VaccinationRecommendationDateCode fromCode(String code) {
		for (VaccinationRecommendationDateCode criterion : VaccinationRecommendationDateCode.values()) {
			if (criterion.getCode().equals(code)) {
				return criterion;
			}
		}
		return null; // Or throw an exception if the code is not found
	}
}
