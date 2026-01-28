package org.immregistries.iis.kernal.fhir.mdm.match;

import ca.uhn.fhir.jpa.mdm.config.MdmConsumerConfig;
import ca.uhn.fhir.mdm.api.IMdmMatchFinderSvc;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.immregistries.iis.kernal.fhir.mdm.MdmConfigCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@Conditional(MdmConfigCondition.class)
public class MdmIisConsumerConfig extends MdmConsumerConfig {

	@Bean
	@Conditional(OnR5Condition.class)
	IMdmMatchFinderSvc mdmMatchFinderSvcR5() {
		return new MdmIisMatchFinderSvc<org.hl7.fhir.r5.model.Immunization, org.hl7.fhir.r5.model.Patient>();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	IMdmMatchFinderSvc mdmMatchFinderSvc() {
		return new MdmIisMatchFinderSvc<org.hl7.fhir.r4.model.Immunization, org.hl7.fhir.r4.model.Patient>();
	}

}
