package org.immregistries.iis.kernal.logic.recommendations;

import org.immregistries.iis.kernal.model.BusinessIdentifier;
import org.immregistries.iis.kernal.model.IisRecommendation;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.vfa.connect.model.ForecastActual;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class RecommendationGenerator {

	public IisRecommendation generateRecommendation(Tenant tenant, Date date) {
		IisRecommendation iisRecommendation = new IisRecommendation();

		BusinessIdentifier businessIdentifier = new BusinessIdentifier();
		businessIdentifier.setValue(UUID.randomUUID().toString().split("-")[0]);
		iisRecommendation.setBusinessIdentifierList(List.of(businessIdentifier));

		iisRecommendation.setDate(date);

		BusinessIdentifier authority = new BusinessIdentifier();
		authority.setSystem("IIS-Sandbox/tenant");
		authority.setValue(tenant.getOrganizationName());
		iisRecommendation.setAuthority(authority);

		ForecastActual forecastActual = randomForecast();

//		recommendation = addRandomGeneratedRecommendation(recommendation);

	}

	public ForecastActual randomForecast() {
		ForecastActual forecastActual = new ForecastActual();
//		 forecastActual.set
	}
}
