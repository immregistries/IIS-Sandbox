package org.immregistries.iis.kernal.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.nimbusds.jose.util.Base64URL;
import jakarta.servlet.ServletException;
import org.immregistries.iis.kernal.GlobalConstants;
import org.immregistries.iis.kernal.model.shlink.ShLinkPayload;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class QrCodeEncoder {

	private final QRCodeWriter qrCodeWriter = new QRCodeWriter();
	private final ObjectMapper jsonMapper = new ObjectMapper();


	public BitMatrix qrCodeBitMatrix(String data, int width, int height) throws ServletException {
		try {
			return qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height);
		} catch (WriterException e) {
			throw new ServletException("Error generating QR code Bit Matrix", e);
		}
	}


	public @NotNull ByteArrayOutputStream toQrCodeStreamPNG(String data) throws IOException, ServletException {
		int width = 300; // Desired QR code width
		int height = 300; // Desired QR code height
		BitMatrix bitMatrix = qrCodeBitMatrix(data, width, height);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		MatrixToImageWriter.writeToStream(bitMatrix, "PNG", byteArrayOutputStream);
		return byteArrayOutputStream;
	}

	public String toBase64QrCode(ShLinkPayload shLinkPayload) {
		String payload = "";
		try {
			payload = jsonMapper.writeValueAsString(shLinkPayload);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		Base64URL base64URL = Base64URL.encode(payload);
		return GlobalConstants.SHLINK_PREFIX + base64URL;
	}
}
