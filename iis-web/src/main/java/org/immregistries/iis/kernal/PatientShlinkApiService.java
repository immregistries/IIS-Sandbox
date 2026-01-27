package org.immregistries.iis.kernal;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.controllers.rest.RestUrlUtil;
import org.immregistries.iis.kernal.fhir.shl.ShLinkPayload;
import org.immregistries.iis.kernal.logic.shlink.PatientShLinkGenerator;
import org.immregistries.iis.kernal.logic.shlink.ShLinkPayloadUtil;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class PatientShlinkApiService {

	@Autowired
	private PatientShLinkGenerator patientShLinkGenerator;

	public String getPatientShLinkQrCode(HttpServletRequest req, IAnyResource patientSelected, Tenant tenant) {
		String manifestUrl = getManifestUrl(req, patientSelected, tenant);
		ShLinkPayload shLinkPayload = patientShLinkGenerator.generatePatientShLinkPayload(manifestUrl);
		return ShLinkPayloadUtil.toBase64QrCode(shLinkPayload);
	}

	public @NotNull String getManifestUrl(HttpServletRequest req, IAnyResource patientSelected, Tenant tenant) {
		// Adjusting Base URL for REST
		String baseUrl = StringUtils.substringBefore(req.getRequestURL().toString(), Application.IIS_PATH_BASE);
		return getManifestUrl(baseUrl, patientSelected, tenant);
	}

	public @NotNull String getManifestUrl(String baseUrl, IAnyResource patientSelected, Tenant tenant) {
		return baseUrl + RestUrlUtil.tenantifyPathWithContextPath(tenant,
			IisRestPath.BasePath.MANIFEST_PATH
				+ "/patient/" + patientSelected.getIdElement().getIdPart());
	}
}
