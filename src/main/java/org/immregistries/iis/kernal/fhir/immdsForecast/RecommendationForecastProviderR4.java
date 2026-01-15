package org.immregistries.iis.kernal.fhir.immdsForecast;


import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.ImmunizationRecommendation;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.logic.ImmunizationRecommendationServiceR4;
import org.immregistries.iis.kernal.logic.messageHandling.IncomingQueryHandler;
import org.immregistries.iis.kernal.mapping.resourceMappers.forR4.ImmunizationMapperR4;
import org.immregistries.iis.kernal.mapping.resourceMappers.forR4.PatientMapperR4;
import org.immregistries.iis.kernal.model.IisVaccination;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.security.CurrentTenantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Controller;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@Conditional(OnR4Condition.class)
public class RecommendationForecastProviderR4 implements IRecommendationForecastProvider<Parameters, Patient, Immunization> {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());


	@Autowired
	IncomingQueryHandler incomingQueryHandler;
	@Autowired
	PatientMapperR4 patientMapperR4;
	@Autowired
	ImmunizationMapperR4 immunizationMapperR4;
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
		List<IisVaccination> vaccinationMasterList;
		if (immunization != null) {
			vaccinationMasterList = immunization.stream().map(immunization1 -> immunizationMapperR4.localObject(immunization1)).collect(Collectors.toList());
		} else {
			vaccinationMasterList = List.of();
		}
		PatientMaster patientMaster = patientMapperR4.localObject(patient);
		try {
			out = immunizationRecommendationServiceR4.queryCds(CurrentTenantUtil.getTenant(), assessmentDate.getValue(), patientMaster, vaccinationMasterList);
			logger.info("out {}", out.getParameters(EVALUATION).size());
		} catch (Exception e) {
			ImmunizationRecommendation immunizationRecommendation = immunizationRecommendationServiceR4.generate(CurrentTenantUtil.getTenant(), assessmentDate.getValue(), patientMaster);
			out.addParameter().setName(RECOMMENDATION).setResource(immunizationRecommendation);
		}
		return out;
	}
}
