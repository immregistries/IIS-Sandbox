package org.immregistries.iis.kernal.logic;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.Tenant;

import java.util.Date;

/**
 * Implements Immunization Recommendation functionalities
 *
 * @param <ImmunizationRecommendation> FHIR class
 * @param <Patient>                    FHIR class
 */
public interface IImmunizationRecommendationService<ImmunizationRecommendation extends IBaseResource, Patient extends IDomainResource> {

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
	ImmunizationRecommendation generate(Tenant tenant, Date date, PatientMaster patientMaster);

	ImmunizationRecommendation addRandomGeneratedRecommendation(ImmunizationRecommendation recommendation);

	ImmunizationRecommendation queryCds(Tenant tenant, Date date, PatientMaster patientMaster);

}
