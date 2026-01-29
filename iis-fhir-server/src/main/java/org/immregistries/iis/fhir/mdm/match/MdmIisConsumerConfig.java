package org.immregistries.iis.fhir.mdm.match;

import ca.uhn.fhir.jpa.mdm.config.MdmConsumerConfig;
import ca.uhn.fhir.mdm.api.IMdmMatchFinderSvc;
import org.hl7.fhir.r5.model.Immunization;
import org.hl7.fhir.r5.model.Patient;
import org.immregistries.iis.fhir.annotations.OnR4Condition;
import org.immregistries.iis.fhir.annotations.OnR5Condition;
import org.immregistries.iis.fhir.mdm.MdmConfigCondition;
import org.immregistries.iis.fhir.mdm.MdmIisMatchFinderSvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@Conditional(MdmConfigCondition.class)
public class MdmIisConsumerConfig extends MdmConsumerConfig {

	@Bean
	@Conditional(OnR5Condition.class)
	IMdmMatchFinderSvc mdmMatchFinderSvcR5() {
		return new MdmIisMatchFinderSvc<Immunization, Patient>();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	IMdmMatchFinderSvc mdmMatchFinderSvc() {
		return new MdmIisMatchFinderSvc<org.hl7.fhir.r4.model.Immunization, org.hl7.fhir.r4.model.Patient>();
	}

}
