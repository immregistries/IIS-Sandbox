package org.immregistries.iis.kernal.logic.shlink;

import ca.uhn.fhir.context.FhirContext;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.immregistries.iis.kernal.logic.KeyStoreService;
import org.immregistries.iis.kernal.model.persisted.IisKey;
import org.immregistries.iis.kernal.model.persisted.Tenant;
import org.immregistries.iis.kernal.model.persisted.UserAccess;
import org.immregistries.iis.kernal.servlet.WellKnownKeyController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ShCardUtil {

	private final static String SIGNATURE_ALGORITHM_NAME = "HmacSha512";


	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private static final int MAX_SINGLE_JWS_SIZE = 1195;
	private static final int MAX_CHUNK_SIZE = 1191;

	private static final int SMALLEST_B64_CHAR_CODE = 45;
	public static final String VERIFIABLE_CREDENTIAL_TYPE = "VerifiableCredential";
	public static final String HTTPS_SMARTHEALTH_CARDS_HEALTH_CARD = "https://smarthealth.cards#health-card";
	public static final String HTTPS_SMARTHEALTH_CARDS_IMMUNIZATION = "https://smarthealth.cards#immunization";
	public static final String FHIR_VERSION = "fhirVersion";
	public static final String TYPE = "type";
	public static final String FHIR_BUNDLE = "fhirBundle";
	public static final String CREDENTIAL_SUBJECT = "credentialSubject";
	public static final String SHC_HEADER = "shc:/";
	public static final String ISSUER_KEY = "issuerKey";
	public static final String VC = "vc";

	private Gson gson = new Gson();

	@Autowired
	KeyStoreService keyStoreService;

	@Autowired
	private FhirContext fhirContext;

	public String qrCompact(IBaseBundle iBaseBundle, HttpServletRequest request, IisKey signingKey, UserAccess userAccess, Tenant tenant) throws IOException {
		String resourceString = fhirContext.newJsonParser().setSummaryMode(true).encodeResourceToString(iBaseBundle);
		return qrCompact(resourceString, request, signingKey, userAccess, tenant);
	}

	public String qrCompact(String resourceString, HttpServletRequest request, IisKey iisKey, UserAccess userAccess, Tenant tenant) throws IOException {
		KeyPair signingKeyPair = iisKey.keyPair();

		Map<String, Object> mapVc = new HashMap<>(2);
		ArrayList<String> type = new ArrayList<>(3);
		type.add(VERIFIABLE_CREDENTIAL_TYPE);
		type.add(HTTPS_SMARTHEALTH_CARDS_HEALTH_CARD);
		type.add(HTTPS_SMARTHEALTH_CARDS_IMMUNIZATION);
		mapVc.put(TYPE, type);

		Map<String, Object> credentialSubject = new HashMap<>(2);
		credentialSubject.put(FHIR_VERSION, fhirContext.getVersion().getVersion().getFhirVersionString());
		credentialSubject.put(FHIR_BUNDLE, JsonParser.parseString(resourceString).getAsJsonObject());
		mapVc.put(CREDENTIAL_SUBJECT, credentialSubject);

		String issuerUrl = WellKnownKeyController.getKeyIssuerUrl(request, tenant);
		Claims claims = Jwts.claims()
			.notBefore(new Date())
			.issuer(issuerUrl)
			.issuedAt(new Date())
			.add(VC, mapVc)
			.build();

		String claimsString = CompressionUtil.minifyJson(gson.toJson(claims));

		JwtBuilder jwtBuilder = Jwts.builder()
			.compressWith(Jwts.ZIP.DEF)
			.header()
			.add("use", "SIG")
			.keyId(iisKey.getKeyId())
			.and()
			.content(claimsString)
			.signWith(signingKeyPair.getPrivate());
		String compact = jwtBuilder.compact();
//		logger.info("parsed {}", Jwts.parser().verifyWith(signingKeyPair.getPublic()).build().parse(compact));
		return compact;
	}

	private static String getEncodedForQrCode(String compact) {
		String encodedForQrCode = compact.
			chars().map(value -> value - SMALLEST_B64_CHAR_CODE)
			.boxed()
			.map(integer -> String.valueOf(integer / 10) + integer % 10)
			.collect(Collectors.joining());
		return encodedForQrCode;
	}


}
