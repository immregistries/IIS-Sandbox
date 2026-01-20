package org.immregistries.iis.kernal.logic.recommendations;

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

	String IMMUNIZATION_RECOMMENDATION_DATE_CRITERION_SYSTEM = "http://hl7.org/fhir/ValueSet/immunization-recommendation-date-criterion";
	String IMMUNIZATION_RECOMMENDATION_STATUS_SYSTEM = "http://hl7.org/fhir/ValueSet/immunization-recommendation-status";

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

	ImmunizationRecommendation addRandomGeneratedRecommendation(ImmunizationRecommendation recommendation);

	ImmunizationRecommendation queryCds(Tenant tenant, Date date, IisPatient patientMaster);

}
