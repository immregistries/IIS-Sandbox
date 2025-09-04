package org.immregistries.iis.kernal.servlet.shlink;

import ca.uhn.fhir.jpa.partition.IPartitionLookupSvc;
import com.nimbusds.jose.util.Base64URL;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.immregistries.iis.kernal.fhir.ips.IpsGeneratorSvcIIS;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.logic.KeyStoreService;
import org.immregistries.iis.kernal.logic.shlink.CompressionUtil;
import org.immregistries.iis.kernal.logic.shlink.ShLinkUtilService;
import org.immregistries.iis.kernal.logic.shlink.evc.EvCPayload;
import org.immregistries.iis.kernal.logic.shlink.evc.EvCUtil;
import org.immregistries.iis.kernal.logic.shlink.evc.EvcService;
import org.immregistries.iis.kernal.model.persisted.IisKey;
import org.immregistries.iis.kernal.model.persisted.Tenant;
import org.immregistries.iis.kernal.model.persisted.UserAccess;
import org.immregistries.iis.kernal.servlet.TenantController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.zip.DataFormatException;

@RestController
@RequestMapping(TenantController.TENANT_PATH + EvcController.EVC_PATH_SUFFIX)
public class EvcController {
	public static final String EVC_PATH_SUFFIX = "/evc";

	Logger logger = LoggerFactory.getLogger(this.getClass());


	@Autowired
	KeyStoreService keyStoreService;

	@Autowired
	ShLinkUtilService shLinkUtilService;

	@Autowired
	EvcService evcService;

	@Autowired
	IpsGeneratorSvcIIS ipsGeneratorSvcIIS;

	@Autowired
	IPartitionLookupSvc partitionLookupSvc;


	@GetMapping("/{patientId}")
	protected void doGetPatientEvc(
		HttpServletRequest req,
		HttpServletResponse resp,
		@PathVariable("patientId") String patientId
	) throws IOException, ServletException, DataFormatException, SignatureException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
//		ObjectMapper objectMapper = new ObjectMapper();
		Tenant tenant = ServletHelper.getTenantRedirectIfNone(req, resp);
		UserAccess userAccess = ServletHelper.getUserAccess();
		OutputStream outputStream = resp.getOutputStream();
//		Gson gson = new Gson();
		IisKey iisSigningKey = keyStoreService.getIisSigningKeyOrCreate("", userAccess, tenant);

		IBaseBundle ipsToBeEncoded = ipsGeneratorSvcIIS.generateIps(ServletHelper.requestDetailsWithPartitionName(partitionLookupSvc), new IdType(patientId), "");
		EvCPayload evCPayload = EvCUtil.toEvCPayloadFromBundle((Bundle) ipsToBeEncoded);

		byte[] bytes = CompressionUtil.minifyJson(evCPayload).getBytes();
		byte[] cborPayload = evcService.cbor(bytes);

		byte[] cosePayload = evcService.createCoseSign1(iisSigningKey, cborPayload);
		logger.info("cosePayload {}", cosePayload);
		String qrCode = Base64URL.encode(cosePayload).toString();
//		String qrCode = new String(cosePayload);
//		logger.info("qrCode {}", qrCode);

		resp.setContentType("image/png"); // Set content type for PNG image
		shLinkUtilService.printQrCodeAsImage(outputStream, qrCode);
	}

}
