package org.immregistries.iis.kernal.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.controllers.servlet.HomeController;
import org.immregistries.iis.kernal.controllers.servlet.PopController;
import org.immregistries.iis.kernal.services.api.IDeployedApiUrlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

import static org.immregistries.iis.kernal.controllers.servlet.LoginFormController.LOGIN_PARAM_TENANT_NAME;
import static org.immregistries.iis.kernal.controllers.servlet.TenantController.TENANT_BASE_PATH;


public class FormAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
	private Logger logger = LoggerFactory.getLogger(this.getClass());


	@Autowired
	private IDeployedApiUrlService deployedApiUrlService;

	private RequestCache requestCache = new HttpSessionRequestCache();

	private final AntPathMatcher antPathMatcher = new AntPathMatcher("/");

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
		SavedRequest savedRequest = this.requestCache.getRequest(request, response);
		String tenantName = StringUtils.defaultString(request.getParameter(LOGIN_PARAM_TENANT_NAME));

		UriComponentsBuilder builder;
		if (savedRequest == null) {
			builder = ServletUriComponentsBuilder.fromRequest(request);
			builder.replacePath(deployedApiUrlService.getContextPath() + "/");
		} else {
			// Use the DefaultSavedRequest URL
			builder = UriComponentsBuilder.fromHttpUrl(savedRequest.getRedirectUrl());
		}
		// If tenant name was specified in Form
		if (StringUtils.isNotBlank(tenantName)) {
			/*
			 * Filtering redirection for login to go back to homepage or pop page
			 */
			filterForSuffix(builder, deployedApiUrlService.getContextPath() + PopController.POP_BASE_PATH, tenantName, PopController.POP_BASE_PATH);
			filterForSuffix(builder, deployedApiUrlService.getContextPath() + HomeController.HOME_BASE_PATH, tenantName, HomeController.HOME_BASE_PATH);
			filterForSuffix(builder, deployedApiUrlService.getContextPath() + "/", tenantName, HomeController.HOME_BASE_PATH);
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
			String newPath = deployedApiUrlService.getContextPath() + TENANT_BASE_PATH + "/" + tenantName + newSuffix;
			builder.replacePath(newPath);
		}
	}

}
