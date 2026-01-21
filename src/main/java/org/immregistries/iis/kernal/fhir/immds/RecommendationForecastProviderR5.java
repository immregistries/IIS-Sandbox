package org.immregistries.iis.kernal.fhir.immds;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r5.model.Immunization;
import org.hl7.fhir.r5.model.ImmunizationRecommendation;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Patient;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.immregistries.iis.kernal.logic.recommendations.CdsQueryServiceR5;
import org.immregistries.iis.kernal.logic.recommendations.IisRecommendationGenerator;
import org.immregistries.iis.kernal.mapping.mappers.resources.r5.ImmunizationMapperR5;
import org.immregistries.iis.kernal.mapping.mappers.resources.r5.PatientMapperR5;
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
@Conditional(OnR5Condition.class)
public class RecommendationForecastProviderR5 implements IRecommendationForecastProvider<Parameters, Patient, Immunization> {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private PatientMapperR5 patientMapperR5;
	@Autowired
	private ImmunizationMapperR5 immunizationMapperR5;
	@Autowired
	private IisRecommendationGenerator iisRecommendationGenerator;
	@Autowired
	private CdsQueryServiceR5 cdsQueryService;

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
		List<? extends IisVaccination> iisVaccinationList = List.of();
		if (immunization != null) {
			iisVaccinationList = immunization.stream().map(immunization1 -> immunizationMapperR5.localObject(immunization1)).collect(Collectors.toList());
		}
		IisPatient iisPatient = patientMapperR5.localObject(patient);
		try {
			out = cdsQueryService.queryCds(CurrentTenantUtil.getTenant(), assessmentDate.getValue(), iisPatient, iisVaccinationList);
		} catch (Exception e) {
			ImmunizationRecommendation immunizationRecommendation = (ImmunizationRecommendation) iisRecommendationGenerator.generateFhirRecommendation(CurrentTenantUtil.getTenant(), assessmentDate.getValue(), iisPatient);
			out.addParameter().setName(RECOMMENDATION).setResource(immunizationRecommendation);
		}
		return out;
	}
}
