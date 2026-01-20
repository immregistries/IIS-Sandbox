package org.immregistries.iis.kernal.logic.shlink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.util.Base64URL;
import org.immregistries.iis.kernal.fhir.shl.ShLinkPayload;

public class ShLinkPayloadUtil {

	public static final String SHLINK_PREFIX = "shlink:/";
	private static final ObjectMapper objectMapper = new ObjectMapper();

	public static String toQrCode(ShLinkPayload shLinkPayload) {
		String payload = "";
		try {
			payload = objectMapper.writeValueAsString(shLinkPayload);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		Base64URL base64URL = Base64URL.encode(payload);
		return SHLINK_PREFIX + base64URL;
	}


}
