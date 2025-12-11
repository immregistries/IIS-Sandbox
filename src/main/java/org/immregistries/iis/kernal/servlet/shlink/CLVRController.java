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
import org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.fhir.security.TenantUtil;
import org.immregistries.iis.kernal.fhir.security.UserAccessUtil;
import org.immregistries.iis.kernal.logic.KeyStoreService;
import org.immregistries.iis.kernal.logic.shlink.ShLinkUtilService;
import org.immregistries.iis.kernal.persisted.model.IisKey;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.persisted.model.UserAccess;
import org.immregistries.iis.kernal.servlet.TenantController;
import org.immregitries.clvr.CLVRPdfService;
import org.immregitries.clvr.CLVRService;
import org.immregitries.clvr.mapping.FhirConversionUtil;
import org.immregitries.clvr.model.CLVRPayload;
import org.immregitries.clvr.model.CLVRToken;
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

	@Autowired
	CLVRPdfService clvrPdfService;

	@GetMapping("/{patientId}")
	protected void doGetPatientCLVR(
			HttpServletRequest req,
			HttpServletResponse resp,
			@PathVariable("patientId") String patientId,
			@RequestParam(value = "pdf", required = false) boolean pdf)
			throws COSEException, IOException, SignatureException, NoSuchAlgorithmException, InvalidKeyException,
			NoSuchProviderException, ServletException, WriterException {
		Tenant tenant = CurrentTenantUtil.getTenantRedirectIfNone(req, resp);
		UserAccess userAccess = UserAccessUtil.getUserAccess();
		OutputStream outputStream = resp.getOutputStream();

		IisKey iisSigningKey = keyStoreService.getIisSigningKeyOrCreate("", userAccess, tenant);

		IBaseBundle ipsToBeEncoded = ipsGeneratorSvcIIS
				.generateIps(TenantUtil.requestDetailsWithPartitionName(), new IdType(patientId), "");
		CLVRPayload clvrPayload = fhirConversionUtil.toCLVRPayloadFromBundle(ipsToBeEncoded);

		CLVRToken clvrToken = new CLVRToken(clvrPayload, "IIS");
		String qrCode = clvrService.encodeCLVRtoQrCode(clvrToken, iisSigningKey.keyPair());
		logger.info("qrCode {}", qrCode);

		if (!pdf) {
			resp.setContentType("image/png"); // Set content type for PNG image
			shLinkUtilService.printQrCodeAsImage(outputStream, qrCode);
		} else {
			PDDocument pdDocument = clvrPdfService.createPdf(clvrToken, qrCode.getBytes(), "IIS SANDBOX");
			printPdf(req, resp, pdDocument, "testCLVR");
		}
	}

	protected void printPdf(
			HttpServletRequest req,
			HttpServletResponse resp,
			PDDocument pdDocument,
			String name) throws IOException {
		resp.setContentType("application/pdf");
		resp.setHeader("Content-Disposition", "attachment; filename=" + name);
		pdDocument.save(resp.getOutputStream());
		pdDocument.close();
		resp.getOutputStream().flush();
		resp.getOutputStream().close();
	}

}
