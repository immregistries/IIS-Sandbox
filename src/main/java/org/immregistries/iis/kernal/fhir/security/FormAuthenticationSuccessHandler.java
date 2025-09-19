package org.immregistries.iis.kernal.fhir.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.fhir.Application;
import org.immregistries.iis.kernal.servlet.HomeController;
import org.immregistries.iis.kernal.servlet.PopController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.MalformedURLException;

import static org.immregistries.iis.kernal.servlet.LoginController.LOGIN_PARAM_TENANT_NAME;
import static org.immregistries.iis.kernal.servlet.TenantController.TENANT_BASE_PATH;

public class FormAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
	private Logger logger = LoggerFactory.getLogger(this.getClass());


	private RequestCache requestCache = new HttpSessionRequestCache();

	private final AntPathMatcher antPathMatcher = new AntPathMatcher("/");

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
		SavedRequest savedRequest = this.requestCache.getRequest(request, response);
		String tenantName = StringUtils.defaultString(request.getParameter(LOGIN_PARAM_TENANT_NAME));

		UriComponentsBuilder builder;
		if (savedRequest == null) {
			builder = ServletUriComponentsBuilder.fromRequest(request);
			builder.replacePath(Application.IIS_PATH_BASE + "/");
		} else {
			// Use the DefaultSavedRequest URL
			builder = UriComponentsBuilder.fromHttpUrl(savedRequest.getRedirectUrl());
		}
		// If tenant name was specified in Form
		if (StringUtils.isNotBlank(tenantName)) {
			/*
			 * Filtering redirection for login to go back to homepage or pop page
			 */
			filterForSuffix(builder, Application.IIS_PATH_BASE + PopController.POP_BASE_PATH, tenantName, PopController.POP_BASE_PATH);
			filterForSuffix(builder, Application.IIS_PATH_BASE + HomeController.HOME_BASE_PATH, tenantName, HomeController.HOME_BASE_PATH);
			filterForSuffix(builder, Application.IIS_PATH_BASE + "/", tenantName, HomeController.HOME_BASE_PATH);
		} else {
			/**
			 * Redirect to tenant page to suggest selecting the tenant
			 */
//			builder.replacePath(Application.IIS_PATH_BASE + TENANT_BASE_PATH);
		}

		clearAuthenticationAttributes(request);
		getRedirectStrategy().sendRedirect(request, response, builder.build().toUri().toURL().toString());
	}

	private void filterForSuffix(UriComponentsBuilder builder, String pathSuffix, String tenantName, String newSuffix) throws MalformedURLException {
		if (StringUtils.endsWith(builder.build().getPath(), pathSuffix)) {
			String newPath = Application.IIS_PATH_BASE + TENANT_BASE_PATH + "/" + tenantName + newSuffix;
			builder.replacePath(newPath);
		}
	}

}
