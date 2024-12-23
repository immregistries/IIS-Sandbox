package org.immregistries.iis.kernal.fhir.immdsForecast;

import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IDomainResource;

public interface IRecommendationForecastProvider<Parameters extends IBaseParameters, Patient extends IDomainResource, Immunization extends IDomainResource> {
	String $_IMMDS_FORECAST = "$immds-forecast";
	String ASSESSMENT_DATE = "assessmentDate";
	String PATIENT = "patient";
	String RECOMMENDATION = "recommendation";
	String IMMUNIZATION = "immunization";
	String IMM_DSFORECAST_CANONICAL_URL = "http://hl7.org/fhir/us/immds/OperationDefinition/ImmDSForecastOperation";

	//	@Operation(name = $_IMMDS_FORECAST)
//	@Operation(name = $_IMMDS_FORECAST,
//		idempotent = true,
//		canonicalUrl = IMM_DSFORECAST_CANONICAL_URL, typeName = "")
//	abstract Parameters immdsForecastOperation(
//		@Description(shortDefinition = "The date on which to assess the forecast.")
//		@OperationParam(name = ASSESSMENT_DATE, min = 1, max = 1, typeName = "date")
//		IPrimitiveType<Date> assessmentDate,
//		@Description(shortDefinition = "Patient information.")
//		@OperationParam(name = PATIENT, min = 1, max = 1)
//		Patient patient,
//		@Description(shortDefinition = "Patient immunization history.")
//		@OperationParam(name = IMMUNIZATION)
//		List<Immunization> immunization);

}
