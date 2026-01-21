package org.immregistries.iis.kernal.logic.recommendations;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.immregistries.iis.kernal.model.BusinessIdentifier;
import org.immregistries.iis.kernal.model.IisPatient;
import org.immregistries.iis.kernal.model.IisRecommendation;
import org.immregistries.iis.kernal.model.IisVaccination;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.vfa.connect.model.ForecastActual;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;
import java.util.UUID;


public interface CdsQueryService<ImmunizationRecommendation extends IAnyResource, Parameters extends IBaseParameters> {

	ImmunizationRecommendation queryCds(Tenant tenant, Date date, IisPatient iisPatient);

	/**
	 * Queries CDS and returns Parameters with ImmunizationEvaluations and ImmunizationRecommendations
	 * @param tenant Tenant
	 * @param date Date
	 * @param iisPatient Patient
	 * @param iisVaccinationList
	 * @return Fhir Parameters with evaluation and recommendation filled
	 */
	Parameters queryCds(Tenant tenant, Date date, IisPatient iisPatient, List<? extends IisVaccination> iisVaccinationList);

	default @NotNull IisRecommendation lonestarIisRecommendation(Tenant tenant, Date date, IisPatient iisPatient, List<ForecastActual> forecastActualList) {
		IisRecommendation iisRecommendation = new IisRecommendation(iisPatient, forecastActualList, date);
		iisRecommendation.getBusinessIdentifierList().add(new BusinessIdentifier(UUID.randomUUID().toString().split("-")[0]));
		iisRecommendation.setAuthority(new BusinessIdentifier("IIS-Sandbox/tenantAndLonestar", tenant.getOrganizationName()));
		return iisRecommendation;
	}
}
