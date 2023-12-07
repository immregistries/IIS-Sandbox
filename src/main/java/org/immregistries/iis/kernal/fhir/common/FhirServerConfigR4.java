package org.immregistries.iis.kernal.fhir.common;

import ca.uhn.fhir.jpa.config.r4.JpaR4Config;
import org.immregistries.iis.kernal.fhir.ServerConfig;
import org.immregistries.iis.kernal.fhir.annotations.OnR4Condition;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Conditional(OnR4Condition.class)
@Import({
	JpaR4Config.class,
	StarterJpaConfig.class,
//	StarterCrR4Config.class,
	ElasticsearchConfig.class,
//	StarterIpsConfig.class,
	ServerConfig.class,
})
public class FhirServerConfigR4 {
}
