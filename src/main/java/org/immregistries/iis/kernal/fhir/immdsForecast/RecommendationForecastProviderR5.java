package org.immregistries.iis.kernal.fhir.immdsForecast;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r5.model.Immunization;
import org.hl7.fhir.r5.model.ImmunizationRecommendation;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Patient;
import org.immregistries.iis.kernal.fhir.annotations.OnR5Condition;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.logic.ImmunizationRecommendationServiceR5;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Controller;

import java.util.Date;
import java.util.List;

@Controller
@Conditional(OnR5Condition.class)
public class RecommendationForecastProviderR5 implements IRecommendationForecastProvider<Parameters, Patient, Immunization> {

	@Autowired
	ImmunizationRecommendationServiceR5 immunizationRecommendationServiceR5;

	@Override
	@Operation(name = $_IMMDS_FORECAST)
	public Parameters immdsForecastOperation(
		@Description(shortDefinition = "The date on which to assess the forecast.")
		@OperationParam(name = ASSESSMENT_DATE, min = 1, max = 1, typeName = "date")
		IPrimitiveType<Date> assessmentDate,
		@Description(shortDefinition = "Patient information.")
		@OperationParam(name = PATIENT, min = 1, max = 1)
		Patient patient,
		@Description(shortDefinition = "Patient immunization history.")
		@OperationParam(name = IMMUNIZATION)
		List<Immunization> immunization
	) {
		Parameters out = new Parameters();
		ImmunizationRecommendation immunizationRecommendation = immunizationRecommendationServiceR5.generate(ServletHelper.getTenant(), new Date(), patient);// generate(assessmentDate.getValue(), patient);
		out.addParameter().setName(RECOMMENDATION).setResource(immunizationRecommendation);
		return out;
	}
}
