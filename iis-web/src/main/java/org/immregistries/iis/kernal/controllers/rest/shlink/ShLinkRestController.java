package org.immregistries.iis.kernal.controllers.rest.shlink;

import jakarta.servlet.http.HttpServletRequest;
import org.immregistries.iis.kernal.IisRequestAttribute;
import org.immregistries.iis.kernal.controllers.IisRestParam;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.logic.shlink.generation.ShLinkGenerator;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.immregistries.iis.kernal.services.QrCodeEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping({IisRestPath.BasePath.REST_PATH + IisRestPath.BasePath.SH_LINK_PATH, IisRestPath.REST_TENANT_PATH + IisRestPath.BasePath.SH_LINK_PATH})
public class ShLinkRestController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private ShLinkGenerator shLinkGenerator;
	@Autowired
	private QrCodeEncoder qrCodeEncoder;

	@PostMapping()
	public String createShLinkIPSQrCode(
		@AuthenticationPrincipal UserAccess userAccess,
		HttpServletRequest req,
			@RequestParam(value = IisRestParam.KEY_ID, required = false) String keyId,
			@RequestParam(value = IisRestParam.ShLink.SECRET_KEY, required = false) String secretKey,
			@RequestParam(IisRestParam.PATIENT_ID) String patientId,
			@RequestParam(IisRestParam.ShLink.FLAG) String flag,
		@RequestParam(IisRestParam.ShLink.PASSCODE) String passcode,
			@RequestParam(value = IisRestParam.ShLink.EXP, required = false, defaultValue = "10000000") String exp,
											@RequestAttribute(IisRequestAttribute.TENANT_REQUEST_ATTRIBUTE) Tenant tenant)
			throws IOException, NoSuchAlgorithmException {
		ServletUriComponentsBuilder uriBuilder = ServletUriComponentsBuilder.fromRequest(req);
		String qrCode = shLinkGenerator.generateShLink(keyId, secretKey, patientId, flag, exp, tenant, userAccess, uriBuilder, passcode);
		return qrCode;
	}
}
