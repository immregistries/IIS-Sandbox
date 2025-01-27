package org.immregistries.iis.kernal.logic;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.Tenant;

import java.util.Date;

/**
 * Implements Immunization Recommendation functionalities
 *
 * @param <ImmunizationRecommendation> FHIR class
 * @param <Patient>                    FHIR class
 */
public interface IImmunizationRecommendationService<ImmunizationRecommendation extends IBaseResource, Patient extends IDomainResource> {

	String[] DATE_CRITERION_CODES = {"30981-5", "30980-7", "59777-3", "59778-1"};
	String[] DATE_CRITERION_DISPLAYS = {"Earliest date to give", "Date vaccine due", "Latest date to give immunization", "Date when overdue for immunization"};
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
	 * @param patientReported patient
	 * @return ImmunizationRecommendation resource
	 */
	ImmunizationRecommendation generate(Tenant tenant, Date date, PatientReported patientReported);

	/**
	 * Generate a random Immunization Recommendation on specific date for patient
	 *
	 * @param tenant  Tenant
	 * @param date    date of recommendation
	 * @param patient FHIR patient
	 * @return ImmunizationRecommendation resource
	 */
	ImmunizationRecommendation generate(Tenant tenant, Date date, Patient patient);

	ImmunizationRecommendation addGeneratedRecommendation(ImmunizationRecommendation recommendation);
}
