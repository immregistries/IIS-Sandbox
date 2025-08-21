package org.immregistries.iis.kernal.logic.shlink;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import org.immregistries.iis.kernal.logic.KeyStoreService;
import org.immregistries.iis.kernal.model.persisted.IisKey;
import org.immregistries.iis.kernal.model.persisted.UserAccess;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

@Service
public class ShCardUtil {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private static final int MAX_SINGLE_JWS_SIZE = 1195;
	private static final int MAX_CHUNK_SIZE = 1191;

	public static final int MAXIMUM_DATA_SIZE = 30000;
	private static final int SMALLEST_B64_CHAR_CODE = 45;
	public static final String VERIFIABLE_CREDENTIAL = "VerifiableCredential";
	public static final String HTTPS_SMARTHEALTH_CARDS_HEALTH_CARD = "https://smarthealth.cards#health-card";
	public static final String HTTPS_SMARTHEALTH_CARDS_IMMUNIZATION = "https://smarthealth.cards#immunization";
	public static final String FHIR_VERSION = "fhirVersion";
	public static final String TYPE = "type";
	public static final String FHIR_BUNDLE = "fhirBundle";
	public static final String CREDENTIAL_SUBJECT = "credentialSubject";
	public static final String SHC_HEADER = "shc:/";
	public static final String ISSUER_KEY = "issuerKey";
	public static final String VC = "vc";


	@Autowired
	KeyStoreService keyStoreService;

	@Autowired
	private FhirContext fhirContext;

	public String qrCodeWrite(String resourceString, HttpServletRequest request, String kid, UserAccess userAccess) {
		Gson gson = new Gson();

		Map<String, Object> mapVc = new HashMap<>(2);
		ArrayList<String> type = new ArrayList<>(3);
		type.add(VERIFIABLE_CREDENTIAL);
		type.add(HTTPS_SMARTHEALTH_CARDS_HEALTH_CARD);
		type.add(HTTPS_SMARTHEALTH_CARDS_IMMUNIZATION);
		mapVc.put(TYPE, type);

		Map<String, Object> credentialSubject = new HashMap<>(2);
		credentialSubject.put(FHIR_VERSION, fhirContext.getVersion().getVersion().getFhirVersionString());
		credentialSubject.put(FHIR_BUNDLE, JsonParser.parseString(resourceString).getAsJsonObject());
		mapVc.put(CREDENTIAL_SUBJECT, credentialSubject);

		String issuerUrl = request.getRequestURL().substring(0, request.getRequestURL().indexOf("/tenants"));
		Claims claims = Jwts.claims()
			.notBefore(new Date())
			.issuer(issuerUrl)
			.issuedAt(new Date())
			.add(VC, mapVc)
			.build();

		String claimsString = gson.toJson(claims).strip();
		/**
		 * Compressing the content
		 */
		byte[] deflated = rawDeflate(claimsString);

		IisKey iisKey = keyStoreService.getKey(kid, userAccess);
		KeyPair keyPair = iisKey.keyPair();
		PrivateKey privateKey = keyPair.getPrivate();
		/**
		 * DEF header added manually as we are using raw deflation
		 */
		JwtBuilder jwtBuilder = Jwts.builder()
			.header()
			.add("use", "SIG")
			.add("zip", "DEF")
			.keyId(kid)
			.and()
			.content(deflated)
//                .compressWith(Jwts.ZIP.DEF)
			.signWith(privateKey);
		String compact = jwtBuilder.compact();
		logger.info("compact {}", compact);
		PublicKey publicKey = keyPair.getPublic();
		logger.info("parsed {}", Jwts.parser().verifyWith(publicKey).build().parse(compact));

		// for download file
//        Map<String, ArrayList<String>> shcMap = new HashMap<>(1);
//        ArrayList<String> arrayList = new ArrayList<>(1);
//        arrayList.add(compact);
//        shcMap.put("verifiableCredential", arrayList);
//        logger.info("shcMap: {}", shcMap);
		return getEncodedForQrCode(compact);
	}

