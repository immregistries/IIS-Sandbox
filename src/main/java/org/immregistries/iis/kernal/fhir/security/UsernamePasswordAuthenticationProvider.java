package org.immregistries.iis.kernal.fhir.security;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.immregistries.iis.kernal.model.Tenant;
import org.immregistries.iis.kernal.model.UserAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

import static org.immregistries.iis.kernal.fhir.security.ServletHelper.SESSION_TENANT;
import static org.immregistries.iis.kernal.fhir.security.ServletHelper.SESSION_USER_ACCESS;
import static org.immregistries.iis.kernal.servlet.LoginServlet.PARAM_TENANT_NAME;


@Component
public class UsernamePasswordAuthenticationProvider implements AuthenticationProvider {
	private static String CLIENT_PROPERTY_KEY = "spring.security.oauth2.client.registration.";
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	Environment env;
	@Autowired
	private OAuth2AuthorizedClientService authorizedClientService;
	@Autowired
	private OAuth2AuthorizedClientRepository authorizedClientRepository;
	@Autowired
	private ClientRegistrationRepository clientRegistrationRepository;

	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		Session dataSession = ServletHelper.getDataSession();

		// TODO maybe customize  "PrincipalExtractor" instead and have the userAccess/tenant as principal https://www.baeldung.com/spring-security-oauth-principal-authorities-extractor
		if (StringUtils.isNotBlank(request.getParameter(PARAM_TENANT_NAME))) {
			Tenant tenant = ServletHelper.authenticateTenant(authentication.getName(), (String) authentication.getCredentials(), request.getParameter(PARAM_TENANT_NAME), dataSession);
			if (tenant != null) {
				/**
				 * Creating a new session after login
				 */
				request.getSession(true).setAttribute(SESSION_TENANT, tenant);
				return tenant.getUserAccess();
			} else {
				return null;
			}
		} else  {
			UserAccess userAccess = ServletHelper.authenticateUserAccessUsernamePassword(authentication.getName(), (String) authentication.getCredentials(), dataSession);
			request.getSession(true).setAttribute(SESSION_USER_ACCESS, userAccess);
			return userAccess;
		}
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
	}
}
