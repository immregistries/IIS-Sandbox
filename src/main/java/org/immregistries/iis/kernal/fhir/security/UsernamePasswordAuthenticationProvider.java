package org.immregistries.iis.kernal.fhir.security;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.servlet.PopServlet;
import org.immregistries.iis.kernal.servlet.ServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static org.immregistries.iis.kernal.servlet.LoginServlet.PARAM_FACILITYID;


@Component
public class UsernamePasswordAuthenticationProvider implements AuthenticationProvider
{
	Logger logger = LoggerFactory.getLogger(UsernamePasswordAuthenticationProvider.class);
	@Autowired
	private Environment env;

	@Autowired
	private OAuth2AuthorizedClientService authorizedClientService;
	@Autowired
	private OAuth2AuthorizedClientRepository authorizedClientRepository;
	@Autowired
	private ClientRegistrationRepository clientRegistrationRepository;
	private static String CLIENT_PROPERTY_KEY = "spring.security.oauth2.client.registration.";

	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		logger.info("AUTHENTICATION {}", authentication);
		OrgAccess orgAccess = null;

		if (authentication instanceof OAuth2LoginAuthenticationToken) {
			OAuth2LoginAuthenticationToken oAuth2LoginAuthenticationToken = (OAuth2LoginAuthenticationToken) authentication;
			WebAuthenticationDetails details = (WebAuthenticationDetails) authentication.getDetails();
			String clientName = "github";
			String clientId = env.getProperty(
				CLIENT_PROPERTY_KEY + clientName + ".client-id");

			if (clientId == null) {
				return null;
			}

			String clientSecret = env.getProperty(
				CLIENT_PROPERTY_KEY + clientName + ".client-secret");
			ClientRegistration clientRegistration = CommonOAuth2Provider.GITHUB.getBuilder(clientName)
				.clientId(clientId).clientSecret(clientSecret).build();
			logger.info("ClientRegistration {}", clientRegistration);
			logger.info("ClientRegistrationRepository {}", clientRegistrationRepository.findByRegistrationId("github"));

			clientRegistrationRepository.findByRegistrationId("github");
			OAuth2AuthorizedClient client = null;
			String userInfoEndpointUri = "https://api.github.com/user";
			if (StringUtils.isNotEmpty(userInfoEndpointUri)) { // inspired by https://www.baeldung.com/spring-security-5-oauth2-login section 6 TODO check for other possibility
				RestTemplate restTemplate = new RestTemplate();
				HttpHeaders headers = new HttpHeaders();
				headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + client.getAccessToken()
					.getTokenValue());
				HttpEntity entity = new HttpEntity("", headers);
				ResponseEntity<Map> response = restTemplate
					.exchange(userInfoEndpointUri, HttpMethod.GET, entity, Map.class);
				Map userAttributes = response.getBody();
				logger.info("AUTHENTICATION name {}", userAttributes);

				Session dataSession = PopServlet.getDataSession();
//				orgAccess = ServletHelper.authenticateOrgAccess("h", "h", "h", dataSession);


//				model.addAttribute("name", userAttributes.get("name"));
//				orgAccess = ServletHelper.authenticateOrgAccess(authentication.getName(), (String) authentication.getCredentials(), authentication.getName(), dataSession);

			}
		}
		if (authentication instanceof UsernamePasswordAuthenticationToken){
			HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
			Session dataSession = PopServlet.getDataSession();

			// TODO maybe customize  "PrincipalExtractor" instead and have the orgAccess/orgMaster as principal https://www.baeldung.com/spring-security-oauth-principal-authorities-extractor
			orgAccess = ServletHelper.authenticateOrgAccess(authentication.getName(), (String) authentication.getCredentials(), request.getParameter(PARAM_FACILITYID), dataSession);
		}


		// If credentials is string password
		if (orgAccess == null ) {
			throw new BadCredentialsException("Authentication Exception");
		}
		return orgAccess;
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
	}
}
