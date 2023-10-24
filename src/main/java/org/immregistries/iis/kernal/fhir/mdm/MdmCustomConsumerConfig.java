package org.immregistries.iis.kernal.fhir.mdm;

import ca.uhn.fhir.jpa.mdm.config.MdmConsumerConfig;
import ca.uhn.fhir.mdm.api.IMdmMatchFinderSvc;
import org.immregistries.iis.kernal.fhir.annotations.OnR4Condition;
import org.immregistries.iis.kernal.fhir.annotations.OnR5Condition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@Conditional(MdmConfigCondition.class)
public class MdmCustomConsumerConfig extends MdmConsumerConfig {

//	@Bean
//	@Conditional(OnR5Condition.class)
//	IMdmMatchFinderSvc mdmMatchFinderSvc() {
//		return new MdmCustomMatchFinderSvcR5();
//	}
//
//	@Bean
//	@Conditional(OnR4Condition.class)
//	IMdmMatchFinderSvc mdmMatchFinderSvcR4() {
//		return new MdmCustomMatchFinderSvcR4();
//	}

}
