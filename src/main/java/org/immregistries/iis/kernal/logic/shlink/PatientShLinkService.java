package org.immregistries.iis.kernal.logic.shlink;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.Application;
import org.immregistries.iis.kernal.controllers.rest.RestUrlUtil;
import org.immregistries.iis.kernal.controllers.rest.util.RestConstants;
import org.immregistries.iis.kernal.fhir.shl.ShLinkPayload;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
public class PatientShLinkService {

	public String getPatientShLinkQrCode(HttpServletRequest req, IAnyResource patientSelected, Tenant tenant) {
		String manifestUrl = getManifestUrl(req, patientSelected, tenant);
		ShLinkPayload shLinkPayload = generatePatientShLinkPayload(manifestUrl);
		return ShLinkPayloadUtil.toBase64QrCode(shLinkPayload);
	}

	public @NotNull ShLinkPayload generatePatientShLinkPayload(String manifestUrl) {
		ShLinkPayload shLinkPayload = new ShLinkPayload();
		shLinkPayload.setUrl(manifestUrl);
		shLinkPayload.setLabel("Generated for testing");
		shLinkPayload.setKey(null);
		shLinkPayload.setFlag("LP");
		shLinkPayload.setExp(10000000L);
		return shLinkPayload;
	}

	public @NotNull String getManifestUrl(HttpServletRequest req, IAnyResource patientSelected, Tenant tenant) {
		// Adjusting Base URL for REST
		String baseUrl = StringUtils.substringBefore(req.getRequestURL().toString(), Application.IIS_PATH_BASE);
		return getManifestUrl(baseUrl, patientSelected, tenant);
	}

	public @NotNull String getManifestUrl(String baseUrl, IAnyResource patientSelected, Tenant tenant) {
		return baseUrl + RestUrlUtil.tenantifyPathWithContextPath(tenant,
				RestConstants.Path.MANIFEST_PATH_SUFFIX
						+ "/patient/" + patientSelected.getIdElement().getIdPart());
	}

}
