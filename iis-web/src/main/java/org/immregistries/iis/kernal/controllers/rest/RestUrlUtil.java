package org.immregistries.iis.kernal.controllers.rest;

import org.apache.commons.lang3.Strings;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.services.api.IDeployedApiUrlService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RestUrlUtil {
	// TODO use AntPAth Matcher
	@Autowired
	private IDeployedApiUrlService deployedApiUrlService;

	public String tenantifyPathWithContextPath(Tenant tenant, String urlSuffix) {
		if (tenant == null || tenant.getOrgId() < 0) {
			return urlSuffix;
		}
		return deployedApiUrlService.getContextPath() + tenantifyPathSuffix(tenant.getOrganizationName(), urlSuffix);
	}

	public String patientifyPathWithContextPath(String tenantName, String patientId, String urlSuffix) {
		if (!Strings.CS.startsWith(urlSuffix, "/")) {
			urlSuffix = "/" + urlSuffix;
		}
		return deployedApiUrlService.getContextPath() + tenantifyPathSuffix(tenantName, IisRestPath.BasePath.PATIENT_PATH + "/" + patientId + urlSuffix);
	}

	/**
	 * Standardized converting url Suffix with tenant Name and variable,
	 * Automatically adds / character if needed
	 * <p>
	 * can also be used for security config with * as tenantName
	 *
	 * @param tenantName  organisation Id
	 * @param urlSuffix
	 * @return {tenantBasePath}/{tenantName}/ + urlSuffix
	 */
	public @NotNull String tenantifyPathSuffix(String tenantName, String urlSuffix) {
		if (!Strings.CS.startsWith(urlSuffix, "/")) {
			urlSuffix = "/" + urlSuffix;
		}
		return IisRestPath.BasePath.REST_PATH + IisRestPath.BasePath.TENANT_PATH + "/" + tenantName + urlSuffix;
	}
}
