package org.immregistries.iis.kernal.servlet.shlink;

import ca.uhn.fhir.jpa.partition.IPartitionLookupSvc;
import com.authlete.cose.COSEException;
import com.google.zxing.WriterException;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.hl7.fhir.instance.model.api.IBaseBundle;
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
import org.immregistries.iis.kernal.rest.shlink.CLVRRestController;
import org.immregistries.iis.kernal.servlet.TenantController;
import org.immregitries.clvr.CLVRPdfService;
import org.immregitries.clvr.CLVRService;
import org.immregitries.clvr.mapping.FhirConversionUtil;
import org.immregitries.clvr.model.CLVRPayload;
import org.immregitries.clvr.model.CLVRToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
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
	ShLinkUtilService shLinkUtilService;

	@Autowired
	CLVRRestController clvrRestController;

	@GetMapping("/{patientId}")
	protected void doGetPatientCLVR(
			HttpServletRequest req,
			HttpServletResponse resp,
			@PathVariable("patientId") String patientId,
			@RequestParam(value = "pdf", required = false) boolean pdf)
			throws COSEException, IOException, SignatureException, NoSuchAlgorithmException, InvalidKeyException,
			NoSuchProviderException, ServletException, WriterException, URISyntaxException {
		Tenant tenant = CurrentTenantUtil.getTenantRedirectIfNone(req, resp);
		UserAccess userAccess = UserAccessUtil.get().getUserAccess();
		ServletOutputStream outputStream = resp.getOutputStream();

//		String qrCode = clvrRestController.getPatientClvrQrCode(patientId, tenant);
//		logger.info("qrCode {}", qrCode);

		if (!pdf) {
			resp.setContentType("image/png"); // Set content type for PNG image
			ResponseEntity<byte[]> responseEntity = clvrRestController.getPatientClvrPng(patientId, tenant);
			outputStream.write(responseEntity.getBody());
			outputStream.flush();
			outputStream.close();

//			String qrCode = clvrRestController.getPatientClvrQrCode(patientId, tenant);
//			shLinkUtilService.printQrCodeAsImage(outputStream, qrCode);
		} else {

			ResponseEntity<byte[]> responseEntity = clvrRestController.getPatientClvrPdf(patientId, tenant);
			resp.setContentType("application/pdf");
			resp.setHeader("Content-Disposition", "attachment; filename=" + "testPdf");
			outputStream.write(responseEntity.getBody());
			outputStream.flush();
			outputStream.close();
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
