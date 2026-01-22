package org.immregistries.iis.kernal.logic.recommendations;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IDomainResource;

/**
 * Implements Immunization Recommendation functionalities
 *
 * @param <ImmunizationRecommendation> FHIR class
 * @param <Patient>                    FHIR class
 */
public interface IImmunizationRecommendationService<ImmunizationRecommendation extends IAnyResource, Patient extends IDomainResource> {

	MethodOutcome updateRecommendation(IGenericClient fhirClient, ImmunizationRecommendation recommendation);

	ImmunizationRecommendation readRecommendation(String recommendationId, String recommendationIdentifier, IGenericClient fhirClient);

	ImmunizationRecommendation getPatientRecommendation(IGenericClient fhirClient, Patient patient);

}
