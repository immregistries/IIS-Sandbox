package org.immregistries.iis.kernal.logic;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.Tenant;

import java.util.Date;

public interface IImmunizationRecommendationService<ImmunizationRecommendation extends IBaseResource, Patient extends IDomainResource> {

	String[] DATE_CRITERION_CODES = {"30981-5", "30980-7", "59777-3", "59778-1"};
	String[] DATE_CRITERION_DISPLAYS = {"Earliest date to give", "Date vaccine due", "Latest date to give immunization", "Date when overdue for immunization"};
	String IMMUNIZATION_RECOMMENDATION_DATE_CRITERION_SYSTEM = "http://hl7.org/fhir/ValueSet/immunization-recommendation-date-criterion";
	String IMMUNIZATION_RECOMMENDATION_STATUS_SYSTEM = "http://hl7.org/fhir/ValueSet/immunization-recommendation-status";

	ImmunizationRecommendation generate(Tenant tenant, Date date);

	ImmunizationRecommendation generate(Tenant tenant, Date date, PatientReported patientReported);

	ImmunizationRecommendation generate(Tenant tenant, Date date, Patient patient);

	ImmunizationRecommendation addGeneratedRecommendation(ImmunizationRecommendation recommendation);
}
