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
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.ArrayList;
import java.util.List;

import static org.immregistries.iis.kernal.servlet.LoginController.LOGIN_PARAM_PASSWORD;
import static org.immregistries.iis.kernal.servlet.LoginController.LOGIN_PARAM_USERID;
import static org.immregistries.iis.kernal.servlet.shlink.PatientShlinkManifestController.PATIENT_MANIFEST_FULL_PATH;
import static org.immregistries.iis.kernal.servlet.shlink.ShLinkManifestController.SHLINKS_CONTROLLER_BASE_URL;


@Configuration
public class ServerSecurityConfig {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * upgraded with AI, TODO verify
	 */
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, CustomOAuthSuccessHandler customOAuthSuccessHandler, FormAuthenticationSuccessHandler formAuthenticationSuccessHandler, RequestCache requestCache) throws Exception {
		http
			.requestCache(cache -> cache.requestCache(requestCache))
			.authorizeHttpRequests((authorize) -> authorize
				.requestMatchers(HttpMethod.GET, "/", "/home", PopController.POP_BASE_PATH, "/SubscriptionTopic/**", "/img/**").permitAll()
				.requestMatchers("/tenant/*/manifest/**", SHLINKS_CONTROLLER_BASE_URL).permitAll() // Shlinks
				.requestMatchers("/loginForm", "/oauth2/**", "/login").permitAll()
				// API AUTHORIZATION AND AUTHENTICATION SEPARATED
				.requestMatchers("/fhir/**", "/soap", FhirMessagingController.FHIR_MESSAGING_BASE_PATH + "/soap", "/.well-known/smart-configuration", "/registerClient", "/token").permitAll()
				.anyRequest().authenticated()
			)
			.formLogin((form) -> form
					.usernameParameter(LOGIN_PARAM_USERID)
					.passwordParameter(LOGIN_PARAM_PASSWORD)
				.loginPage("/loginForm") // Page where redirected when unauthorised
				.loginProcessingUrl("/login") // url for login request to be processed (hollow)
				.successHandler(formAuthenticationSuccessHandler)
//				.addObjectPostProcessor()
			)

			.oauth2Login((oauth2) -> oauth2
				.defaultSuccessUrl("/home")
				.successHandler(customOAuthSuccessHandler)
			)
			.logout((logout) -> logout
				.logoutRequestMatcher(new AntPathRequestMatcher("/logout")) // Use RequestMatcher
				.logoutSuccessUrl("/loginForm")
				.deleteCookies("JSESSIONID")
			);

		List<RequestMatcher> csrfIgnoringRequestMatchers = new ArrayList<>(30);
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, PATIENT_MANIFEST_FULL_PATH + "/**");
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, "/manifest/**");
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, PopController.POP_BASE_PATH);
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, V2ToFhirController.V2_TO_FHIR_BASE_PATH);
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, FhirMessagingController.FHIR_MESSAGING_BASE_PATH);
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, FhirMessagingController.FHIR_MESSAGING_BASE_PATH + "/soap");
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, "/message");
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, "/fhir/**");
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, "/loginForm");
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, "/login");
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, "/logout");
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, "/patient");
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, "/subscription");
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, "/vaccination");
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, "/tenant");
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, "/soap");
		http.csrf((csrf) -> csrf
			.ignoringRequestMatchers(csrfIgnoringRequestMatchers.toArray(new RequestMatcher[csrfIgnoringRequestMatchers.size()]))
		);
		// ... other configuration
		return http.build();
	}

	@Bean
	FormAuthenticationSuccessHandler formAuthenticationSuccessHandler(HttpSecurity http, RequestCache requestCache) {
		FormAuthenticationSuccessHandler formAuthenticationSuccessHandler = new FormAuthenticationSuccessHandler();
		formAuthenticationSuccessHandler.setRequestCache(requestCache);
		return formAuthenticationSuccessHandler;
	}

	@Bean
	RequestCache requestCache(HttpSecurity http) {
		return new HttpSessionRequestCache();
	}


	private AntPathRequestMatcher tenantifyRequestMatcher(HttpMethod httpMethod, String pathSuffix) {
		AntPathRequestMatcher antPathRequestMatcher = tenantifyRequestMatcher(pathSuffix);
		String tenantified = ServletHelper.tenantifyUrl("*", pathSuffix);
		return new AntPathRequestMatcher(pathSuffix, httpMethod.toString());
	}

	private void addTenantifiedRequestMatcher(List<RequestMatcher> matchers, String pathSuffix) {
		String tenantified = ServletHelper.tenantifyUrl("*", pathSuffix);
		matchers.add(new AntPathRequestMatcher(pathSuffix));
		matchers.add(new AntPathRequestMatcher(tenantified));
	}

	private AntPathRequestMatcher tenantifyRequestMatcher(String pathSuffix) {
		String tenantified = ServletHelper.tenantifyUrl("*", pathSuffix);
		return new AntPathRequestMatcher(tenantified);
	}

}
