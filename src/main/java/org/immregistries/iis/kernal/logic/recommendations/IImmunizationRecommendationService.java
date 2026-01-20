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

	/**
	 * Generate a random Immunization Recommendation on specific date
	 *
	 * @param tenant Tenant
	 * @param date   date of recommendation
	 * @return ImmunizationRecommendation resource
	 */
	ImmunizationRecommendation generate(Tenant tenant, Date date);

	/**
	 * Generate a random Immunization Recommendation on specific date for patient
	 *
	 * @param tenant          Tenant
	 * @param date            date of recommendation
	 * @param patientMaster patient
	 * @return ImmunizationRecommendation resource
	 */
	ImmunizationRecommendation generate(Tenant tenant, Date date, IisPatient patientMaster);

	ImmunizationRecommendation addRandomGeneratedRecommendation(IGenericClient fhirClient, IAnyResource patient);

	ImmunizationRecommendation addRandomGeneratedRecommendation(ImmunizationRecommendation recommendation);

	ImmunizationRecommendation queryCds(Tenant tenant, Date date, IisPatient patientMaster);

	MethodOutcome updateRecommendation(IGenericClient fhirClient, IAnyResource recommendation);

	ImmunizationRecommendation readRecommendation(String recommendationId, String recommendationIdentifier, IGenericClient fhirClient);

	ImmunizationRecommendation getPatientRecommendation(IGenericClient fhirClient, IAnyResource patient);

}
