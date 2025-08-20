package org.immregistries.iis.kernal.fhir.security;

import org.immregistries.iis.kernal.servlet.*;
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
	public static final String LOGIN_PATH = "/login";
	public static final String LOGIN_FORM_PATH = "/loginForm";
	public static final String LOGOUT_PATH = "/logout";
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * upgraded with AI, TODO verify
	 */
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, CustomOAuthSuccessHandler customOAuthSuccessHandler, FormAuthenticationSuccessHandler formAuthenticationSuccessHandler, RequestCache requestCache) throws Exception {
		http
			.requestCache(cache -> cache.requestCache(requestCache))
			.authorizeHttpRequests((authorize) -> authorize
				.requestMatchers(HttpMethod.GET, "/", HomeController.HOME_BASE_PATH, PopController.POP_BASE_PATH, "/SubscriptionTopic/**", "/img/**").permitAll()
				.requestMatchers("/tenant/*/manifest/**", SHLINKS_CONTROLLER_BASE_URL).permitAll() // Shlinks
				.requestMatchers(LOGIN_FORM_PATH, "/oauth2/**", LOGIN_PATH).permitAll()
				// API AUTHORIZATION AND AUTHENTICATION SEPARATED
				.requestMatchers("/fhir/**", SoapController.SOAP_BASE_PATH, FhirMessagingController.FHIR_MESSAGING_BASE_PATH + SoapController.SOAP_BASE_PATH, "/.well-known/smart-configuration", "/registerClient", "/token").permitAll()
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
				.defaultSuccessUrl(HomeController.HOME_BASE_PATH)
				.successHandler(customOAuthSuccessHandler)
			)
			.logout((logout) -> logout
				.logoutRequestMatcher(new AntPathRequestMatcher(LOGOUT_PATH)) // Use RequestMatcher
				.logoutSuccessUrl(LOGIN_FORM_PATH)
				.deleteCookies("JSESSIONID")
			);

		List<RequestMatcher> csrfIgnoringRequestMatchers = new ArrayList<>(30);
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, PATIENT_MANIFEST_FULL_PATH + "/**");
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, "/manifest/**");
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, PopController.POP_BASE_PATH);
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, V2ToFhirController.V2_TO_FHIR_BASE_PATH);
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, FhirMessagingController.FHIR_MESSAGING_BASE_PATH);
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, FhirMessagingController.FHIR_MESSAGING_BASE_PATH + SoapController.SOAP_BASE_PATH);
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, MessageController.MESSAGE_BASE_PATH);
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, "/fhir/**");
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, LOGIN_FORM_PATH);
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, LOGIN_PATH);
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, LOGOUT_PATH);
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, PatientController.PATIENT_BASE_PATH);
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, SubscriptionController.SUBSCRIPTION_BASE_PATH);
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, VaccinationController.VACCINATION_BASE_PATH);
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, TenantController.TENANT_PATH);
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, LocationController.LOCATION_BASE_PATH);
		addTenantifiedRequestMatcher(csrfIgnoringRequestMatchers, SoapController.SOAP_BASE_PATH);
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
