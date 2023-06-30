package org.immregistries.iis.kernal.fhir.security;

import org.immregistries.iis.kernal.fhir.security.CustomAuthenticationManager;
import org.immregistries.iis.kernal.fhir.security.RejectedRequestRedirector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

@Configuration
public class ServerSecurityConfig {

	@Bean
	public WebSecurityCustomizer webSecurityCustomizer(RejectedRequestRedirector rejectedRequestRedirector) {
		return (web) -> web
			.requestRejectedHandler(rejectedRequestRedirector)
			.ignoring().antMatchers("/home", "/fhir", "/pop");
	}

}
