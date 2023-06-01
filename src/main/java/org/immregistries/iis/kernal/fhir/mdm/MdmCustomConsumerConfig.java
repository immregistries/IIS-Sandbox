package org.immregistries.iis.kernal.fhir.mdm;

import ca.uhn.fhir.jpa.mdm.config.MdmConsumerConfig;
import ca.uhn.fhir.mdm.api.IMdmMatchFinderSvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@Conditional(MdmConfigCondition.class)
public class MdmCustomConsumerConfig extends MdmConsumerConfig {

	@Bean
	IMdmMatchFinderSvc mdmMatchFinderSvc() {
		return new MdmCustomMatchFinderSvc();
	}

}
