package org.immregistries.iis.kernal.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.fhir.security.TenantUtil;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class TenantRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(TenantRequestLoggingFilter.class);
    private static final String TENANT_PREFIX = "/rest/tenant/";
    public static final String TENANT_REQUEST_ATTRIBUTE = "tenant";

	@Autowired
	TenantUtil tenantUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
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
					Tenant tenant = tenantUtil.getTenantByIdAuthenticated(tenantIdInt);
                logger.info("Request for tenant {}: {}", tenant.getOrganizationName(), path);
                request.setAttribute(CurrentTenantUtil.TENANT_ID_URL, tenantId);
            } catch (NumberFormatException e) {
                logger.warn("Invalid tenant ID format in URL: {}", tenantId);
            } catch (Exception e) {
                logger.warn("Could not authenticate tenant for logging: {}", e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}
