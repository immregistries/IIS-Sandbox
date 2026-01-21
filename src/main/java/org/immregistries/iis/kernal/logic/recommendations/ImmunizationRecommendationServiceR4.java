package org.immregistries.iis.kernal.logic.recommendations;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.*;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.logic.CodeMapManagerService;
import org.immregistries.iis.kernal.logic.v2.handling.IncomingQueryHandler;
import org.immregistries.iis.kernal.mapping.mappers.fields.r4.BusinessIdentifierMapperR4;
import org.immregistries.iis.kernal.mapping.mappers.resources.r4.ImmunizationEvaluationMapperR4;
import org.immregistries.iis.kernal.mapping.mappers.resources.r4.ImmunizationRecommendationMapperR4;
import org.immregistries.iis.kernal.mapping.requesters.FhirSearchRequester;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.vfa.connect.model.ForecastActual;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.immregistries.iis.kernal.fhir.immds.IRecommendationForecastProvider.EVALUATION;
import static org.immregistries.iis.kernal.fhir.immds.IRecommendationForecastProvider.RECOMMENDATION;

@Service
@Conditional(OnR4Condition.class)
public class ImmunizationRecommendationServiceR4
		implements IImmunizationRecommendationService<ImmunizationRecommendation, Patient> {

	public MethodOutcome updateRecommendation(IGenericClient fhirClient, IAnyResource recommendation) {
		return fhirClient.update().resource(recommendation).withId(recommendation.getId()).execute();
	}

	public ImmunizationRecommendation readRecommendation(String recommendationId, String recommendationIdentifier, IGenericClient fhirClient) {
		ImmunizationRecommendation recommendation = null;
		if (recommendationId != null) {
			recommendation = fhirClient.read().resource(ImmunizationRecommendation.class)
				.withId(recommendationId).execute();
		} else if (recommendationIdentifier != null) {
			Bundle recommendationBundle = fhirClient.search()
				.forResource(ImmunizationRecommendation.class).where(
					Patient.IDENTIFIER.exactly().identifier(recommendationIdentifier))
				.returnBundle(Bundle.class).execute();
			if (recommendationBundle.hasEntry()) {
				recommendation = (ImmunizationRecommendation) recommendationBundle
					.getEntryFirstRep().getResource();
			}
		}
		return recommendation;
	}

	public @Nullable ImmunizationRecommendation getPatientRecommendation(IGenericClient fhirClient, IAnyResource patient) {
		ImmunizationRecommendation recommendation = null;
		IBaseBundle baseBundle = fhirClient.search().forResource(ImmunizationRecommendation.class)
			.where(ImmunizationRecommendation.PATIENT
				.hasId(new IdType(patient.getId()).getIdPart()))
			.execute();
		Bundle recommendationBundle = (Bundle) baseBundle;
		if (recommendationBundle.hasEntry()) {
			recommendation = (ImmunizationRecommendation) recommendationBundle
				.getEntryFirstRep().getResource();
		}
		return recommendation;
	}

}
