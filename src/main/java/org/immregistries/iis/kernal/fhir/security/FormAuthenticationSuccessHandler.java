package org.immregistries.iis.kernal.fhir.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

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

		if (savedRequest == null) {
			super.onAuthenticationSuccess(request, response, authentication);
			return;
		}
		clearAuthenticationAttributes(request);
		// Use the DefaultSavedRequest URL
		String targetUrl = savedRequest.getRedirectUrl();

		logger.info("targetUrl {}", targetUrl);
		logger.info("Authentication success source url ctx path {}\n url {}\n servlet path {}\n location header {}\n location parameter {}\n",
			request.getContextPath(), request.getRequestURI(), request.getServletPath(), request.getHeader("referer"), request.getParameter("referer"));
		String redirectUrl = targetUrl;

		if (StringUtils.isNotBlank(tenantName)) {
			logger.info(" match 1 {} \n match 2 {} \n match 3 {} \n",
				antPathMatcher.match(targetUrl, "/pop"),
				antPathMatcher.match(targetUrl, "/iis/pop"),
				antPathMatcher.match(targetUrl, "/iis/pop"));
			redirectUrl = filterForSuffix(redirectUrl, "/pop", tenantName);
			redirectUrl = filterForSuffix(redirectUrl, "/home", tenantName);
		} else {
			response.sendRedirect("/iis/home");
		}
		getRedirectStrategy().sendRedirect(request, response, redirectUrl);
	}

	private String filterForSuffix(String targetUrl, String pathSuffix, String tenantName) throws MalformedURLException {
		URL url = new URL(targetUrl);
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(targetUrl);
		logger.info("Source {}, parsed {}, path {}, suffix {}", targetUrl, builder.build(), url.getPath(), pathSuffix);


		if (StringUtils.endsWith(url.getPath(), pathSuffix)) {
			logger.info("MATCH");
			String newPath = TENANT_BASE_PATH + "/" + tenantName + pathSuffix;
			String params = StringUtils.substringAfter(targetUrl, "?");
			builder.replacePath(newPath);
		}
		String resultUrl = builder.build().toUri().toURL().toString();

		logger.info("Transfo {} {} {}", targetUrl, pathSuffix, resultUrl);
		return resultUrl;
	}

}
