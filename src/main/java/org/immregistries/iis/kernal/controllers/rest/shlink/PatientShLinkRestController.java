package org.immregistries.iis.kernal.controllers.rest.shlink;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.controllers.rest.RestUrlUtil;
import org.immregistries.iis.kernal.logic.shlink.CompressionService;
import org.immregistries.iis.kernal.logic.shlink.PatientShLinkService;
import org.immregistries.iis.kernal.mapping.IisFhirClientFactory;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.security.CurrentTenantUtil;
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
	private IisFhirClientFactory iisFhirClientFactory;
	@Autowired
	private CompressionService compressionService;
	@Autowired
	private PatientShLinkService patientShlinkService;

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
		String qrCode = patientShlinkService.getPatientShLinkQrCode(req, patientSelected, tenant);
		ByteArrayOutputStream byteArrayOutputStreamPNG = compressionService.toQrCodeStreamPNG(qrCode);
		return ResponseEntity.ok(byteArrayOutputStreamPNG.toByteArray());

	}

}
