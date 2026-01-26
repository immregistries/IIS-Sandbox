package org.immregistries.iis.kernal.controllers.rest.shlink;

import org.immregistries.iis.kernal.controllers.rest.util.RestConstants;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.immregistries.iis.kernal.logic.shlink.CompressionService;
import org.immregistries.iis.kernal.logic.shlink.ShLinkGenerator;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.immregistries.iis.kernal.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.security.UserAccessUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping({ RestConstants.Path.REST + "/shlink", RestConstants.Path.REST + "/tenant/{tenantName}/shlink" })
public class ShLinkRestController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	// Keeping constants that might be useful
	private static final String PARAM_EXP = "exp";

	@Autowired
	ShLinkGenerator shLinkGenerator;

	@Autowired
	CompressionService compressionService;

	@PostMapping()
	public String shLinkIPSQrCode(HttpServletRequest req,
			@RequestParam(value = RestConstants.Param.KEY_ID, required = false) String keyId,
			@RequestParam(value = RestConstants.Param.SECRET_KEY, required = false) String secretKey,
			@RequestParam(RestConstants.Param.PATIENT_ID) String patientId,
			@RequestParam(RestConstants.Param.FLAG) String flag,
			@RequestParam(value = PARAM_EXP, required = false, defaultValue = "10000000") String exp,
			@RequestAttribute(CurrentTenantUtil.SESSION_REQUEST_TENANT) Tenant tenant)
			throws IOException, NoSuchAlgorithmException {
		UserAccess userAccess = UserAccessUtil.get().getUserAccess();
		String qrCode = shLinkGenerator.generateShLink(req, keyId, secretKey, patientId, flag, exp, tenant, userAccess);
		return qrCode;
	}

	@PostMapping(value = "/png")
	public ResponseEntity<byte[]> shLinkIPSPng(HttpServletRequest req,
			@RequestParam(value = RestConstants.Param.KEY_ID, required = false) String keyId,
			@RequestParam(value = RestConstants.Param.SECRET_KEY, required = false) String secretKey,
			@RequestParam(RestConstants.Param.PATIENT_ID) String patientId,
			@RequestParam(RestConstants.Param.FLAG) String flag,
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
