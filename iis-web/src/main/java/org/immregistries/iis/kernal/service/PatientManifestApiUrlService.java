package org.immregistries.iis.kernal.service;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.controllers.rest.RestUrlUtil;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.services.api.IDeployedApiUrlService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class PatientManifestApiUrlService implements org.immregistries.iis.kernal.logic.shlink.IPatientManifestApiUrlService {
	@Autowired
	private IDeployedApiUrlService deployedApiUrlService;
	@Autowired
	private RestUrlUtil restUrlUtil;

	@Override
	public @NotNull String getManifestUrl(HttpServletRequest req, IAnyResource patientSelected, Tenant tenant) {
		// Adjusting Base URL for REST
		String baseUrl = StringUtils.substringBefore(req.getRequestURL().toString(), deployedApiUrlService.getContextPath());
		return getManifestUrl(baseUrl, patientSelected, tenant);
	}

	@Override
	public @NotNull String getManifestUrl(String baseUrl, IAnyResource patientSelected, Tenant tenant) {
		return baseUrl + restUrlUtil.tenantifyPathWithContextPath(tenant,
			IisRestPath.BasePath.PATIENT_MANIFEST_PATH
				+ "/patient/" + patientSelected.getIdElement().getIdPart());
	}
}
