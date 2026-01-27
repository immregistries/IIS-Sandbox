package org.immregistries.iis.kernal.controllers.rest.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.immregistries.iis.kernal.Application;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.security.TenantAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import org.immregistries.iis.kernal.controllers.RestConstants;

@Service
@WebFilter
public class RestTenantUrlFilter extends OncePerRequestFilter {

	public static final String TENANT_REQUEST_ATTRIBUTE = CurrentTenantUtil.SESSION_REQUEST_TENANT;
	private static final Logger logger = LoggerFactory.getLogger(RestTenantUrlFilter.class);
	private static final String TENANT_PREFIX = Application.IIS_PATH_BASE + RestConstants.Path.REST_PATH + "/tenant/";

	@Autowired
	private TenantAuthService tenantAuthService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String path = request.getRequestURI();
		/*
		 * For Smart health links manifest retrieval, authentication is dealt with later
		 * or well known key
		 */
		if (path.startsWith(RestConstants.Path.MANIFEST_FULL_PATH)) {
			filterChain.doFilter(request, response);
		}
		if (path.startsWith(TENANT_PREFIX)) {
			String remainingPath = path.substring(TENANT_PREFIX.length());
			int slashIndex = remainingPath.indexOf('/');
			String tenantId;
			if (slashIndex > 0) {
				tenantId = remainingPath.substring(0, slashIndex);
			} else {
				tenantId = remainingPath;
			}
			try {
				int tenantIdInt = Integer.parseInt(tenantId);
				Tenant tenant = tenantAuthService.getTenantByIdAuthenticated(tenantIdInt);
				request.setAttribute(CurrentTenantUtil.TENANT_ID_URL, tenantId);
				request.setAttribute(TENANT_REQUEST_ATTRIBUTE, tenant);
			} catch (NumberFormatException e) {
				logger.warn("Invalid tenant ID format in URL: {}", tenantId);
			} catch (Exception e) {
				logger.warn("Could not authenticate tenant for logging: {}", e.getMessage());
			}
		}
		filterChain.doFilter(request, response);
	}
}
