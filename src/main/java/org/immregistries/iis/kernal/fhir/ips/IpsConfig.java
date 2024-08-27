package org.immregistries.iis.kernal.fhir.ips;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.ips.api.IIpsGenerationStrategy;
import ca.uhn.fhir.jpa.ips.generator.IIpsGeneratorSvc;
import ca.uhn.fhir.jpa.ips.generator.IpsGeneratorSvcImpl;
import ca.uhn.fhir.jpa.ips.provider.IpsOperationProvider;
import ca.uhn.fhir.jpa.ips.strategy.DefaultIpsGenerationStrategy;
import org.immregistries.iis.kernal.fhir.annotations.OnR4Condition;
import org.immregistries.iis.kernal.fhir.annotations.OnR5Condition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IpsConfig {

	@Bean
	@Conditional(OnR4Condition.class)
	public IIpsGenerationStrategy ipsGenerationStrategyR4() {
		return new DefaultIpsGenerationStrategy();
	}

	@Bean
	@Conditional(OnR5Condition.class)
	public IIpsGenerationStrategy ipsGenerationStrategyR5() {
		return new IpsGenerationStrategyR5();
	}

	@Bean
	public IpsGeneratorSvcImpl ipsGeneratorSvc(FhirContext fhirContext, IIpsGenerationStrategy ipsGenerationStrategy,DaoRegistry daoRegistry) {
		IpsGeneratorSvcImpl ipsGeneratorSvc = new IpsGeneratorSvcIIS(fhirContext, ipsGenerationStrategy, daoRegistry);
		return ipsGeneratorSvc;
	}

	@Bean
	public IpsOperationProvider ipsOperationProvider(IIpsGeneratorSvc iIpsGeneratorSvc) {
		IpsOperationProvider ipsOperationProvider = new IpsOperationProvider(iIpsGeneratorSvc);
		return ipsOperationProvider;
	}
}
