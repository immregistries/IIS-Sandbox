package org.immregistries.iis.kernal.logic.recommendations;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.immregistries.iis.kernal.model.IisPatient;
import org.immregistries.iis.kernal.persisted.model.Tenant;

import java.util.Date;

/**
 * Implements Immunization Recommendation functionalities
 *
 * @param <ImmunizationRecommendation> FHIR class
 * @param <Patient>                    FHIR class
 */
public interface IImmunizationRecommendationService<ImmunizationRecommendation extends IAnyResource, Patient extends IDomainResource> {

	MethodOutcome updateRecommendation(IGenericClient fhirClient, IAnyResource recommendation);

	ImmunizationRecommendation readRecommendation(String recommendationId, String recommendationIdentifier, IGenericClient fhirClient);

	ImmunizationRecommendation getPatientRecommendation(IGenericClient fhirClient, IAnyResource patient);

}
