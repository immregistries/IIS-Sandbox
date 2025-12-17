package org.immregistries.iis.kernal.logic;

import gov.cdc.izgw.v2tofhir.converter.MessageParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class V2toFhirConfig {

	@Bean
	public MessageParser messageParser() {
		return new MessageParser();
	}


}
