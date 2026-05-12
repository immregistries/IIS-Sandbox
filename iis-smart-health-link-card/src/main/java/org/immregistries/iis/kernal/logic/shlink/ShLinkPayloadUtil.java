package org.immregistries.iis.kernal.logic.shlink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.util.Base64URL;
import org.immregistries.iis.kernal.GlobalConstants;
import org.immregistries.iis.kernal.model.shlink.ShLinkPayload;
import org.springframework.stereotype.Service;

@Service
public class ShLinkPayloadUtil {

	private final ObjectMapper objectMapper = new ObjectMapper();

	public String toBase64QrCode(ShLinkPayload shLinkPayload) {
		String payload = "";
		try {
			payload = objectMapper.writeValueAsString(shLinkPayload);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		Base64URL base64URL = Base64URL.encode(payload);
		return GlobalConstants.SHLINK_PREFIX + base64URL;
	}


}
