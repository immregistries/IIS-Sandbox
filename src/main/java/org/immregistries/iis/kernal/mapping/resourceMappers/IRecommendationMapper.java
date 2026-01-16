package org.immregistries.iis.kernal.mapping.resourceMappers;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.model.IisRecommendation;

public interface IRecommendationMapper<ImmunizationRecommendation extends IAnyResource> extends IisResourceMasterMapper<IisRecommendation, ImmunizationRecommendation> {

	default Class<IisRecommendation> localType() {
		return IisRecommendation.class;
	}

	default String fhirType() {
		return IMMUNIZATION_RECOMMENDATION;
	}

	String IMMUNIZATION_RECOMMENDATION = "ImmunizationRecommendation";


}
