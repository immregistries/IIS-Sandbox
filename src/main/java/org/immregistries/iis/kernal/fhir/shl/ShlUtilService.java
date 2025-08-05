package org.immregistries.iis.kernal.fhir.shl;

import ca.uhn.fhir.context.FhirContext;
import com.google.gson.Gson;
import com.nimbusds.jose.util.Base64URL;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.model.persisted.ShLinkManifest;
import org.immregistries.iis.kernal.model.persisted.Tenant;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ShlUtilService {
	private static final Logger logger = LoggerFactory.getLogger(ShlUtilService.class);

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

	@Autowired
	FhirContext fhirContext;

	public String qrCode(SmartHealthLinkPayload smartHealthLinkPayload) {
		Gson gson = new Gson();
		String payload = gson.toJson(smartHealthLinkPayload);
//		String minified = payload.trim();
		Base64URL base64URL = Base64URL.encode(payload);
		return SHLINK_PREFIX + base64URL;
	}

	public ShLinkManifest generateManifest(Tenant tenant) {
		ShLinkManifest shLinkManifest = new ShLinkManifest();
		shLinkManifest.setTenant(tenant);
		shLinkManifest.setStatus("finalized");
		ShLinkManifest.FileManifest fileManifest = generateFhirFileManifest();
		shLinkManifest.addFiles(fileManifest);
		fileManifest.setLocation("/fhir" + "/" + tenant.getOrganizationName() + "/Patient?identifier=test");
		return shLinkManifest;
	}

	private ShLinkManifest.@NotNull FileManifest generateFhirFileManifest() {
		ShLinkManifest.FileManifest fileManifest = new ShLinkManifest.FileManifest();
		String fhirVersion = fhirContext.getVersion().getVersion().getFhirVersionString();
		fileManifest.setContentType("application/fhir+json;fhirVersion=" + fhirVersion);
		return fileManifest;
	}

	public ShLinkManifest saveManifest(ShLinkManifest shLinkManifest) {
		if (StringUtils.isBlank(shLinkManifest.getId())) {
			shLinkManifest.setId(UUID.randomUUID().toString());
		}
		try (Session dataSession = ServletHelper.getDataSession()) {
			Transaction transaction = dataSession.beginTransaction();
			dataSession.persist(shLinkManifest);
			transaction.commit();
		}
		return shLinkManifest;
	}


}
