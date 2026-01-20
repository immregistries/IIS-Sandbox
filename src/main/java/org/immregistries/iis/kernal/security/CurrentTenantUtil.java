package org.immregistries.iis.kernal.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.Application;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.persisted.model.UserAccess;
import org.immregistries.iis.kernal.controllers.servlet.TenantController;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

/**
 * static class providing tools related to the Tenent selected using request
 * context
 */
public class CurrentTenantUtil {

	public static final String TENANT_ID_URL = "TENANT_ID_URL";
	public static final String TENANT_NAME_URL = "TENANT_NAME_URL";

	public static final String SESSION_REQUEST_TENANT = "tenant";

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

	public static @NotNull Tenant getTenantRedirectIfNone(HttpServletRequest req,
			HttpServletResponse resp) throws IOException {
		Tenant tenant = getTenant(req);
		if (tenant == null) {
			if (UserAccessUtil.get().getUserAccess() != null) {
				resp.sendRedirect(Application.IIS_PATH_BASE +
						TenantController.TENANT_BASE_PATH);
			}
			throw new AuthenticationCredentialsNotFoundException("");
		}
		return tenant;
	}

}
