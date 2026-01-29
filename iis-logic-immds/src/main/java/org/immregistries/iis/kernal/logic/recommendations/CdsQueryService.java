package org.immregistries.iis.kernal.logic.recommendations;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.services.CodeMapManagerService;
import org.immregistries.iis.kernal.model.BusinessIdentifier;
import org.immregistries.iis.kernal.model.IisPatient;
import org.immregistries.iis.kernal.model.IisRecommendation;
import org.immregistries.iis.kernal.model.IisVaccination;
import org.immregistries.iis.kernal.model.enums.ProcessingFlavor;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.vfa.connect.ConnectFactory;
import org.immregistries.vfa.connect.ConnectorInterface;
import org.immregistries.vfa.connect.model.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.*;


public abstract class CdsQueryService<ImmunizationRecommendation extends IAnyResource, Parameters extends IBaseParameters> {
	public static final String CDS_SERVER_BASE_URL = "https://sabbia.westus2.cloudapp.azure.com";
	public static final String LONESTAR_PATH = "/lonestar/forecast";
	public static final String EVALUATION_SERVICE_PATH = "/opencds-decision-support-service/evaluate";
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private CodeMapManagerService codeMapManagerService;

	public abstract ImmunizationRecommendation queryCds(Tenant tenant, Date date, IisPatient iisPatient);

	/**
	 * Queries CDS and returns Parameters with ImmunizationEvaluations and ImmunizationRecommendations
	 * @param tenant Tenant
	 * @param date Date
	 * @param iisPatient Patient
	 * @param iisVaccinationList
	 * @return Fhir Parameters with evaluation and recommendation filled
	 */
	public abstract Parameters queryCds(Tenant tenant, Date date, IisPatient iisPatient, List<? extends IisVaccination> iisVaccinationList);

	public @NotNull IisRecommendation lonestarIisRecommendation(Tenant tenant, Date date, IisPatient iisPatient, List<ForecastActual> forecastActualList) {
		IisRecommendation iisRecommendation = new IisRecommendation(iisPatient, forecastActualList, date);
		iisRecommendation.getBusinessIdentifierList().add(new BusinessIdentifier(UUID.randomUUID().toString().split("-")[0]));
		iisRecommendation.setAuthority(new BusinessIdentifier("IIS-Sandbox/tenantAndLonestar", tenant.getOrganizationName()));
		return iisRecommendation;
	}

	public List<ForecastActual> doForecast(IisPatient patient, List<? extends IisVaccination> iisVaccinations, Tenant tenant, Date date) {
		CodeMap codeMap = codeMapManagerService.getCodeMap();
		List<ForecastActual> forecastActualList = null;
		Set<ProcessingFlavor> processingFlavorSet = tenant.getProcessingFlavorSet();
		try {
			TestCase testCase = new TestCase();
			testCase.setEvalDate(date);
			if (patient != null) {
				testCase.setPatientSex(patient.getSex());
				testCase.setPatientDob(patient.getBirthDate());
			} else {
				testCase.setPatientSex("F");
			}
			List<TestEvent> testEventList = new ArrayList<>();
			for (IisVaccination vaccination : iisVaccinations) {
				Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, vaccination.getVaccineCvxCode());
				if (cvxCode == null) {
					continue;
				}
				if ("D".equals(vaccination.getActionCode())) {
					continue;
				}
				int cvx;
				try {
					cvx = Integer.parseInt(vaccination.getVaccineCvxCode());
					TestEvent testEvent = new TestEvent(cvx, vaccination.getAdministeredDate());
					testEventList.add(testEvent);
					vaccination.setTestEvent(testEvent);
				} catch (NumberFormatException ignored) {
				}
			}
			testCase.setTestEventList(testEventList);
			Software software = new Software();
			software.setServiceUrl(CDS_SERVER_BASE_URL + LONESTAR_PATH);
			software.setService(org.immregistries.vfa.connect.model.Service.LSVF);
			if (processingFlavorSet.contains(ProcessingFlavor.ICE)) {
				software.setServiceUrl(CDS_SERVER_BASE_URL + EVALUATION_SERVICE_PATH);
				software.setService(org.immregistries.vfa.connect.model.Service.ICE);
			}

			ConnectorInterface connector = ConnectFactory.createConnecter(software, VaccineGroup.getForecastItemList());
			connector.setLogText(false);
			try {

				SoftwareResult softwareResult = new SoftwareResult();
				forecastActualList = connector.queryForForecast(testCase, softwareResult);
//				logger.info("swr {}", softwareResult.getLogText());
			} catch (IOException ioe) {
				logger.error("Unable to query for forecast", ioe);
			}
		} catch (Exception e) {
			logger.error("Unable to query for forecast", e);
		}
		return forecastActualList;
	}

}
