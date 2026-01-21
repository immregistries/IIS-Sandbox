package org.immregistries.iis.kernal.logic.recommendations;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ImmunizationEvaluation;
import org.hl7.fhir.r4.model.ImmunizationRecommendation;
import org.hl7.fhir.r4.model.Reference;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.logic.v2.handling.IncomingQueryHandler;
import org.immregistries.iis.kernal.mapping.mappers.resources.r4.ImmunizationEvaluationMapperR4;
import org.immregistries.iis.kernal.mapping.mappers.resources.r4.ImmunizationRecommendationMapperR4;
import org.immregistries.iis.kernal.mapping.requesters.FhirSearchRequester;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.vfa.connect.model.ForecastActual;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.immregistries.iis.kernal.fhir.immds.IRecommendationForecastProvider.EVALUATION;
import static org.immregistries.iis.kernal.fhir.immds.IRecommendationForecastProvider.RECOMMENDATION;

@Service
@Conditional(OnR4Condition.class)
public class CdsQueryServiceR4 implements  CdsQueryService<ImmunizationRecommendation, Parameters> {

	@Autowired
	private ImmunizationRecommendationMapperR4 immunizationRecommendationMapperR4;
	@Autowired
	private ImmunizationEvaluationMapperR4 immunizationEvaluationMapperR4;

	@Autowired
	private IncomingQueryHandler incomingQueryHandler;
	@Autowired
	private FhirSearchRequester fhirSearchRequester;

	public ImmunizationRecommendation queryCds(Tenant tenant, Date date, IisPatient iisPatient) {
		List<VaccinationMaster> vaccinationMasterList = fhirSearchRequester.searchVaccinationMasterGoldenList(
			new SearchParameterMap("patient",
				new ReferenceParam("Patient/" + iisPatient.getPatientId()).setMdmExpand(true)));
		Parameters parameters = queryCds(tenant, date, iisPatient, vaccinationMasterList);
		return (ImmunizationRecommendation) parameters.getParameter(RECOMMENDATION).getResource();
	}

	public Parameters queryCds(Tenant tenant, Date date, IisPatient iisPatient,
																	 List<? extends IisVaccination> iisVaccinationList) {
		List<ForecastActual> forecastActualList = incomingQueryHandler.doForecast(iisPatient,
			iisVaccinationList, tenant, date);
		IisRecommendation iisRecommendation = lonestarIisRecommendation(tenant, date, iisPatient, forecastActualList);

		ImmunizationRecommendation immunizationRecommendation = immunizationRecommendationMapperR4.fhirObject(iisRecommendation);
		Parameters parameters = new Parameters();
		parameters.addParameter().setResource(immunizationRecommendation).setName(RECOMMENDATION);
		for (IisVaccination iisVaccination : iisVaccinationList) {
			IisEvaluation iisEvaluation = new IisEvaluation(iisVaccination, date);
			ImmunizationEvaluation immunizationEvaluation = immunizationEvaluationMapperR4.fhirObject(iisEvaluation);
			if (immunizationEvaluation != null) {
				parameters.addParameter().setResource(immunizationEvaluation).setName(EVALUATION);
			}
		}
		return parameters;
	}
}
