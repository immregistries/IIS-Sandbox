package org.immregistries.iis.kernal.security;

import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.controllers.JwtSmartAuthController;
import org.immregistries.iis.kernal.controllers.WellKnownKeyController;
import org.immregistries.iis.kernal.controllers.servlet.HomeController;
import org.immregistries.iis.kernal.controllers.servlet.PopController;
import org.immregistries.iis.kernal.controllers.servlet.SoapDescriptionController;
import org.immregistries.iis.kernal.controllers.servlet.TenantController;
import org.immregistries.iis.kernal.controllers.servlet.util.UrlTenantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.List;

import static org.immregistries.iis.kernal.HapiFhirServerRegistrationConfig.FHIR_SERVER_PATH_EXTENSION;
import static org.immregistries.iis.kernal.controllers.IisRestPath.SH_LINKS_STORED_MANIFEST_FULL_PATH;
import static org.immregistries.iis.kernal.controllers.IisRestPath.SH_LINK_CONTENT_PATH;
import static org.immregistries.iis.kernal.controllers.servlet.LoginFormController.LOGIN_PARAM_PASSWORD;
import static org.immregistries.iis.kernal.controllers.servlet.LoginFormController.LOGIN_PARAM_USERID;

@Configuration
public class ServerSecurityConfig {
	public static final String LOGIN_PATH = "/login";
	public static final String LOGIN_FORM_PATH = "/loginForm";
	public static final String LOGOUT_PATH = "/logout";
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private UrlTenantUtil urlTenantUtil;

	/**
	 * upgraded with AI, TODO verify
	 */
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, IisOAuthSuccessHandler iisOAuthSuccessHandler,
			FormAuthenticationSuccessHandler formAuthenticationSuccessHandler, RequestCache requestCache)
			throws Exception {
		http.requestCache(cache -> cache.requestCache(requestCache));
		http.authorizeHttpRequests((authorize) -> authorize
			.requestMatchers(HttpMethod.GET,
				"/",
				HomeController.HOME_BASE_PATH,
				PopController.POP_BASE_PATH,
				IisRestPath.BasePath.SUBSCRIPTION_TOPIC_PATH + "/**",
				"/img/**")
						.permitAll()
			.requestMatchers(
				IisRestPath.PATIENT_MANIFEST_FULL_PATH + "/**",
				SH_LINKS_STORED_MANIFEST_FULL_PATH + "/*",
				TenantController.TENANT_PATH + WellKnownKeyController.WELL_KNOWN_PATH_SUFFIX,
						SH_LINK_CONTENT_PATH + "/*")
						.permitAll() // ShLinks
			.requestMatchers(LOGIN_FORM_PATH, "/oauth2/**", LOGIN_PATH)
			.permitAll()
						// API AUTHORIZATION AND AUTHENTICATION SEPARATED
					.requestMatchers(FHIR_SERVER_PATH_EXTENSION + "/**",
						SoapDescriptionController.SOAP_BASE_PATH,
						IisRestPath.BasePath.FHIR_MESSAGING_PATH + SoapDescriptionController.SOAP_BASE_PATH,
						JwtSmartAuthController.WELL_KNOWN_SMART_CONFIGURATION,
						JwtSmartAuthController.REGISTER_CLIENT,
						JwtSmartAuthController.TOKEN,
						IisRestPath.BasePath.REST_PATH + "/**")
						.permitAll()
			.anyRequest().authenticated()
		);

		http.formLogin((form) -> form
						.usernameParameter(LOGIN_PARAM_USERID)
						.passwordParameter(LOGIN_PARAM_PASSWORD)
						.loginPage(LOGIN_FORM_PATH) // Page where redirected when unauthorised
						.loginProcessingUrl(LOGIN_PATH) // url for login request to be processed (hollow)
			.successHandler(formAuthenticationSuccessHandler));
		http.oauth2Login((oauth2) -> oauth2
						.defaultSuccessUrl(HomeController.HOME_BASE_PATH)
			.successHandler(iisOAuthSuccessHandler));

		http.logout((logout) -> logout
						.logoutRequestMatcher(new AntPathRequestMatcher(LOGOUT_PATH)) // Use RequestMatcher
						.logoutSuccessUrl(LOGIN_FORM_PATH)
						.deleteCookies("JSESSIONID"));

		http.csrf(AbstractHttpConfigurer::disable);
		return http.build();
	}

	@Bean
	public FormAuthenticationSuccessHandler formAuthenticationSuccessHandler(HttpSecurity http, RequestCache requestCache) {
		FormAuthenticationSuccessHandler formAuthenticationSuccessHandler = new FormAuthenticationSuccessHandler();
		formAuthenticationSuccessHandler.setRequestCache(requestCache);
		return formAuthenticationSuccessHandler;
	}

	@Bean
	public RequestCache requestCache(HttpSecurity http) {
		return new HttpSessionRequestCache();
	}

	private void addTenantifiedRequestMatcher(List<RequestMatcher> matchers, String pathSuffix) {
		String tenantified = urlTenantUtil.securityConfigUrl(pathSuffix);
		matchers.add(new AntPathRequestMatcher(pathSuffix));
		matchers.add(new AntPathRequestMatcher(tenantified));
	}

}
