package org.immregistries.iis.kernal.fhir.immdsForecast;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r5.model.Immunization;
import org.hl7.fhir.r5.model.ImmunizationRecommendation;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Patient;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.logic.ImmunizationRecommendationServiceR5;
import org.immregistries.iis.kernal.logic.IncomingQueryHandler;
import org.immregistries.iis.kernal.mapping.forR5.ImmunizationMapperR5;
import org.immregistries.iis.kernal.mapping.forR5.PatientMapperR5;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Controller;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@Conditional(OnR5Condition.class)
public class RecommendationForecastProviderR5 implements IRecommendationForecastProvider<Parameters, Patient, Immunization> {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	IncomingQueryHandler incomingQueryHandler;
	@Autowired
	PatientMapperR5 patientMapperR5;
	@Autowired
	ImmunizationMapperR5 immunizationMapperR5;
	@Autowired
	private ImmunizationRecommendationServiceR5 immunizationRecommendationServiceR5;

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
		List<VaccinationMaster> vaccinationMasterList = List.of();
		if (immunization != null) {
			vaccinationMasterList = immunization.stream().map(immunization1 -> immunizationMapperR5.localObject(immunization1)).collect(Collectors.toList());
		}
		PatientMaster patientMaster = patientMapperR5.localObject(patient);
		try {
			out = immunizationRecommendationServiceR5.queryCds(ServletHelper.getTenant(), assessmentDate.getValue(), patientMaster, vaccinationMasterList);
		} catch (Exception e) {
			ImmunizationRecommendation immunizationRecommendation = immunizationRecommendationServiceR5.generate(ServletHelper.getTenant(), assessmentDate.getValue(), patientMaster);
			out.addParameter().setName(RECOMMENDATION).setResource(immunizationRecommendation);
		}
		return out;
	}
}
