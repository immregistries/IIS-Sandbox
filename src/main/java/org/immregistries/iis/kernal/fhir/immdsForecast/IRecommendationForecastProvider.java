package org.immregistries.iis.kernal.fhir.immdsForecast;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

import java.util.Date;
import java.util.List;

public interface IRecommendationForecastProvider<Parameters extends IBaseParameters, Patient extends IDomainResource, Immunization extends IDomainResource> {
	String $_IMMDS_FORECAST = "$immds-forecast";
	String ASSESSMENT_DATE = "assessmentDate";
	String PATIENT = "patient";
	String RECOMMENDATION = "recommendation";
	String IMMUNIZATION = "immunization";

	@Operation(name = $_IMMDS_FORECAST)
	Parameters immdsForecastOperation(
		@Description(shortDefinition = "The date on which to assess the forecast.")
		@OperationParam(name = ASSESSMENT_DATE, min = 1, max = 1, typeName = "date")
		IPrimitiveType<Date> assessmentDate,
		@Description(shortDefinition = "Patient information.")
		@OperationParam(name = PATIENT, min = 1, max = 1)
		Patient patient,
		@Description(shortDefinition = "Patient immunization history.")
		@OperationParam(name = IMMUNIZATION)
		List<Immunization> immunization);

}
