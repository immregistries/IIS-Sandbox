package org.immregistries.iis.kernal.controllers.servlet.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.nimbusds.jose.util.Base64URL;
import jakarta.servlet.ServletException;
import org.immregistries.iis.kernal.fhir.shl.ShLinkPayload;

import java.io.IOException;
import java.io.OutputStream;


public class UiQrCodeUtil {

	public static final String SHLINK_PREFIX = "shlink:/";

	private static final QRCodeWriter qrCodeWriter = new QRCodeWriter();
	private static final ObjectMapper jsonMapper = new ObjectMapper();

	public static void printQrCodeAsImage(OutputStream outputStream, String data) throws ServletException, IOException, WriterException {
		int width = 300; // Desired QR code width
		int height = 300; // Desired QR code height
		BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height);
		MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
	}

	public static String qrCodeBase64(ShLinkPayload shLinkPayload) throws JsonProcessingException {
		String payload = jsonMapper.writeValueAsString(shLinkPayload);
		Base64URL base64URL = Base64URL.encode(payload);
		return SHLINK_PREFIX + base64URL;
	}
}