	/**
	 * Split endoded for Qr Code String into several smaller codes
	 *
	 * @param encodedForQrCode
	 * @return
	 */
	public static @NotNull List<String> divideQrCode(String encodedForQrCode) {
		int finalLength = encodedForQrCode.length();
		List<String> result;
		if (finalLength < MAX_SINGLE_JWS_SIZE) {
			result = List.of(SHC_HEADER + encodedForQrCode);
		} else {
			int numberOfChunks = finalLength / MAX_CHUNK_SIZE;
			if (finalLength % MAX_CHUNK_SIZE > 0) {
				numberOfChunks += 1;
			}
			result = new ArrayList<>(numberOfChunks);
			int chunkSize = finalLength / numberOfChunks;
			for (int i = 1; i < numberOfChunks; i++) {
				result.add(SHC_HEADER + i + "/" + numberOfChunks + "/" + encodedForQrCode.substring((i - 1) * chunkSize, i * chunkSize));
			}
			result.add(SHC_HEADER + numberOfChunks + "/" + numberOfChunks + "/" +
				encodedForQrCode.substring((numberOfChunks - 1) * chunkSize, finalLength - 1));
		}
		return result;
	}


	public String parseVCFromCompactJwtUnsecure(String compact) {
		Gson gson = new Gson();
		String[] chunks = compact.split("\\.");
		Base64.Decoder decoder = Base64.getUrlDecoder();
		JsonObject header = JsonParser.parseString(new String(decoder.decode(chunks[0]))).getAsJsonObject();
//        logger.info("header {}", header);
		byte[] payload = decoder.decode(chunks[1]);
		String payloadString;
		if (header.has("zip") && header.get("zip").getAsString().equalsIgnoreCase("DEF")) {
			payloadString = rawInflate(payload);
		} else {
			payloadString = new String(payload);
		}

		JsonObject jwtPayload = JsonParser.parseString(payloadString).getAsJsonObject();
		return jwtPayload.getAsJsonObject(VC).toString();
	}


	public JsonObject parseVCFromJwt(Jwt jwt) {
		Gson gson = new Gson();
		logger.info("JWT {}", gson.toJson(jwt));
		JsonObject jwtPayload = gson.toJsonTree(jwt.getPayload()).getAsJsonObject();
		JsonObject vc = null;
		if (jwtPayload.has(VC)) {
			vc = jwtPayload.getAsJsonObject(VC);
		}
		return vc;
	}

	private static String getEncodedForQrCode(String compact) {
		String encodedForQrCode = compact.
			chars().map(value -> value - SMALLEST_B64_CHAR_CODE)
			.boxed()
			.map(integer -> String.valueOf(integer / 10) + integer % 10)
			.collect(Collectors.joining());
		return encodedForQrCode;
	}

	public static String rawInflate(byte[] deflated) {
		try {
			Inflater inflater = new Inflater(true);
			inflater.setInput(deflated);
			byte[] result = new byte[MAXIMUM_DATA_SIZE];
			int resultLength = inflater.inflate(result);
			inflater.end();
			return new String(result).substring(0, resultLength);
		} catch (DataFormatException e) {
			throw new RuntimeException(e);
		}
	}

	private static byte[] rawDeflate(String claimsString) {
		byte[] output = new byte[MAXIMUM_DATA_SIZE];
		Deflater deflater = new Deflater();
		deflater.setInput(claimsString.getBytes());
		deflater.finish();
		int compressedDataSize = deflater.deflate(output);
		if (compressedDataSize >= MAXIMUM_DATA_SIZE) {
			throw new InternalErrorException("Resource is too large");
		}
		byte[] deflated = Arrays.copyOfRange(output, 0, compressedDataSize);
		return deflated;
	}
}
