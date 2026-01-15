package org.immregistries.iis.kernal.controllers.rest.shlink;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.Application;
import org.immregistries.iis.kernal.controllers.rest.RestUrlUtil;
import org.immregistries.iis.kernal.fhir.shl.ShLinkPayload;
import org.immregistries.iis.kernal.logic.shlink.CompressionService;
import org.immregistries.iis.kernal.logic.shlink.ShLinkUtilService;
import org.immregistries.iis.kernal.mapping.internalClient.IisFhirClientFactory;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.security.CurrentTenantUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RestController
@RequestMapping({RestUrlUtil.REST + RestUrlUtil.PATIENT_BASE_PATH + PatientShLinkRestController.SHLINK_QR_CODE_PATH_SUFFIX, RestUrlUtil.REST_TENANT_PATH + RestUrlUtil.PATIENT_BASE_PATH + PatientShLinkRestController.SHLINK_QR_CODE_PATH_SUFFIX})
public class PatientShLinkRestController {

	public static final String SHLINK_QR_CODE_PATH_SUFFIX = "/qr";

	@Autowired
	private ShLinkUtilService shLinkUtilService;
	@Autowired
	private IisFhirClientFactory iisFhirClientFactory;
	@Autowired
	private CompressionService compressionService;

	@GetMapping( produces = MediaType.IMAGE_PNG_VALUE)
	public ResponseEntity<byte[]> doGetShLinkQrCode(HttpServletRequest req, HttpServletResponse resp,
																	@RequestAttribute(CurrentTenantUtil.SESSION_REQUEST_TENANT) Tenant tenant,
																	@PathVariable(RestUrlUtil.PATIENT_ID) String patientId)
		throws IOException, ServletException {
		IGenericClient client = iisFhirClientFactory.newGenericClient(tenant, req);
		IAnyResource patientSelected = (IAnyResource) client.read().resource("Patient").withId(patientId).execute();
		if (patientSelected == null) {
			throw new RuntimeException("Patient not found");
		}
		String qrCode = getQrCode(req, patientSelected, tenant);
		ByteArrayOutputStream byteArrayOutputStreamPNG = compressionService.toQrCodeStreamPNG(qrCode);
		return ResponseEntity.ok(byteArrayOutputStreamPNG.toByteArray());

	}

	public String getQrCode(HttpServletRequest req, IAnyResource patientSelected, Tenant tenant) {
		String manifestUrl = getManifestUrl(req, patientSelected, tenant);
		ShLinkPayload shLinkPayload = generatePatientShLinkPayload(manifestUrl);
		return shLinkUtilService.qrCode(shLinkPayload);
	}

	public static @NotNull ShLinkPayload generatePatientShLinkPayload(String manifestUrl) {
		ShLinkPayload shLinkPayload = new ShLinkPayload();
		shLinkPayload.setUrl(manifestUrl);
		shLinkPayload.setLabel("Generated for testing");
		shLinkPayload.setKey(null);
		shLinkPayload.setFlag("LP");
		shLinkPayload.setExp(10000000L);
		return shLinkPayload;
	}

	public static @NotNull String getManifestUrl(HttpServletRequest req, IAnyResource patientSelected, Tenant tenant) {
		// Adjusting Base URL for REST
		String baseUrl = StringUtils.substringBefore(req.getRequestURL().toString(), Application.IIS_PATH_BASE);
		return getManifestUrl(baseUrl, patientSelected, tenant);
	}

	public static @NotNull String getManifestUrl(String baseUrl, IAnyResource patientSelected, Tenant tenant) {
		return baseUrl + RestUrlUtil.tenantifyPathWithContextPath(tenant,
			PatientShLinkManifestRestController.MANIFEST_PATH_SUFFIX
				+ "/patient/" + patientSelected.getIdElement().getIdPart());
	}

}
