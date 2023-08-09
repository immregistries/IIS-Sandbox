package org.immregistries.iis.kernal.fhir.security;

import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.servlet.PopServlet;
import org.immregistries.iis.kernal.servlet.ServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {
	Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException, ServletException {
		this.onAuthenticationSuccess(request, response, authentication);
		chain.doFilter(request, response);
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
		logger.info("SUCCESS {} ", authentication);
		if (authentication instanceof OAuth2AuthenticationToken) {
			OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) authentication;
			OAuth2User oAuth2User = oAuth2AuthenticationToken.getPrincipal();
			OrgAccess orgAccess = null;
			orgAccess = ServletHelper.authenticateOrgAccess(
				"github-" + oAuth2User.getAttribute("login"),
				oAuth2User.getName(),
				"github-" + oAuth2User.getAttribute("login"),
				PopServlet.getDataSession());

			request.getSession(false).setAttribute("orgAccess", orgAccess);
		}
	}
}
