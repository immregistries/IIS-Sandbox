package org.immregistries.iis.kernal.fhir.security;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgMaster;
import org.immregistries.iis.kernal.servlet.PopServlet;
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

import static org.immregistries.iis.kernal.fhir.security.ServletHelper.SESSION_ORGACCESS;
import static org.immregistries.iis.kernal.servlet.LoginServlet.PARAM_FACILITYID;
import static org.immregistries.iis.kernal.fhir.security.ServletHelper.SESSION_ORGMASTER;


@Component
public class UsernamePasswordAuthenticationProvider implements AuthenticationProvider {
	private static String CLIENT_PROPERTY_KEY = "spring.security.oauth2.client.registration.";
	Logger logger = LoggerFactory.getLogger(UsernamePasswordAuthenticationProvider.class);
	@Autowired
	private Environment env;
	@Autowired
	private OAuth2AuthorizedClientService authorizedClientService;
	@Autowired
	private OAuth2AuthorizedClientRepository authorizedClientRepository;
	@Autowired
	private ClientRegistrationRepository clientRegistrationRepository;

	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		Session dataSession = PopServlet.getDataSession();

		// TODO maybe customize  "PrincipalExtractor" instead and have the orgAccess/orgMaster as principal https://www.baeldung.com/spring-security-oauth-principal-authorities-extractor
		if (StringUtils.isNotBlank(request.getParameter(PARAM_FACILITYID))) {
			OrgMaster orgMaster = ServletHelper.authenticateOrgMaster(authentication.getName(), (String) authentication.getCredentials(), request.getParameter(PARAM_FACILITYID), dataSession);
			if (orgMaster != null) {
				/**
				 * Creating a new session after login
				 */
				request.getSession(true).setAttribute(SESSION_ORGMASTER,orgMaster);
				return orgMaster.getOrgAccess();
			} else {
				return null;
			}
		} else  {
			OrgAccess orgAccess = ServletHelper.authenticateOrgAccessUsernamePassword(authentication.getName(), (String) authentication.getCredentials(), dataSession);
			request.getSession(true).setAttribute(SESSION_ORGACCESS,orgAccess);
			return orgAccess;
		}
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
	}
}
