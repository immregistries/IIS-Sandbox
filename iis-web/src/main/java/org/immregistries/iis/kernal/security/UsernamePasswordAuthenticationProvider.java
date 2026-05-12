package org.immregistries.iis.kernal.security;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
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

import static org.immregistries.iis.kernal.controllers.servlet.LoginFormController.LOGIN_PARAM_TENANT_NAME;

@Component
public class UsernamePasswordAuthenticationProvider implements AuthenticationProvider {
	private static String CLIENT_PROPERTY_KEY = "spring.security.oauth2.client.registration.";
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private Environment env;
	@Autowired
	private OAuth2AuthorizedClientService authorizedClientService;
	@Autowired
	private OAuth2AuthorizedClientRepository authorizedClientRepository;
	@Autowired
	private ClientRegistrationRepository clientRegistrationRepository;
	@Autowired
	private TenantAuthService tenantAuthService;
	@Autowired
	private UserAccessUtil userAccessUtil;

	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
				.getRequest();

		// TODO maybe customize "PrincipalExtractor" instead and have the
		// userAccess/tenant as principal
		// https://www.baeldung.com/spring-security-oauth-principal-authorities-extractor
		if (StringUtils.isNotBlank(request.getParameter(LOGIN_PARAM_TENANT_NAME))) {
			Tenant tenant = tenantAuthService.authenticateTenant(authentication.getName(),
					(String) authentication.getCredentials(), request.getParameter(LOGIN_PARAM_TENANT_NAME));
			if (tenant != null) {
				/**
				 * Creating a new session after login
				 */
				request.getSession(true);
//				request.getSession(true).setAttribute(GlobalConstants.SESSION_REQUEST_TENANT, tenant);
				return tenant.getUserAccess();
			} else {
				return null;
			}
		} else {
			UserAccess userAccess = userAccessUtil.authenticateUserAccessUsernamePassword(authentication.getName(),
					(String) authentication.getCredentials());
			request.getSession(true).setAttribute(UserAccessUtil.SESSION_USER_ACCESS, userAccess);
			return userAccess;
		}
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
	}
}
