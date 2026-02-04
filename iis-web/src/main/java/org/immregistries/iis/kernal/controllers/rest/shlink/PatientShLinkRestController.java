package org.immregistries.iis.kernal.controllers.rest.shlink;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.GlobalConstants;
import org.immregistries.iis.kernal.controllers.IisPathVariable;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.logic.shlink.generation.PatientShLinkGenerator;
import org.immregistries.iis.kernal.mapping.IisFhirClientFactory;
import org.immregistries.iis.kernal.mapping.mappers.resources.PatientMapper;
import org.immregistries.iis.kernal.model.shlink.ShLinkPayload;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.services.QrCodeEncoder;
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
	private QrCodeEncoder qrCodeEncoder;
	@Autowired
	private PatientShLinkGenerator patientShLinkGenerator;
	@Autowired
	private org.immregistries.iis.kernal.logic.shlink.IPatientManifestApiUrlService patientShlinkApiManifestUrlService;

	@GetMapping(produces = MediaType.IMAGE_PNG_VALUE)
	public ResponseEntity<byte[]> doGetShLinkQrCode(HttpServletRequest req, HttpServletResponse resp,
																	@RequestAttribute(GlobalConstants.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
			@PathVariable(IisPathVariable.Key.PATIENT_ID) String patientId)
			throws IOException, ServletException {
		IGenericClient client = iisFhirClientFactory.newGenericClient(tenant, req);
		IAnyResource patientSelected = (IAnyResource) client.read().resource(PatientMapper.PATIENT_FHIR_TYPE_NAME).withId(patientId).execute();
		if (patientSelected == null) {
			throw new RuntimeException("Patient not found");
		}
		String manifestUrl = patientShlinkApiManifestUrlService.getManifestUrl(req, patientSelected, tenant);
		ShLinkPayload shLinkPayload = patientShLinkGenerator.generatePatientShLinkPayload(manifestUrl);
		String qrCode = qrCodeEncoder.toBase64QrCode(shLinkPayload);
		ByteArrayOutputStream byteArrayOutputStreamPNG = qrCodeEncoder.toQrCodeStreamPNG(qrCode);
		return ResponseEntity.ok(byteArrayOutputStreamPNG.toByteArray());

	}

}
