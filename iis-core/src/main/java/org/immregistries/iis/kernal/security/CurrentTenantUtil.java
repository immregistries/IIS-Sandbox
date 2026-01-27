package org.immregistries.iis.kernal.security;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.FunctionalConstants;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * static class providing tools related to the Tenant selected using request
 * context
 */
public final class CurrentTenantUtil {

	public static final String TENANT_ID_URL = FunctionalConstants.TENANT_ID_URL;
	public static final String TENANT_NAME_URL = FunctionalConstants.TENANT_NAME_URL;

	public static final String SESSION_REQUEST_TENANT = FunctionalConstants.SESSION_REQUEST_TENANT;

	public static Tenant getTenant() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
				.getRequest();
		return getTenant(request);
	}

	public static Tenant getTenant(HttpServletRequest request) {
		final Tenant tenant;
		/*
		 * Extracting variables from the Request
		 */
		/*
		 * If Tenant was already set as attribute return it
		 */
		Tenant requestTenant = (Tenant) request.getAttribute(SESSION_REQUEST_TENANT);
		if (requestTenant != null) {
			tenant = requestTenant;
		} else {
			String urlTenantName = (String) request.getAttribute(TENANT_NAME_URL);
			Object tenantIdUrlAttribute = request.getAttribute(TENANT_ID_URL);
			int urlTenantId = 0;
			if (tenantIdUrlAttribute != null) {
				urlTenantId = (int) tenantIdUrlAttribute;
			}

			/*
			 * if Tenant Id specified
			 * else check if name
			 */
			if (urlTenantId > 0) {
//				tenant = tenantUtil.getTenantByIdAuthenticated(urlTenantId);
//				request.setAttribute(SESSION_REQUEST_TENANT, tenant);
				tenant = null; //TODO change
			} else if (StringUtils.isNotBlank(urlTenantName)) {
				tenant = getTenantFromName(urlTenantName);
				request.setAttribute(SESSION_REQUEST_TENANT, tenant);
			} else {
				tenant = null;
			}
		}
		return tenant;
	}

	public static Tenant getTenantFromName(String pathVariable) {
		Tenant tenant = null;
		if (StringUtils.isNotBlank(pathVariable)) {
			UserAccess userAccess = null;
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (authentication instanceof UserAccess) {
				userAccess = (UserAccess) authentication;
			}
			tenant = TenantAuthService.get().authenticateTenant(userAccess, pathVariable);
		}
		return tenant;
	}

}
