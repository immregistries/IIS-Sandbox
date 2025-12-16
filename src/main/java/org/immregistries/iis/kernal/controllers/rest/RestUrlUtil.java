package org.immregistries.iis.kernal.controllers.rest;

import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.controllers.servlet.TenantController;
import org.immregistries.iis.kernal.fhir.Application;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.jetbrains.annotations.NotNull;

public class RestUrlUtil {

	public static final String REST = "/rest";
	public static final String REST_TENANT_PATH = REST + TenantController.TENANT_BASE_PATH + "/{tenantId}";

	public static String tenantifyPathWithContextPath(Tenant tenant, String urlSuffix) {
		if (tenant == null || tenant.getOrgId() < 0) {
			return urlSuffix;
		}
		return Application.IIS_PATH_BASE + tenantifyPathSuffix(tenant.getOrgId(), urlSuffix);
	}

	/**
	 * Standardized converting url Suffix with tenant Name and variable,
	 * Automatically adds / character if needed
	 * <p>
	 * can also be used for security config with * as tenantName
	 *
	 * @param tenantId organisation Id
	 * @param urlSuffix
	 * @return {tenantBasePath}/{tenantName}/ + urlSuffix
	 */
	public static @NotNull String tenantifyPathSuffix(int tenantId, String urlSuffix) {
		if (!StringUtils.startsWith(urlSuffix, "/")) {
			urlSuffix = "/" + urlSuffix;
		}
		return "/rest" + TenantController.TENANT_BASE_PATH + "/" + tenantId + urlSuffix;
	}

//	/**
//	 * Deals with tenantName path variable for Authorization config
//	 *
//	 * @param urlSuffix
//	 * @return
//	 */
//	public static @NotNull String securityConfigUrl(String urlSuffix) {
//		return tenantifyPathSuffix("*", urlSuffix);
//	}
}
