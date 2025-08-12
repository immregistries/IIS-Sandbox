package org.immregistries.iis.kernal.fhir.security;

import org.immregistries.iis.kernal.servlet.FhirMessagingController;
import org.immregistries.iis.kernal.servlet.PopController;
import org.immregistries.iis.kernal.servlet.V2ToFhirController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import static org.immregistries.iis.kernal.servlet.LoginController.PARAM_PASSWORD;
import static org.immregistries.iis.kernal.servlet.LoginController.PARAM_USERID;
import static org.immregistries.iis.kernal.servlet.shlink.PatientShlinkManifestController.PATIENT_MANIFEST_FULL_PATH;
import static org.immregistries.iis.kernal.servlet.shlink.ShLinkManifestController.SHLINKS_CONTROLLER_BASE_URL;


@Configuration
public class ServerSecurityConfig {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * upgraded with AI, TODO verify
	 */
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, CustomOAuthSuccessHandler customOAuthSuccessHandler) throws Exception {
		http
			.authorizeHttpRequests((authorize) -> authorize
				.requestMatchers(HttpMethod.GET, "/", "/home", PopController.POP_BASE_PATH, "/SubscriptionTopic/**", "/img/**").permitAll()
				.requestMatchers("/tenant/*/manifest/**", SHLINKS_CONTROLLER_BASE_URL).permitAll() //Shlinks
				.requestMatchers("/loginForm", "/oauth2/**", "/login").permitAll()
				// API AUTHORIZATION AND AUTHENTICATION SEPARATED
				.requestMatchers("/fhir/**", "/soap", FhirMessagingController.FHIR_MESSAGING_BASE_PATH + "/soap", "/.well-known/smart-configuration", "/registerClient", "/token").permitAll()
				.anyRequest().authenticated()
			)
			.formLogin((form) -> form
				.usernameParameter(PARAM_USERID)
				.passwordParameter(PARAM_PASSWORD)
				.loginPage("/loginForm") // Page where redirected when unauthorised
				.loginProcessingUrl("/login") // url for login request to be processed (hollow)
				.defaultSuccessUrl("/home")
			)
			.oauth2Login((oauth2) -> oauth2
				.defaultSuccessUrl("/home")
				.successHandler(customOAuthSuccessHandler)
			)
			.logout((logout) -> logout
				.logoutRequestMatcher(new AntPathRequestMatcher("/logout")) // Use RequestMatcher
				.logoutSuccessUrl("/loginForm")
				.deleteCookies("JSESSIONID")
			)
			.csrf((csrf) -> csrf
				.ignoringRequestMatchers(
					new AntPathRequestMatcher(PATIENT_MANIFEST_FULL_PATH + "/**"),
					new AntPathRequestMatcher("/tenant/*/manifest/**"),
					new AntPathRequestMatcher(PopController.POP_BASE_PATH),
					new AntPathRequestMatcher(V2ToFhirController.V2_TO_FHIR_BASE_PATH),
					new AntPathRequestMatcher(FhirMessagingController.FHIR_MESSAGING_BASE_PATH),
					new AntPathRequestMatcher(FhirMessagingController.FHIR_MESSAGING_BASE_PATH + "/soap"),
					new AntPathRequestMatcher("/message"),
					new AntPathRequestMatcher("/fhir/**"),
					new AntPathRequestMatcher("/loginForm"),
					new AntPathRequestMatcher("/login"),
					new AntPathRequestMatcher("/logout"),
					new AntPathRequestMatcher("/patient"),
					new AntPathRequestMatcher("/subscription"),
					new AntPathRequestMatcher("/vaccination"),
					new AntPathRequestMatcher("/tenant"),
					new AntPathRequestMatcher("/soap")
				)
			);

		// ... other configuration
		return http.build();
	}

}
