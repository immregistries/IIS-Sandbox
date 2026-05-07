package org.immregistries.iis.kernal.controllers.rest.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.immregistries.iis.kernal.IisRequestAttribute;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.security.RequestTenantUtil;
import org.immregistries.iis.kernal.security.TenantAuthService;
import org.immregistries.iis.kernal.security.UserAccessUtil;
import org.immregistries.iis.kernal.services.api.IDeployedApiUrlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Service
@WebFilter
public class RestTenantUrlFilter extends OncePerRequestFilter {

	private final AntPathMatcher antPathMatcher = new AntPathMatcher();
	private final String tenantPattern;
	private final String patientManifestPattern;
	private final String storedManifestPattern;
	@Autowired
	public RestTenantUrlFilter(IDeployedApiUrlService deployedApiUrlService) {
		tenantPattern = deployedApiUrlService.getContextPath() + IisRestPath.BasePath.REST_PATH + IisRestPath.BasePath.TENANT_PATH + "/**";
		patientManifestPattern = deployedApiUrlService.getContextPath() + IisRestPath.PATIENT_MANIFEST_FULL_PATH + "/**";
		storedManifestPattern = deployedApiUrlService.getContextPath() + IisRestPath.SH_LINKS_STORED_MANIFEST_FULL_PATH + "/**";
	}



	private static final Logger logger = LoggerFactory.getLogger(RestTenantUrlFilter.class);


	@Autowired
	private TenantAuthService tenantAuthService;
	@Autowired
	private RequestTenantUtil requestTenantUtil;
	@Autowired
	private UserAccessUtil userAccessUtil;

	@Override
	public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String path = request.getRequestURI();
		if (antPathMatcher.match(tenantPattern, path)) {
			String remainingPath = antPathMatcher.extractPathWithinPattern(tenantPattern, path);
			int slashIndex = remainingPath.indexOf('/');
			String tenantName;
			if (slashIndex > 0) {
				tenantName = remainingPath.substring(0, slashIndex);
			} else {
				tenantName = remainingPath;
			}
			try {
				Tenant tenant = tenantAuthService.authenticateTenant(userAccessUtil.getUserAccess(), tenantName);
				request.setAttribute(IisRequestAttribute.TENANT_NAME_URL, tenantName);
				requestTenantUtil.setTenantForRequest(tenant, request);
			} catch (Exception e) {
				/*
				 * For Smart health links manifest retrieval, authentication is dealt with later
				 * or well known key
				 */
				if (antPathMatcher.match(patientManifestPattern, path) || antPathMatcher.match(storedManifestPattern, path)) {
					filterChain.doFilter(request, response);
				} else {
					logger.warn("Could not authenticate tenant for logging: {}", e.getMessage());
				}
			}
		}
		filterChain.doFilter(request, response);
	}
}
