package org.immregistries.iis.kernal.logic.config;

import org.immregistries.mismo.match.PatientMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;

@Configuration
public class MismoConfig {
	private static final Logger logger = LoggerFactory.getLogger(MismoConfig.class);
	public static final String MISMO_CONFIGURATION_YML = "/Mismo-Configuration.yml";


	@Bean
	public PatientMatcher patientMismoMatcher() {
		InputStream is = this.getClass().getResourceAsStream(MISMO_CONFIGURATION_YML);
		if (is == null) {
			logger.error("Unable to find Mismo-Configuration file");
		} else {
			logger.info("Found Mismo-Configuration file");
		}
		return new PatientMatcher(is);
	}
}
