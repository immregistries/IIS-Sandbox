package org.immregistries.iis.kernal.fhir.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.immregistries.iis.kernal.HibernateConfig;
import org.immregistries.iis.kernal.fhir.Application;
import org.immregistries.iis.kernal.model.persisted.Tenant;
import org.immregistries.iis.kernal.model.persisted.UserAccess;
import org.immregistries.iis.kernal.servlet.TenantController;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

public class CurrentTenantUtil {

    public static final String TENANT_ID_URL = "TENANT_ID_URL";
    public static final String TENANT_NAME_URL = "TENANT_NAME_URL";

    public static final String SESSION_REQUEST_TENANT = "tenant";

    public static Tenant getTenant(String pathVariable, HttpServletRequest request, Session dataSession) {
        Tenant tenant = null;
        if (StringUtils.isBlank(pathVariable)) {
            tenant = getTenant(request);
        } else {
            UserAccess userAccess = null;
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof UserAccess) {
                userAccess = (UserAccess) authentication;
            }
            tenant = TenantUtil.authenticateTenant(userAccess, pathVariable, dataSession, null);
        }
        // if (tenant == null) {
        // throw new AuthenticationCredentialsNotFoundException("");
        // }
        request.setAttribute(SESSION_REQUEST_TENANT, tenant);
        return tenant;
    }

    public static Tenant getTenant(HttpServletRequest request, Session existingDataSession) {
        final Tenant tenant;
        Tenant requestTenant = (Tenant) request.getAttribute(SESSION_REQUEST_TENANT);
        String urlTenantName = (String) request.getAttribute(TENANT_NAME_URL);
		 Object attribute = request.getAttribute(TENANT_ID_URL);
		 int urlTenantId = 0;
		 if (attribute != null) {
			 urlTenantId = (int) attribute;
		 }

        if (urlTenantId > 0) {
            try (Session dataSession = HibernateConfig.getDataSession()) {
                tenant = TenantUtil.getTenantByIdAuthenticated(urlTenantId, dataSession);
            }
        } else if (StringUtils.isNotBlank(urlTenantName)) {
            if (requestTenant != null && StringUtils.equals(requestTenant.getOrganizationName(), urlTenantName)) {
                tenant = requestTenant;
            } else if (existingDataSession != null) {
                tenant = getTenant(urlTenantName, request, existingDataSession);
            } else
                try (Session dataSession = HibernateConfig.getDataSession()) {
                    tenant = getTenant(urlTenantName, request, dataSession);
                }
        } else {
            tenant = requestTenant;
        }
        return tenant;
    }

    public static Tenant getTenant(HttpServletRequest request) {
        return getTenant(request, null);
    }

    public static @NotNull Tenant getTenantRedirectIfNone(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        return getTenantRedirectIfNone(req, resp, null);
    }

    public static @NotNull Tenant getTenantRedirectIfNone(HttpServletRequest req, HttpServletResponse resp,
            Session existingDataSession) throws IOException {
        Tenant tenant = getTenant(req, existingDataSession);
        if (tenant == null) {
            if (UserAccessUtil.getUserAccess() != null) {
                resp.sendRedirect(Application.IIS_PATH_BASE + TenantController.TENANT_BASE_PATH);
            }
            throw new AuthenticationCredentialsNotFoundException("");
        }
        return tenant;
    }

    public static Tenant getTenant() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        return getTenant(request);
    }

}
