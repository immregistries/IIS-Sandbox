package org.immregistries.iis.kernal.controllers.servlet.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import org.immregistries.iis.kernal.Application;
import org.immregistries.iis.kernal.controllers.servlet.TenantController;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.security.UserAccessUtil;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import java.io.IOException;

public class RedirectUtil {

	public static @NotNull Tenant getTenantRedirectIfNone(HttpServletRequest req,
																			HttpServletResponse resp) throws IOException {
		Tenant tenant = CurrentTenantUtil.getTenant(req);
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