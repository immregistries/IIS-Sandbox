package org.immregistries.iis.kernal.servlet.shlink;

import ca.uhn.fhir.jpa.partition.IPartitionLookupSvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.nimbusds.jose.util.Base64URL;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.pdfbox.cos.COSBoolean;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
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
		@PathVariable("patientId") String patientId,
		@RequestParam(value = "pdf", required = false) boolean pdf
	) throws IOException, ServletException, DataFormatException, SignatureException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, WriterException {
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

		if (!pdf) {
			resp.setContentType("image/png"); // Set content type for PNG image
			shLinkUtilService.printQrCodeAsImage(outputStream, qrCode);
		} else {
			PDDocument pdDocument = createPdf(evCPayload, qrCode.getBytes());
			pringPdf(req, resp, pdDocument, "testEvc");
		}
	}

	protected void pringPdf(
		HttpServletRequest req,
		HttpServletResponse resp,
		PDDocument pdDocument,
		String name
	) throws IOException {
		resp.setContentType("application/pdf");
		resp.setHeader("Content-Disposition", "attachment; filename=" + name);
//		resp.setContentLength(pdDocument.getfileToDownload.available());
		pdDocument.save(resp.getOutputStream());
		pdDocument.close();
		resp.getOutputStream().flush();
		resp.getOutputStream().close();
	}

	private static PDDocument createPdf(EvCPayload evCPayload, byte[] qrCode) throws IOException, WriterException {
		ObjectMapper objectMapper = new ObjectMapper();
		PDDocument document = new PDDocument();
		PDPage page = new PDPage();
		document.addPage(page);

		PDDocumentInformation pdDocumentInformation = new PDDocumentInformation();
		document.setDocumentInformation(pdDocumentInformation);
		pdDocumentInformation.setCreator("IIS SANDBOX");
		pdDocumentInformation.setCustomMetadataValue("evc", objectMapper.writeValueAsString(evCPayload));

		PDPageContentStream contentStream = new PDPageContentStream(document, page);

		BufferedImage bufferedImage;
		QRCodeWriter qrCodeWriter = new QRCodeWriter();
		int width = 200; // Desired QR code width
		int height = 200; // Desired QR code height
		BitMatrix bitMatrix = qrCodeWriter.encode(new String(qrCode), BarcodeFormat.QR_CODE, width, height);
		bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
		{
			// Create a dictionary for the inline image parameters
			COSDictionary parameters = new COSDictionary();
			parameters.setItem(COSName.IM, COSBoolean.TRUE); // Indicate it's an inline image
			parameters.setInt(COSName.W, width); // Width of the image
			parameters.setInt(COSName.H, height); // Height of the image
			parameters.setInt(COSName.BPC, 1); // Bits per component (for a 1-bit image)

			PDImageXObject imageXObject;
			imageXObject = LosslessFactory.createFromImage(document, bufferedImage);

//			PDInlineImage inlineImage = new PDInlineImage(parameters, qrCode, null);
//			inlineImage.setColorSpace(new PDJPXColorSpace(ColorSpace.getInstance(ColorSpace.CS_GRAY)));
			contentStream.drawImage(imageXObject, 0, 0);
		}


		contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.COURIER), 12);
		contentStream.beginText();
		contentStream.showText("IIS Sandbox Test EVC");
		contentStream.endText();
		contentStream.close();

		return document;
	}

}
