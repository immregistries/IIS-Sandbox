package org.immregistries.iis.kernal.controllers.servlet.util;

import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.controllers.servlet.TenantController;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.services.api.IDeployedApiUrlService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UrlTenantUtil {
	@Autowired
	private IDeployedApiUrlService deployedApiUrlService;
	/**
	 * Adds tenant prefix to urlSuffix if tenant is not null
	 * Used for links in the UI with href
	 *
	 * @param tenant    tenant
	 * @param urlSuffix
	 * @return {tenantBasePath}/{tenantName}/ + urlSuffix
	 */
	public String tenantifyPathWithContextPath(Tenant tenant, String urlSuffix) {
		if (tenant == null || tenant.getOrgId() < 0) {
			return urlSuffix;
		}
		String organizationName = tenant.getOrganizationName();
		return deployedApiUrlService.getContextPath() + tenantifyPathSuffix(organizationName, urlSuffix);
	}

	/**
	 * Standardized converting url Suffix with tenant Name and variable,
	 * Automatically adds / character if needed
	 * <p>
	 * can also be used for security config with * as tenantName
	 *
	 * @param tenantName organisation name
	 * @param urlSuffix
	 * @return {tenantBasePath}/{tenantName}/ + urlSuffix
	 */
	public @NotNull String tenantifyPathSuffix(String tenantName, String urlSuffix) {
		if (!StringUtils.startsWith(urlSuffix, "/")) {
			urlSuffix = "/" + urlSuffix;
		}
		return TenantController.TENANT_BASE_PATH + "/" + tenantName + urlSuffix;
	}

	/**
	 * Deals with tenantName path variable for Authorization config
	 *
	 * @param urlSuffix
	 * @return
	 */
	public @NotNull String securityConfigUrl(String urlSuffix) {
		return tenantifyPathSuffix("*", urlSuffix);
	}
}
