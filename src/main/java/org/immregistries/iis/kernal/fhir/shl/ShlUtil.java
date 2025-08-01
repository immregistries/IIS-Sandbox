package org.immregistries.iis.kernal.fhir.shl;

import com.google.gson.Gson;
import com.nimbusds.jose.util.Base64URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShlUtil {
	private static final Logger logger = LoggerFactory.getLogger(ShlUtil.class);

	public static final String EMBEDDED = "embedded";
	public static final String CONTENT_TYPE = "contentType";
	public static final String FILES = "files";
	public static final String U = "U";
	public static final String URL = "url";
	public static final String KEY = "key";
	public static final String FLAG = "flag";
	public static final String EXP = "exp";
	public static final String LABEL = "label";
	public static final String V = "v";
	public static final String SHLINK_PREFIX = "shlink:/";
	public static final String LOCATION = "location";
	public static final String VERIFIABLE_CREDENTIAL = "verifiableCredential";
	public static final String APPLICATION_JOSE = "application/jose";

	public String qrCode(SmartHealthLinkPayload smartHealthLinkPayload) {
		Gson gson = new Gson();
		String payload = gson.toJson(smartHealthLinkPayload);
		String minified = payload.trim();
		Base64URL base64URL = Base64URL.encode(minified);
		String shLink = SHLINK_PREFIX + base64URL;

		return shLink;
	}
}
