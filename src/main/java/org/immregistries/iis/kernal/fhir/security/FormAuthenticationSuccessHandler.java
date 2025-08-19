package org.immregistries.iis.kernal.fhir.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.fhir.Application;
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

		logger.info("targetUrl CALLED, {}, {}", requestCache, tenantName);

		UriComponentsBuilder builder;
		if (savedRequest == null) {
			builder = ServletUriComponentsBuilder.fromRequest(request);
			builder.replacePath("/iis/");
		} else {
			// Use the DefaultSavedRequest URL
			builder = UriComponentsBuilder.fromHttpUrl(savedRequest.getRedirectUrl());
		}
		if (StringUtils.isNotBlank(tenantName)) {
			filterForSuffix(builder, "/iis/pop", tenantName, "/pop");
			filterForSuffix(builder, "/iis/home", tenantName, "/home");
			filterForSuffix(builder, "/iis/", tenantName, "/home");
		}

		clearAuthenticationAttributes(request);
		getRedirectStrategy().sendRedirect(request, response, builder.build().toUri().toURL().toString());
	}

	private void filterForSuffix(UriComponentsBuilder builder, String pathSuffix, String tenantName, String newSuffix) throws MalformedURLException {
		if (StringUtils.endsWith(builder.build().getPath(), pathSuffix)) {
			String newPath = Application.IIS_PATH_BASE + TENANT_BASE_PATH + "/" + tenantName + newSuffix;
			builder.replacePath(newPath);
		}
		String resultUrl = builder.build().toUri().toURL().toString();
//		logger.info("Transfo {} {} ", pathSuffix, resultUrl);
	}

}
