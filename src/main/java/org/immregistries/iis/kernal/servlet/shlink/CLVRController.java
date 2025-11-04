package org.immregistries.iis.kernal.servlet.shlink;

import ca.uhn.fhir.jpa.partition.IPartitionLookupSvc;
import com.authlete.cose.COSEException;
import com.google.zxing.WriterException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.immregistries.iis.kernal.fhir.ips.IpsGeneratorSvcIIS;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.logic.KeyStoreService;
import org.immregistries.iis.kernal.logic.shlink.ShLinkUtilService;
import org.immregistries.iis.kernal.model.persisted.IisKey;
import org.immregistries.iis.kernal.model.persisted.Tenant;
import org.immregistries.iis.kernal.model.persisted.UserAccess;
import org.immregistries.iis.kernal.servlet.TenantController;
import org.immregitries.clvr.CLVRPdfUtil;
import org.immregitries.clvr.CLVRService;
import org.immregitries.clvr.FhirConversionUtil;
import org.immregitries.clvr.model.CLVRPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.zip.DataFormatException;

@RestController
@RequestMapping(TenantController.TENANT_PATH + CLVRController.CLVR_PATH_SUFFIX)
public class CLVRController {
	public static final String CLVR_PATH_SUFFIX = "/clvr";
	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	KeyStoreService keyStoreService;

	@Autowired
	ShLinkUtilService shLinkUtilService;

	@Autowired
	CLVRService clvrService;

	@Autowired
	IpsGeneratorSvcIIS ipsGeneratorSvcIIS;

	@Autowired
	IPartitionLookupSvc partitionLookupSvc;

	@Autowired
	FhirConversionUtil fhirConversionUtil;


	@GetMapping("/{patientId}")
	protected void doGetPatientCLVR(
		HttpServletRequest req,
		HttpServletResponse resp,
		@PathVariable("patientId") String patientId,
		@RequestParam(value = "pdf", required = false) boolean pdf
	) throws IOException, ServletException, DataFormatException, SignatureException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, WriterException, COSEException {
		Tenant tenant = ServletHelper.getTenantRedirectIfNone(req, resp);
		UserAccess userAccess = ServletHelper.getUserAccess();
		OutputStream outputStream = resp.getOutputStream();

		IisKey iisSigningKey = keyStoreService.getIisSigningKeyOrCreate("", userAccess, tenant);

		IBaseBundle ipsToBeEncoded = ipsGeneratorSvcIIS.generateIps(ServletHelper.requestDetailsWithPartitionName(partitionLookupSvc), new IdType(patientId), "");
		CLVRPayload clvrPayload = fhirConversionUtil.toCLVRPayloadFromBundle((Bundle) ipsToBeEncoded);

		String qrCode = clvrService.encodeCLVRtoQrCode(clvrPayload, iisSigningKey.keyPair());
		logger.info("qrCode {}", qrCode);

		if (!pdf) {
			resp.setContentType("image/png"); // Set content type for PNG image
			shLinkUtilService.printQrCodeAsImage(outputStream, qrCode);
		} else {
			PDDocument pdDocument = CLVRPdfUtil.createPdf(clvrPayload, qrCode.getBytes(), "IIS SANDBOX");
			printPdf(req, resp, pdDocument, "testCLVR");
		}
	}


	protected void printPdf(
		HttpServletRequest req,
		HttpServletResponse resp,
		PDDocument pdDocument,
		String name
	) throws IOException {
		resp.setContentType("application/pdf");
		resp.setHeader("Content-Disposition", "attachment; filename=" + name);
		pdDocument.save(resp.getOutputStream());
		pdDocument.close();
		resp.getOutputStream().flush();
		resp.getOutputStream().close();
	}

}
