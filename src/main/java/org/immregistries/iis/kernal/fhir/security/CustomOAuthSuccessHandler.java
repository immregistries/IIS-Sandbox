package org.immregistries.iis.kernal.fhir.security;

import org.hibernate.Session;
import org.immregistries.iis.kernal.model.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static org.immregistries.iis.kernal.fhir.security.ServletHelper.GITHUB_PREFIX;
import static org.immregistries.iis.kernal.fhir.security.ServletHelper.SESSION_TENANT;

@Component
public class CustomOAuthSuccessHandler implements AuthenticationSuccessHandler {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

//	@Override
//	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException, ServletException {
//		this.onAuthenticationSuccess(request, response, authentication);
//
//		chain.doFilter(request, response);
//	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
		logger.info("Authentication success {}",authentication);
		String queryString = "from UserAccess where accessName = ?0";
//		UserAccess userAccess = null;
		Tenant tenant = null;
		if (authentication instanceof OAuth2AuthenticationToken) {
			HttpSession session = request.getSession(true);
			Session dataSession = ServletHelper.getDataSession();
			try {
				OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) authentication;
				tenant = ServletHelper.authenticateTenant(
					oAuth2AuthenticationToken.getPrincipal(),
					GITHUB_PREFIX + oAuth2AuthenticationToken.getPrincipal().getAttribute("login"),
					dataSession);
				dataSession.close();
				session.setAttribute(SESSION_TENANT, tenant);
			} finally {
				dataSession.close();
			}

//			session.setAttribute(SESSION_ORGACCESS, tenant.userAccess);
			// TODO switch to userAccess when facilities creation implemented
		}


		String targetUrl = "/iis/home";

		if (response.isCommitted()) {
			logger.debug(
				"Response has already been committed. Unable to redirect to "
					+ targetUrl);
			return;
		}

		response.sendRedirect(targetUrl);
	}
}
