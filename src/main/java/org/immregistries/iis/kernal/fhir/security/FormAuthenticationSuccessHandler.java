package org.immregistries.iis.kernal.fhir.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.util.AntPathMatcher;

public class FormAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
	private Logger logger = LoggerFactory.getLogger(this.getClass());


//	@Override
//	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException, ServletException {
//		request.get
//		AuthenticationSuccessHandler.super.onAuthenticationSuccess(request, response, chain, authentication);
//	}

	private final AntPathMatcher antPathMatcher = new AntPathMatcher("/");

//	@Override
//	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
//		logger.info("Authentication success source url ctx path {}\n url {}\n servlet path {}\n location header {}\n location parameter {}\n",
//			request.getContextPath(), request.getRequestURI(), request.getServletPath(), request.getHeader("referer"), request.getParameter("referer"));
//		String refererHeader = request.getHeader("referer");
//		String redirectUrl;
//
//
////		String tenantName = StringUtils.defaultString(request.getParameter(PARAM_TENANT_NAME));
////		if (StringUtils.isNotBlank(tenantName)) {
////			String pathSuffix = "/iis/pop";
//////			antPathMatcher.
////			if (refererHeader.endsWith(pathSuffix) ) {
////				redirectUrl = StringUtils.substringBeforeLast(refererHeader, pathSuffix);
//////				redirectUrl += TENANT_BASE_PATH + "/" + tenantName +
////			}
////		} else {
////			response.sendRedirect("/iis/home");
////		}
//		response.sendRedirect(refererHeader);
//
//	}
}
