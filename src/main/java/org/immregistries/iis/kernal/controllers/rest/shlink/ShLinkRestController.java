package org.immregistries.iis.kernal.controllers.rest.shlink;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.IdType;
import org.immregistries.iis.kernal.fhir.ips.IpsGeneratorSvcIIS;
import org.immregistries.iis.kernal.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.security.TenantUtil;
import org.immregistries.iis.kernal.security.UserAccessUtil;
import org.immregistries.iis.kernal.fhir.shl.ShLinkPayload;
import org.immregistries.iis.kernal.logic.KeyStoreService;
import org.immregistries.iis.kernal.logic.shlink.CompressionService;
import org.immregistries.iis.kernal.logic.shlink.ShLinkUtilService;
import org.immregistries.iis.kernal.persisted.model.IisKey;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.persisted.model.UserAccess;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.immregistries.iis.kernal.controllers.rest.RestUrlUtil;

import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping({ RestUrlUtil.REST + "/shlink", RestUrlUtil.REST + "/tenant/{tenantName}/shlink" })
public class ShLinkRestController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	// Keeping constants that might be useful
	public static final String PARAM_SECRET_KEY = "secretKey";
	public static final String PARAM_KEY_ID = "keyId";
	public static final String PARAM_PATIENT_ID = "patientId";
	public static final String PARAM_FLAG = "flag";
	private static final String PARAM_EXP = "exp";


	@Autowired
	ShLinkUtilService shLinkUtilService;


	@Autowired
	CompressionService compressionService;

	@PostMapping()
	public String shLinkIPSQrCode(HttpServletRequest req,
			@RequestParam(value = PARAM_KEY_ID, required = false) String keyId,
			@RequestParam(value = PARAM_SECRET_KEY, required = false) String secretKey,
			@RequestParam(PARAM_PATIENT_ID) String patientId,
			@RequestParam(PARAM_FLAG) String flag,
			@RequestParam(value = PARAM_EXP, required = false, defaultValue = "10000000") String exp,
			@RequestAttribute(CurrentTenantUtil.SESSION_REQUEST_TENANT) Tenant tenant)
			throws IOException, NoSuchAlgorithmException {
		UserAccess userAccess = UserAccessUtil.get().getUserAccess();
		String qrCode = shLinkUtilService.generateShLink(req, keyId, secretKey, patientId, flag, exp, tenant, userAccess);
		return qrCode;
	}


	@PostMapping(value = "/png")
	public ResponseEntity<byte[]> shLinkIPSPng(HttpServletRequest req,
			@RequestParam(value = PARAM_KEY_ID, required = false) String keyId,
			@RequestParam(value = PARAM_SECRET_KEY, required = false) String secretKey,
			@RequestParam(PARAM_PATIENT_ID) String patientId,
			@RequestParam(PARAM_FLAG) String flag,
			@RequestParam(value = PARAM_EXP, required = false, defaultValue = "10000000") String exp,
			@RequestAttribute(CurrentTenantUtil.SESSION_REQUEST_TENANT) Tenant tenant)
			throws ServletException, IOException, NoSuchAlgorithmException {
		String qrCode = shLinkIPSQrCode(req, keyId, secretKey, patientId, flag, exp, tenant);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.IMAGE_PNG);
		ByteArrayOutputStream outputStream = compressionService.toQrCodeStreamPNG(qrCode);
		return new ResponseEntity<>(outputStream.toByteArray(), headers, HttpStatus.OK);

	}



	// Skipping doGet as it was purely HTML UI form.
}
