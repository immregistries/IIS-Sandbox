package org.immregistries.iis.kernal.controllers.rest;

import org.apache.commons.lang3.Strings;
import org.immregistries.iis.kernal.Application;
import org.immregistries.iis.kernal.controllers.rest.util.RestConstants;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.jetbrains.annotations.NotNull;

public class RestUrlUtil {

	public static final String REST = RestConstants.Path.REST;
	public static final String REST_PATH = RestConstants.Path.REST_PATH;

	public static final String TENANT = RestConstants.Path.TENANT;
	public static final String TENANT_PATH = RestConstants.Path.TENANT_PATH;
	public static final String TENANT_ID = RestConstants.Path.Variables.TENANT_ID;
	public static final String TENANT_ID_PLACEHOLDER = RestConstants.Path.TENANT_ID_PLACEHOLDER;
	public static final String REST_TENANT_PATH = RestConstants.Path.REST_TENANT_PATH;

	public static final String PATIENT = RestConstants.Path.PATIENT;
	public static final String PATIENT_PATH = RestConstants.Path.PATIENT_PATH;
	public static final String PATIENT_ID = RestConstants.Path.Variables.PATIENT_ID;
	public static final String PATIENT_ID_PLACEHOLDER = RestConstants.Path.PATIENT_ID_PLACEHOLDER;
	public static final String PATIENT_BASE_PATH = RestConstants.Path.PATIENT_BASE_PATH;

	public static final String REST_PATIENT_PATH = RestConstants.Path.REST_PATIENT_PATH;

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
		return REST_PATH + TENANT_PATH + "/" + tenantId + PATIENT_PATH + "/" + patientId + urlSuffix;
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
		return REST_PATH + TENANT_PATH + "/" + tenantId + urlSuffix;
	}
}
