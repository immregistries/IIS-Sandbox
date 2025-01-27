package org.immregistries.iis.kernal.fhir.ips;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.ips.api.IIpsGenerationStrategy;
import ca.uhn.fhir.jpa.ips.generator.IIpsGeneratorSvc;
import ca.uhn.fhir.jpa.ips.generator.IpsGeneratorSvcImpl;
import ca.uhn.fhir.jpa.ips.provider.IpsOperationProvider;
import org.immregistries.iis.kernal.fhir.annotations.OnR4Condition;
import org.immregistries.iis.kernal.fhir.annotations.OnR5Condition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IpsConfig {

	@Bean
	@Conditional(OnR4Condition.class)
	public ICustomIpsGenerationStrategy ipsGenerationStrategyR4() {
		return new IpsGenerationStrategyR4();
	}

	@Bean
	@Conditional(OnR5Condition.class)
	public ICustomIpsGenerationStrategy ipsGenerationStrategyR5() {
		return new IpsGenerationStrategyR5();
	}

	@Bean
	public IpsGeneratorSvcImpl ipsGeneratorSvc(FhirContext fhirContext, IIpsGenerationStrategy ipsGenerationStrategy,DaoRegistry daoRegistry) {
		return new IpsGeneratorSvcIIS(fhirContext, ipsGenerationStrategy, daoRegistry);
	}

	@Bean
	public IpsOperationProvider ipsOperationProvider(IIpsGeneratorSvc iIpsGeneratorSvc) {
		return new IpsOperationProvider(iIpsGeneratorSvc);
	}
}
