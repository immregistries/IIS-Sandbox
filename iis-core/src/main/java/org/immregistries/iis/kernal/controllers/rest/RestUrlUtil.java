package org.immregistries.iis.kernal.controllers.rest;

import org.apache.commons.lang3.Strings;
import org.immregistries.iis.kernal.Application;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.jetbrains.annotations.NotNull;

public class RestUrlUtil {

	public static String tenantifyPathWithContextPath(Tenant tenant, String urlSuffix) {
		if (tenant == null || tenant.getOrgId() < 0) {
			return urlSuffix;
		}
		return Application.IIS_PATH_BASE + tenantifyPathSuffix(tenant.getOrgId(), urlSuffix);
	}

	public static String patientifyPathWithContextPath(Integer tenantId, String patientId, String urlSuffix) {
		if (!Strings.CS.startsWith(urlSuffix, "/")) {
			urlSuffix = "/" + urlSuffix;
		}
		return IisRestPath.BasePath.REST_PATH + IisRestPath.BasePath.TENANT_PATH + "/" + tenantId + IisRestPath.BasePath.PATIENT_PATH + "/" + patientId + urlSuffix;
	}

	/**
	 * Standardized converting url Suffix with tenant Name and variable,
	 * Automatically adds / character if needed
	 * <p>
	 * can also be used for security config with * as tenantName
	 *
	 * @param tenantId  organisation Id
	 * @param urlSuffix
	 * @return {tenantBasePath}/{tenantName}/ + urlSuffix
	 */
	public static @NotNull String tenantifyPathSuffix(int tenantId, String urlSuffix) {
		if (!Strings.CS.startsWith(urlSuffix, "/")) {
			urlSuffix = "/" + urlSuffix;
		}
		return IisRestPath.BasePath.REST_PATH + IisRestPath.BasePath.TENANT_PATH + "/" + tenantId + urlSuffix;
	}
}
