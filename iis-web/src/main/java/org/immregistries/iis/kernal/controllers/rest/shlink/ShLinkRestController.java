package org.immregistries.iis.kernal.controllers.rest.shlink;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.immregistries.iis.kernal.GlobalConstants;
import org.immregistries.iis.kernal.controllers.IisRestParam;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.logic.shlink.generation.ShLinkGenerator;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.immregistries.iis.kernal.security.UserAccessUtil;
import org.immregistries.iis.kernal.services.QrCodeEncoder;
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
@RequestMapping({ IisRestPath.BasePath.REST_PATH + "/shlink",  IisRestPath.BasePath.REST_PATH + "/tenant/{tenantName}/shlink" })
public class ShLinkRestController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());


	@Autowired
	private ShLinkGenerator shLinkGenerator;
	@Autowired
	private QrCodeEncoder qrCodeEncoder;

	@PostMapping()
	public String shLinkIPSQrCode(HttpServletRequest req,
			@RequestParam(value = IisRestParam.KEY_ID, required = false) String keyId,
			@RequestParam(value = IisRestParam.ShLink.SECRET_KEY, required = false) String secretKey,
			@RequestParam(IisRestParam.PATIENT_ID) String patientId,
			@RequestParam(IisRestParam.ShLink.FLAG) String flag,
			@RequestParam(value = IisRestParam.ShLink.EXP, required = false, defaultValue = "10000000") String exp,
											@RequestAttribute(GlobalConstants.SESSION_REQUEST_TENANT) Tenant tenant)
			throws IOException, NoSuchAlgorithmException {
		UserAccess userAccess = UserAccessUtil.get().getUserAccess();
		String qrCode = shLinkGenerator.generateShLink(req, keyId, secretKey, patientId, flag, exp, tenant, userAccess);
		return qrCode;
	}

	@PostMapping(value = "/png")
	public ResponseEntity<byte[]> shLinkIPSPng(HttpServletRequest req,
			@RequestParam(value = IisRestParam.KEY_ID, required = false) String keyId,
			@RequestParam(value = IisRestParam.ShLink.SECRET_KEY, required = false) String secretKey,
			@RequestParam(IisRestParam.PATIENT_ID) String patientId,
			@RequestParam(IisRestParam.ShLink.FLAG) String flag,
			@RequestParam(value = IisRestParam.ShLink.EXP, required = false, defaultValue = "10000000") String exp,
															 @RequestAttribute(GlobalConstants.SESSION_REQUEST_TENANT) Tenant tenant)
			throws ServletException, IOException, NoSuchAlgorithmException {
		String qrCode = shLinkIPSQrCode(req, keyId, secretKey, patientId, flag, exp, tenant);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.IMAGE_PNG);
		ByteArrayOutputStream outputStream = qrCodeEncoder.toQrCodeStreamPNG(qrCode);
		return new ResponseEntity<>(outputStream.toByteArray(), headers, HttpStatus.OK);

	}

	// Skipping doGet as it was purely HTML UI form.
}
