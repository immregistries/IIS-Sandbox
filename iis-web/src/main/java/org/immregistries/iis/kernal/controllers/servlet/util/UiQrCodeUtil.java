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
import org.immregistries.iis.kernal.GlobalConstants;
import org.immregistries.iis.kernal.controllers.rest.RestUrlUtil;
import org.immregistries.iis.kernal.model.shlink.ShLinkPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

@Service
public class UiQrCodeUtil {

	@Autowired
	private RestUrlUtil restUrlUtil;

	private final QRCodeWriter qrCodeWriter = new QRCodeWriter();
	private final ObjectMapper jsonMapper = new ObjectMapper();

	public void printQrCodeAsImage(OutputStream outputStream, String data)
			throws ServletException, IOException, WriterException {
		int width = 300; // Desired QR code width
		int height = 300; // Desired QR code height
		BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height);
		MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
	}

	public String qrCodeBase64(ShLinkPayload shLinkPayload) throws JsonProcessingException {
		String payload = jsonMapper.writeValueAsString(shLinkPayload);
		Base64URL base64URL = Base64URL.encode(payload);
		return GlobalConstants.SHLINK_PREFIX + base64URL;
	}

	public void prettyPrintQrCodeCard(PrintWriter out, String label, String description, String imageUrl, String qrCode, String manifestUrl) {
		out.println("<div class=\"w3-col l4 m4 s12\">");
		out.println("<div class=\"w3-card-4 w3-sand w3-center w3-hover-shadow\">");
		out.println("<header class=\"w3-panel \">");
		out.println("<h3>" + label + "</h3>");
		out.println("</header>");

		out.println("<div class=\"w3-container\">");

		out.println("<img src=\""
			+ imageUrl
			+ "\"  alt=\"sh-link\">");

		out.println("<textarea id =\"qrCode\" cols=\"30\" rows=\"1\" style=\"white-space: nowrap; overflow: auto;\">");
		out.print(qrCode);
		out.println("</textarea>");
		out.println("</div>");

		out.println("<footer class=\"w3-container w3-center\">" + "<p>");
		out.println(description);
		if (manifestUrl != null && !manifestUrl.isEmpty()) {
			out.println(" (<a href= \"" + manifestUrl + "\">manifest</a>)");
		}
		out.println("</p>" + "</footer>");
		out.println("</div>");
		out.println("</div>");
	}
}
