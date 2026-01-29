package org.immregistries.iis.kernal.logic.recommendations;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.hl7.fhir.r5.model.ImmunizationEvaluation;
import org.hl7.fhir.r5.model.ImmunizationRecommendation;
import org.hl7.fhir.r5.model.Parameters;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.immregistries.iis.kernal.logic.hl7v2.handling.IncomingQueryHandler;
import org.immregistries.iis.kernal.mapping.mappers.resources.r5.ImmunizationEvaluationMapperR5;
import org.immregistries.iis.kernal.mapping.mappers.resources.r5.ImmunizationRecommendationMapperR5;
import org.immregistries.iis.kernal.mapping.requesters.FhirSearchRequester;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.vfa.connect.model.ForecastActual;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

import static org.immregistries.iis.kernal.FhirConstants.EVALUATION;
import static org.immregistries.iis.kernal.FhirConstants.RECOMMENDATION;

@Service
@Conditional(OnR5Condition.class)
public class CdsQueryServiceR5 extends CdsQueryService<ImmunizationRecommendation, Parameters> {

	@Autowired
	private ImmunizationRecommendationMapperR5 immunizationRecommendationMapperR5;
	@Autowired
	private ImmunizationEvaluationMapperR5 immunizationEvaluationMapperR5;

	@Autowired
	private FhirSearchRequester fhirSearchRequester;

	public ImmunizationRecommendation queryCds(Tenant tenant, Date date, IisPatient iisPatient) {
		List<VaccinationMaster> vaccinationMasterList = fhirSearchRequester.searchVaccinationMasterGoldenList(
			new SearchParameterMap("patient",
				new ReferenceParam("Patient/" + iisPatient.getPatientId()).setMdmExpand(true)));
		Parameters parameters = queryCds(tenant, date, iisPatient, vaccinationMasterList);
		return (ImmunizationRecommendation) parameters
			.getParameter(RECOMMENDATION).getResource();
	}

	public Parameters queryCds(Tenant tenant, Date date, IisPatient iisPatient,
																	 List<? extends IisVaccination> iisVaccinationList) {
		List<ForecastActual> forecastActualList = doForecast(iisPatient,
			iisVaccinationList, tenant, date);
		IisRecommendation iisRecommendation = lonestarIisRecommendation(tenant, date, iisPatient, forecastActualList);

		ImmunizationRecommendation immunizationRecommendation = immunizationRecommendationMapperR5.fhirObject(iisRecommendation);

		Parameters parameters = new Parameters();
		parameters.addParameter().setResource(immunizationRecommendation).setName(RECOMMENDATION);
		for (IisVaccination iisVaccination : iisVaccinationList) {
			IisEvaluation iisEvaluation = new IisEvaluation(iisVaccination, date);
			ImmunizationEvaluation immunizationEvaluation = immunizationEvaluationMapperR5.fhirObject(iisEvaluation);
			if (immunizationEvaluation != null) {
				parameters.addParameter().setResource(immunizationEvaluation).setName(EVALUATION);
			}
		}
		return parameters;
	}


}
