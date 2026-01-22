package org.immregistries.iis.kernal.logic.recommendations;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.ImmunizationRecommendation;
import org.hl7.fhir.r5.model.Patient;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Service
@Conditional(OnR5Condition.class)
public class ImmunizationRecommendationServiceR5 implements IImmunizationRecommendationService<ImmunizationRecommendation, Patient> {

	public MethodOutcome updateRecommendation(IGenericClient fhirClient, ImmunizationRecommendation recommendation) {
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

	public @Nullable ImmunizationRecommendation getPatientRecommendation(IGenericClient fhirClient, Patient patient) {
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
