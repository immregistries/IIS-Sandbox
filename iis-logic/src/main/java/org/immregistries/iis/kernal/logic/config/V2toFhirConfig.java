package org.immregistries.iis.kernal.logic.config;

import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;
import gov.cdc.izgw.v2tofhir.converter.MessageParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@Conditional(OnR4Condition.class)
public class V2toFhirConfig {

	@Bean
	public MessageParser messageParser() {
		return new MessageParser();
	}


}
