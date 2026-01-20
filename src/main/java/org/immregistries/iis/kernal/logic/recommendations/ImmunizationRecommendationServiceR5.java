package org.immregistries.iis.kernal.logic.recommendations;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.immregistries.iis.kernal.logic.CodeMapManagerService;
import org.immregistries.iis.kernal.logic.v2.handling.IncomingQueryHandler;
import org.immregistries.iis.kernal.mapping.mappers.fields.r5.BusinessIdentifierMapperR5;
import org.immregistries.iis.kernal.mapping.mappers.resources.r5.ImmunizationEvaluationMapperR5;
import org.immregistries.iis.kernal.mapping.mappers.resources.r5.ImmunizationRecommendationMapperR5;
import org.immregistries.iis.kernal.mapping.requesters.FhirSearchRequester;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.vfa.connect.model.ForecastActual;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.immregistries.iis.kernal.fhir.immds.IRecommendationForecastProvider.EVALUATION;
import static org.immregistries.iis.kernal.fhir.immds.IRecommendationForecastProvider.RECOMMENDATION;

@Service
@Conditional(OnR5Condition.class)
public class ImmunizationRecommendationServiceR5 implements IImmunizationRecommendationService<ImmunizationRecommendation, Patient> {

	@Autowired
	private IncomingQueryHandler incomingQueryHandler;
	@Autowired
	private FhirSearchRequester fhirSearchRequester;
	@Autowired
	private ImmunizationRecommendationMapperR5 immunizationRecommendationMapperR5;
	@Autowired
	private ImmunizationEvaluationMapperR5 immunizationEvaluationMapperR5;
	@Autowired
	private CodeMapManagerService codeMapManagerService;
	@Autowired
	private BusinessIdentifierMapperR5 businessIdentifierMapper;
	@Autowired
	private IisRecommendationGenerator recommendationGenerator;

	@Override
	public ImmunizationRecommendation generate(Tenant tenant, Date date) {
		return immunizationRecommendationMapperR5.fhirObject(recommendationGenerator.generateRecommendation(tenant, date));
	}

	@Override
	public ImmunizationRecommendation generate(Tenant tenant, Date date, IisPatient iisPatient) {
		return immunizationRecommendationMapperR5.fhirObject(recommendationGenerator.generateRecommendation(tenant, date, iisPatient));
	}

	@Override
	public ImmunizationRecommendation addRandomGeneratedRecommendation(IGenericClient fhirClient, IAnyResource patient) {
		return addRandomGeneratedRecommendation(getPatientRecommendation(fhirClient, patient));
	}

	@Override
	public ImmunizationRecommendation addRandomGeneratedRecommendation(ImmunizationRecommendation recommendation) {
		ForecastActual forecastActual = recommendationGenerator.randomForecast();
		recommendation.addRecommendation(immunizationRecommendationMapperR5.recommendationComponent(forecastActual));
		return recommendation;
	}

	public ImmunizationRecommendation queryCds(Tenant tenant, Date date, IisPatient iisPatient) {
		List<VaccinationMaster> vaccinationMasterList = fhirSearchRequester.searchVaccinationMasterGoldenList(
				new SearchParameterMap("patient",
					new ReferenceParam("Patient/" + iisPatient.getPatientId()).setMdmExpand(true)));
		return (ImmunizationRecommendation) queryCds(tenant, date, iisPatient, vaccinationMasterList)
				.getParameter(RECOMMENDATION).getResource();
	}

	public Parameters queryCds(Tenant tenant, Date date, IisPatient iisPatient,
										List<? extends IisVaccination> iisVaccinationList) {
		List<ForecastActual> forecastActualList = incomingQueryHandler.doForecast(iisPatient,
			iisVaccinationList, tenant, date);
		IisRecommendation iisRecommendation = new IisRecommendation(iisPatient, forecastActualList, date);
		ImmunizationRecommendation immunizationRecommendation = immunizationRecommendationMapperR5.fhirObject(iisRecommendation);
		immunizationRecommendation.addIdentifier(new Identifier().setValue(UUID.randomUUID().toString().split("-")[0]));
		immunizationRecommendation.setAuthority(new Reference()
				.setIdentifier(new Identifier().setSystem("IIS-Sandbox/tenantAndLonestar")
						.setValue(tenant.getOrganizationName())));

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

	public MethodOutcome updateRecommendation(IGenericClient fhirClient, IAnyResource recommendation) {
		return fhirClient.update().resource(recommendation).withId(recommendation.getId()).execute();
	}

	public ImmunizationRecommendation readRecommendation(String recommendationId, String recommendationIdentifier, IGenericClient fhirClient) {
		ImmunizationRecommendation recommendation = null;
		if (recommendationId != null) {
			recommendation = fhirClient.read().resource(ImmunizationRecommendation.class)
				.withId(recommendationId).execute();
		} else if (recommendationIdentifier != null) {
			Bundle recommendationBundle = fhirClient.search()
				.forResource(ImmunizationRecommendation.class).where(
					Patient.IDENTIFIER.exactly().identifier(recommendationIdentifier))
				.returnBundle(Bundle.class).execute();
			if (recommendationBundle.hasEntry()) {
				recommendation = (ImmunizationRecommendation) recommendationBundle
					.getEntryFirstRep().getResource();
			}
		}
		return recommendation;
	}

	public @Nullable ImmunizationRecommendation getPatientRecommendation(IGenericClient fhirClient, IAnyResource patient) {
		ImmunizationRecommendation recommendation = null;
		IBaseBundle baseBundle = fhirClient.search().forResource(ImmunizationRecommendation.class)
			.where(ImmunizationRecommendation.PATIENT
				.hasId(new IdType(patient.getId()).getIdPart()))
			.execute();
		Bundle recommendationBundle = (Bundle) baseBundle;
		if (recommendationBundle.hasEntry()) {
			recommendation = (ImmunizationRecommendation) recommendationBundle
				.getEntryFirstRep().getResource();
		}
		return recommendation;
	}

}
