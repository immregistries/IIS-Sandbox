package org.immregistries.iis.kernal.fhir.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;


import static org.immregistries.iis.kernal.servlet.PopServlet.PARAM_PASSWORD;
import static org.immregistries.iis.kernal.servlet.PopServlet.PARAM_USERID;

@Configuration
public class ServerSecurityConfig {

//	@Bean
//	public WebSecurityCustomizer webSecurityCustomizer(RejectedRequestRedirector rejectedRequestRedirector) {
//		return (web) -> web
//			.requestRejectedHandler(rejectedRequestRedirector)
//			.ignoring().antMatchers("/home", "/fhir", "/pop")
//			;
//	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, RejectedRequestRedirector rejectedRequestRedirector, CustomAuthenticationManager customAuthenticationManager) throws Exception {
		http
			.authorizeRequests()
//			.anyRequest().permitAll()
			.antMatchers("/home","/pop","/message","/fhir/**","/oauth2/**").permitAll()
//			.antMatchers(HttpMethod.POST, "/pop","/message").anonymous()
			.anyRequest().authenticated()

			.and()
			// ... endpoints
			.formLogin()
			.usernameParameter(PARAM_USERID)
			.passwordParameter(PARAM_PASSWORD)
			.loginPage("/message")
			.loginProcessingUrl("/message")
			.failureHandler(rejectedRequestRedirector)
			.defaultSuccessUrl("/home", true)

			.and().csrf().disable()
//			.ignoringAntMatchers("/pop","/message", "/fhir/**")
//			.and().oauth2Login().

		;
		// ... other configuration
		return http.build();
	}

}
