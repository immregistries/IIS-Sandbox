package org.immregistries.iis.kernal.fhir.shl;

import com.google.gson.Gson;
import com.nimbusds.jose.util.Base64URL;
import org.immregistries.iis.kernal.model.persisted.ShLinkManifest;
import org.immregistries.iis.kernal.model.persisted.Tenant;
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
//		String minified = payload.trim();
		Base64URL base64URL = Base64URL.encode(payload);
		return SHLINK_PREFIX + base64URL;
	}

	public static ShLinkManifest generateManifest(Tenant tenant) {
		ShLinkManifest shLinkManifest = new ShLinkManifest();
		shLinkManifest.setTenant(tenant);
		shLinkManifest.setStatus("finalized");
		ShLinkManifest.FileManifest fileManifest = new ShLinkManifest.FileManifest();
		String fhirVersion = "4.0.1"; // TODO change
		fileManifest.setContentType("application/fhir+json;fhirVersion=" + fhirVersion);
		fileManifest.setLocation("/fhir" + "/" + tenant.getOrganizationName() + "/Patient?identifier=test");
		shLinkManifest.addFiles(fileManifest);
		return shLinkManifest;
	}


}
