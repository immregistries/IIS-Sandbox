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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;


import static org.immregistries.iis.kernal.servlet.PopServlet.PARAM_PASSWORD;
import static org.immregistries.iis.kernal.servlet.PopServlet.PARAM_USERID;

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
	public SecurityFilterChain filterChain(HttpSecurity http, RejectedRequestRedirector rejectedRequestRedirector
														,AuthenticationManager customAuthenticationManager
	) throws Exception {
		http
			.authenticationManager(customAuthenticationManager)
			.authorizeRequests()
				.antMatchers(HttpMethod.GET,"/","/home","/pop","/SubscriptionTopic").permitAll()
				.antMatchers("/loginForm","/fhir/**","/oauth2/**", "/login","/soap").permitAll()
				.anyRequest().authenticated()
			.and()
			// ... endpoints
			.formLogin()
				.usernameParameter(PARAM_USERID)
				.passwordParameter(PARAM_PASSWORD)
				.loginPage("/loginForm") // Page where redirected when unauthorised
				.loginProcessingUrl("/login") // url for login request to be processed (hollow)
//				.failureHandler(rejectedRequestRedirector) // Used for testing but could be removed
//			.and()
//				.oauth2Login()
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


//	@Bean
//	public ReactiveClientRegistrationRepository clientRegistrationRepository() {
//		return new InMemoryReactiveClientRegistrationRepository(this.githubClientRegistration());
//	}
//
//	private ClientRegistration githubClientRegistration() {
//		String clientId = env.getProperty(
//			CLIENT_PROPERTY_KEY + "github.client-id");
//		String clientSecret = env.getProperty(
//			CLIENT_PROPERTY_KEY  + "github.client-secret");
//		return CommonOAuth2Provider.GITHUB.getBuilder("github")
//			.clientId(clientId).clientSecret(clientSecret).build();
//	}

}
