package org.immregistries.iis.kernal.mapping.resourceMappers;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.model.IisRecommendation;

public abstract class RecommendationMapper<ImmunizationRecommendation extends IAnyResource>
		implements IisResourceMapper<IisRecommendation, ImmunizationRecommendation> {

	public Class<IisRecommendation> localType() {
		return IisRecommendation.class;
	}

	public String fhirResourceName() {
		return IMMUNIZATION_RECOMMENDATION;
	}

	public static final String IMMUNIZATION_RECOMMENDATION = "ImmunizationRecommendation";

}
