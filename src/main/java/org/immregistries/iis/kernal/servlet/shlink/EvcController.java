package org.immregistries.iis.kernal.servlet.shlink;

import ca.uhn.fhir.jpa.partition.IPartitionLookupSvc;
import com.authlete.cose.COSEException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.minvws.encoding.Base45;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.zip.DataFormatException;

import static org.immregistries.iis.kernal.logic.shlink.evc.EvcService.VC_1;

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

	@Autowired
	EvCUtil evCUtil;


	@GetMapping("/{patientId}")
	protected void doGetPatientEvc(
		HttpServletRequest req,
		HttpServletResponse resp,
		@PathVariable("patientId") String patientId,
		@RequestParam(value = "pdf", required = false) boolean pdf
	) throws IOException, ServletException, DataFormatException, SignatureException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, WriterException, COSEException {
//		ObjectMapper objectMapper = new ObjectMapper();
		Tenant tenant = ServletHelper.getTenantRedirectIfNone(req, resp);
		UserAccess userAccess = ServletHelper.getUserAccess();
		OutputStream outputStream = resp.getOutputStream();

		IisKey iisSigningKey = keyStoreService.getIisSigningKeyOrCreate("", userAccess, tenant);

		IBaseBundle ipsToBeEncoded = ipsGeneratorSvcIIS.generateIps(ServletHelper.requestDetailsWithPartitionName(partitionLookupSvc), new IdType(patientId), "");
		EvCPayload evCPayload = evCUtil.toEvCPayloadFromBundle((Bundle) ipsToBeEncoded);

		String qrCode = evcService.encodeQrCode(evCPayload, iisSigningKey);
//		logger.info("qrCode {}", qrCode);

		if (!pdf) {
			resp.setContentType("image/png"); // Set content type for PNG image
			shLinkUtilService.printQrCodeAsImage(outputStream, qrCode);
		} else {
			PDDocument pdDocument = createPdf(evCPayload, qrCode.getBytes());
			printPdf(req, resp, pdDocument, "testEvc");
		}
		evcService.decodeFullQrCode(qrCode.getBytes(), iisSigningKey);
	}


	protected void printPdf(
		HttpServletRequest req,
		HttpServletResponse resp,
		PDDocument pdDocument,
		String name
	) throws IOException {
		resp.setContentType("application/pdf");
		resp.setHeader("Content-Disposition", "attachment; filename=" + name);
//		resp.setContentLength(pdDocument.getfileToDownload.available());
		pdDocument.save(resp.getOutputStream());
//		pdDocument.save("CACA.pdf");
		pdDocument.close();
		resp.getOutputStream().flush();
		resp.getOutputStream().close();
	}

	private static PDDocument createPdf(EvCPayload evCPayload, byte[] qrCode) throws IOException, WriterException, ServletException {
		ObjectMapper objectMapper = new ObjectMapper();
		PDDocument document = new PDDocument();
		PDPage page = new PDPage();
		document.addPage(page);

		PDDocumentInformation pdDocumentInformation = new PDDocumentInformation();
		document.setDocumentInformation(pdDocumentInformation);
		pdDocumentInformation.setCreator("IIS SANDBOX");
		pdDocumentInformation.setCustomMetadataValue("evc", objectMapper.writeValueAsString(evCPayload));
		PDPageContentStream contentStream = new PDPageContentStream(document, page);

		PDImageXObject qrCodeImageObject;
		{
			int width = 300; // Desired QR code width
			int height = 300; // Desired QR code height
			BitMatrix bitMatrix = CompressionUtil.qrCodeBitMatrix(new String(qrCode), width, height);
			BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
			qrCodeImageObject = LosslessFactory.createFromImage(document, bufferedImage);
		}
		contentStream.drawImage(qrCodeImageObject, 150, 150);


		// Creating Paragraph object
		contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.COURIER), 12);
		contentStream.beginText();
		contentStream.setLeading(14.5f);
		contentStream.newLineAtOffset(25, 700);
		contentStream.showText("IIS Sandbox Test IPS to EVC");
		contentStream.newLine();
		contentStream.showText("Patient Information for " +
			evCPayload.getName().getFamilyName() +
			", " +
			evCPayload.getName().getGivenName());
		contentStream.newLine();
		contentStream.showText("Identifier: " + evCPayload.getPersonIdentifier().getObjectIdentifier() + " " + evCPayload.getPersonIdentifier().getId());
		contentStream.newLine();
		contentStream.showText(new String(qrCode));
//		contentStream.showText(evCPayload.getName().getGivenName());
		contentStream.endText();
		contentStream.close();

		return document;
	}

}
