package org.immregistries.iis.kernal.mapping.mappers.resources;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.immregistries.iis.kernal.model.IisRecommendation;
import org.immregistries.vfa.connect.model.ForecastActual;

public abstract class RecommendationMapper<ImmunizationRecommendation extends IAnyResource, Component extends IBaseBackboneElement>
		implements IisResourceMapper<IisRecommendation, ImmunizationRecommendation> {

	public Class<IisRecommendation> localType() {
		return IisRecommendation.class;
	}

	public String fhirTypeName() {
		return IMMUNIZATION_RECOMMENDATION;
	}

	public static final String IMMUNIZATION_RECOMMENDATION = "ImmunizationRecommendation";

	public abstract Component recommendationComponent(ForecastActual forecastActual);

}
