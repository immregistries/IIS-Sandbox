package org.immregistries.iis.kernal.fhir.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.immregistries.iis.kernal.servlet.LoginServlet.PARAM_PASSWORD;
import static org.immregistries.iis.kernal.servlet.LoginServlet.PARAM_USERID;


@Configuration
public class ServerSecurityConfig {
	public static String PRINCIPAL_NAME = "cerbeor";
	Logger logger = LoggerFactory.getLogger(ServerSecurityConfig.class);
	@Autowired
	ApplicationContext applicationContext;
	@Autowired
	private Environment env;
	private static String CLIENT_PROPERTY_KEY = "spring.security.oauth2.client.registration.";


	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, CustomOAuthSuccessHandler customOAuthSuccessHandler) throws Exception {
		http
			.authorizeRequests()
				.antMatchers(HttpMethod.GET,"/","/home","/pop","/SubscriptionTopic","/img/**").permitAll()
				.antMatchers("/loginForm","/fhir/**","/oauth2/**", "/login","/soap").permitAll()
				.anyRequest().authenticated()
			.and()
			// ... endpoints
			.formLogin()
				.usernameParameter(PARAM_USERID)
				.passwordParameter(PARAM_PASSWORD)
				.loginPage("/loginForm") // Page where redirected when unauthorised
				.loginProcessingUrl("/login") // url for login request to be processed (hollow)
				.defaultSuccessUrl("/home")
			.and()
				.oauth2Login()
				.defaultSuccessUrl("/home")
				.successHandler(customOAuthSuccessHandler)
			.and()
			.logout()
				.logoutUrl("/logout")
				.logoutSuccessUrl("/loginForm")
				.deleteCookies("JSESSIONID")
			.and()
				.csrf()
				.disable()
//				.ignoringAntMatchers("/pop","/message", "/fhir/**", "/loginForm", "/logout",  "/patient", "/subscription", "/vaccination")
		;
		// ... other configuration
		return http.build();
	}

//	private static List<String> clients = Arrays.asList("github");
//	@Bean
//	public ClientRegistrationRepository clientRegistrationRepository() {
//		List<ClientRegistration> registrations = clients.stream()
//			.map(c -> getRegistration(c))
//			.filter(registration -> registration != null)
//			.collect(Collectors.toList());
//
//		return new InMemoryClientRegistrationRepository(registrations);
//	}

//	public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
//
//	}

}
