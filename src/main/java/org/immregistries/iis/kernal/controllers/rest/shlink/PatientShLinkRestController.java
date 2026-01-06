package org.immregistries.iis.kernal.controllers.rest.shlink;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.controllers.rest.RestUrlUtil;
import org.immregistries.iis.kernal.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.fhir.shl.ShLinkPayload;
import org.immregistries.iis.kernal.logic.shlink.CompressionService;
import org.immregistries.iis.kernal.logic.shlink.ShLinkUtilService;
import org.immregistries.iis.kernal.mapping.internalClient.IisFhirClientFactory;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RestController
@RequestMapping({ RestUrlUtil.REST_KEY + "/patient", RestUrlUtil.REST_KEY + "/tenant/{tenantName}/patient" })
public class PatientShLinkRestController {

	public static final String SHLINK_QR_CODE_PATH_SUFFIX = "/qr";

	@Autowired
	private ShLinkUtilService shLinkUtilService;
	@Autowired
	private IisFhirClientFactory iisFhirClientFactory;
	@Autowired
	private CompressionService compressionService;

	@GetMapping(value = { SHLINK_QR_CODE_PATH_SUFFIX }, produces = MediaType.IMAGE_PNG_VALUE)
	public ResponseEntity<byte[]> doGetShLinkQrCode(HttpServletRequest req, HttpServletResponse resp,
			@RequestAttribute(CurrentTenantUtil.SESSION_REQUEST_TENANT) Tenant tenant,
			@PathVariable("patientId") String patientId)
			throws IOException, ServletException {
		IGenericClient client = iisFhirClientFactory.newGenericClient(tenant, req);
		IBaseResource patientSelected = client.read().resource("Patient").withId(patientId).execute();
		if (patientSelected == null) {
			throw new RuntimeException("Patient not found");
		}
		String qrCode = getQrCode(req, patientSelected, tenant);
		ByteArrayOutputStream byteArrayOutputStreamPNG = compressionService.toQrCodeStreamPNG(qrCode);
		return ResponseEntity.ok(byteArrayOutputStreamPNG.toByteArray());

	}

	public String getQrCode(HttpServletRequest req, IBaseResource patientSelected, Tenant tenant) {
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

	public static @NotNull String getManifestUrl(HttpServletRequest req, IBaseResource patientSelected, Tenant tenant) {
		// Adjusting Base URL for REST
		String baseUrl = StringUtils.substringBefore(req.getRequestURL().toString(), "/rest/");
		return getManifestUrl(baseUrl, patientSelected, tenant);
	}

	public static @NotNull String getManifestUrl(String baseUrl, IBaseResource patientSelected, Tenant tenant) {
		return baseUrl + RestUrlUtil.tenantifyPathWithContextPath(tenant,
				PatientShLinkManifestRestController.MANIFEST_PATH_SUFFIX
						+ "/patient/" + patientSelected.getIdElement().getIdPart());
	}

}
