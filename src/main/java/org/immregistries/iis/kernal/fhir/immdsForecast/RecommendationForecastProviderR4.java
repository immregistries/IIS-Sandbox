package org.immregistries.iis.kernal.fhir.immdsForecast;


import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.ImmunizationRecommendation;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.immregistries.iis.kernal.fhir.annotations.OnR4Condition;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.logic.ImmunizationRecommendationServiceR4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Controller;

import java.util.Date;
import java.util.List;

@Controller
@Conditional(OnR4Condition.class)
public class RecommendationForecastProviderR4
	implements IRecommendationForecastProvider<Parameters, Patient, Immunization> {
	Logger logger = LoggerFactory.getLogger(RecommendationForecastProviderR4.class);

	@Autowired
	private ImmunizationRecommendationServiceR4 immunizationRecommendationServiceR4;

	@Operation(name = $_IMMDS_FORECAST,
		idempotent = true,
		canonicalUrl = IMM_DSFORECAST_CANONICAL_URL,
		typeName = "")
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
		ImmunizationRecommendation immunizationRecommendation = immunizationRecommendationServiceR4.generate(ServletHelper.getTenant(), new Date(), patient);// generate(assessmentDate.getValue(), patient);
		out.addParameter().setName(RECOMMENDATION).setResource(immunizationRecommendation);
		return out;
	}
}
