package org.immregistries.iis.kernal.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.immregistries.iis.kernal.controllers.servlet.HomeController;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.services.api.IDeployedApiUrlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class IisOAuthSuccessHandler implements AuthenticationSuccessHandler {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private TenantAuthService tenantAuthService;
	@Autowired
	private IDeployedApiUrlService deployedApiUrlService;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
													Authentication authentication) throws IOException {
		logger.info("Authentication success {}", authentication);
		String queryString = "from UserAccess where accessName = ?1";
		// UserAccess userAccess = null;
		Tenant tenant = null;
		if (authentication instanceof OAuth2AuthenticationToken) {
			HttpSession session = request.getSession(true);
			OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) authentication;
			tenant = tenantAuthService.authenticateTenant(
					oAuth2AuthenticationToken.getPrincipal(),
					UserAccessUtil.GITHUB_PREFIX + oAuth2AuthenticationToken.getPrincipal().getAttribute("login"));
			session.setAttribute(CurrentTenantUtil.SESSION_REQUEST_TENANT, tenant);
			// session.setAttribute(SESSION_ORGACCESS, tenant.userAccess);
			// TODO switch to userAccess when facilities creation implemented
		}

		String targetUrl = deployedApiUrlService.getContextPath() + HomeController.HOME_BASE_PATH;

		if (response.isCommitted()) {
			logger.debug(
					"Response has already been committed. Unable to redirect to "
							+ targetUrl);
			return;
		}

		response.sendRedirect(targetUrl);
	}
}
