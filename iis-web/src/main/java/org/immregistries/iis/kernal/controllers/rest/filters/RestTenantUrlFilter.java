package org.immregistries.iis.kernal.controllers.rest.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.immregistries.iis.kernal.GlobalConstants;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.security.TenantAuthService;
import org.immregistries.iis.kernal.security.UserAccessUtil;
import org.immregistries.iis.kernal.services.api.IDeployedApiUrlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Service
@WebFilter
public class RestTenantUrlFilter extends OncePerRequestFilter {
	@Autowired
	private IDeployedApiUrlService deployedApiUrlService;

	public static final String TENANT_REQUEST_ATTRIBUTE = GlobalConstants.SESSION_REQUEST_TENANT;
	private static final Logger logger = LoggerFactory.getLogger(RestTenantUrlFilter.class);

	private String tenantPrefix() {
		return deployedApiUrlService.getContextPath() + IisRestPath.BasePath.REST_PATH + "/tenant/";
	}

	@Autowired
	private TenantAuthService tenantAuthService;

	@Autowired
	private UserAccessUtil userAccessUtil;

	@Override
	public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String path = request.getRequestURI();
		/*
		 * For Smart health links manifest retrieval, authentication is dealt with later
		 * or well known key
		 */
		if (path.startsWith(IisRestPath.MANIFEST_FULL_PATH)) {
			filterChain.doFilter(request, response);
		}
		if (path.startsWith(tenantPrefix())) {
			/**
			 * TODO optimize prefix length calculus
			 */
			String remainingPath = path.substring(tenantPrefix().length());
			int slashIndex = remainingPath.indexOf('/');
			String tenantName;
			if (slashIndex > 0) {
				tenantName = remainingPath.substring(0, slashIndex);
			} else {
				tenantName = remainingPath;
			}
			try {
				Tenant tenant = tenantAuthService.authenticateTenant(userAccessUtil.getUserAccess(), tenantName);
				request.setAttribute(GlobalConstants.TENANT_NAME_URL, tenantName);
				request.setAttribute(TENANT_REQUEST_ATTRIBUTE, tenant);
			} catch (Exception e) {
				logger.warn("Could not authenticate tenant for logging: {}", e.getMessage());
			}
		}
		filterChain.doFilter(request, response);
	}
}
