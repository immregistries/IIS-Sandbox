package org.immregistries.iis.kernal.flogic.immds;


import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.ImmunizationRecommendation;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.logic.recommendations.CdsQueryServiceR4;
import org.immregistries.iis.kernal.logic.recommendations.IisRecommendationGenerator;
import org.immregistries.iis.kernal.mapping.mappers.resources.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.mappers.resources.PatientMapper;
import org.immregistries.iis.kernal.model.IisPatient;
import org.immregistries.iis.kernal.model.IisVaccination;
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
	private PatientMapper<Patient> patientMapper;
	@Autowired
	private ImmunizationMapper<Immunization> immunizationMapper;
	@Autowired
	private IisRecommendationGenerator iisRecommendationGenerator;
	@Autowired
	private CdsQueryServiceR4 cdsQueryService;

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
		List<Immunization> immunization,
		RequestDetails theRequestDetails
	) {
		Parameters out = new Parameters();
		List<? extends IisVaccination> iisVaccinationList;
		if (immunization != null) {
			iisVaccinationList = immunization.stream().map(immunization1 -> immunizationMapper.localObject(immunization1)).collect(Collectors.toList());
		} else {
			iisVaccinationList = List.of();
		}
		IisPatient iisPatient = patientMapper.localObject(patient);
		try {
			out = cdsQueryService.queryCds(CurrentTenantUtil.getTenant(), assessmentDate.getValue(), iisPatient, iisVaccinationList);
			logger.info("out {}", out.getParameters(EVALUATION).size());
		} catch (Exception e) {
			ImmunizationRecommendation immunizationRecommendation = (ImmunizationRecommendation) iisRecommendationGenerator.generateFhirRecommendation(CurrentTenantUtil.getTenant(), assessmentDate.getValue(), iisPatient);
			out.addParameter().setName(RECOMMENDATION).setResource(immunizationRecommendation);
		}
		return out;
	}
}
