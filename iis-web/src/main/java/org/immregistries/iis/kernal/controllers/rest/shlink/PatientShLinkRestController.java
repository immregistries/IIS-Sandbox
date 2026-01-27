package org.immregistries.iis.kernal.controllers.rest.shlink;

import org.immregistries.iis.kernal.PatientShlinkApiService;
import org.immregistries.iis.kernal.controllers.IisPathVariable;
import org.immregistries.iis.kernal.controllers.IisRestPath;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.logic.shlink.CompressionService;
import org.immregistries.iis.kernal.logic.shlink.IShApiUrlService;
import org.immregistries.iis.kernal.logic.shlink.PatientShLinkGenerator;
import org.immregistries.iis.kernal.logic.shlink.ShApiUrlServiceImpl;
import org.immregistries.iis.kernal.mapping.IisFhirClientFactory;
import org.immregistries.iis.kernal.mapping.mappers.resources.PatientMapper;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.security.CurrentTenantUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RestController
@RequestMapping({ IisRestPath.REST_PATIENT_PATH + IisRestPath.BasePath.PATIENT_SH_LINK_PATH})
public class PatientShLinkRestController {

	@Autowired
	private IisFhirClientFactory iisFhirClientFactory;
	@Autowired
	private CompressionService compressionService;
	@Autowired
	private PatientShLinkGenerator patientShlinkGenerator;
	@Autowired
	private PatientShlinkApiService patientShlinkApiService;

	@GetMapping(produces = MediaType.IMAGE_PNG_VALUE)
	public ResponseEntity<byte[]> doGetShLinkQrCode(HttpServletRequest req, HttpServletResponse resp,
			@RequestAttribute(CurrentTenantUtil.SESSION_REQUEST_TENANT) Tenant tenant,
			@PathVariable(IisPathVariable.Key.PATIENT_ID) String patientId)
			throws IOException, ServletException {
		IGenericClient client = iisFhirClientFactory.newGenericClient(tenant, req);
		IAnyResource patientSelected = (IAnyResource) client.read().resource(PatientMapper.PATIENT_FHIR_TYPE_NAME).withId(patientId).execute();
		if (patientSelected == null) {
			throw new RuntimeException("Patient not found");
		}
		String qrCode = patientShlinkApiService.getPatientShLinkQrCode(req, patientSelected, tenant);
		ByteArrayOutputStream byteArrayOutputStreamPNG = compressionService.toQrCodeStreamPNG(qrCode);
		return ResponseEntity.ok(byteArrayOutputStreamPNG.toByteArray());

	}

}
