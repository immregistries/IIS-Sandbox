package org.immregistries.iis.kernal.fhir.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.immregistries.iis.kernal.servlet.LoginServlet.PARAM_PASSWORD;
import static org.immregistries.iis.kernal.servlet.LoginServlet.PARAM_USERID;


@Configuration
public class ServerSecurityConfig {
	public static String PRINCIPAL_NAME = "cerbeor";
	Logger logger = LoggerFactory.getLogger(ServerSecurityConfig.class);

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, CustomOAuthSuccessHandler customOAuthSuccessHandler) throws Exception {
		http
			.authorizeRequests()
			.antMatchers(HttpMethod.GET, "/", "/home", "/pop", "/SubscriptionTopic", "/img/**").permitAll()
			.antMatchers("/loginForm", "/oauth2/**", "/login" ).permitAll()
			// API AUTHORIZATION AND AUTHENTICATION SEPARATED
			.antMatchers("/fhir/**", "/soap").permitAll()
			.anyRequest().authenticated()
			.and()
			//USERNAME
			.formLogin()
			.usernameParameter(PARAM_USERID)
			.passwordParameter(PARAM_PASSWORD)
			.loginPage("/loginForm") // Page where redirected when unauthorised
			.loginProcessingUrl("/login") // url for login request to be processed (hollow)
			.defaultSuccessUrl("/home")
			.and()
			//OAUTH
			.oauth2Login()
			.defaultSuccessUrl("/home")
			.successHandler(customOAuthSuccessHandler)
			.and()
			// LOGOUT
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

}
