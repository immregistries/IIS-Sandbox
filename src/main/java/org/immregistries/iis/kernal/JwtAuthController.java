package org.immregistries.iis.kernal;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.hibernate.Session;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.model.UserAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.immregistries.iis.kernal.fhir.interceptors.CustomAuthorizationInterceptor.CONNECTATHON_USER;

/**
 * Used for SMART AUTH, and testing around keystores
 * TODO proper key store
 */
@RestController()
public class JwtAuthController {
	@Autowired
	JwtUtils jwtUtils;
	private final Map<String, PublicKey> keystore;
	private final Map<String, String> jwtStore;
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final static String CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

	public JwtAuthController() {
		this.keystore = new HashMap<>(10);
		this.jwtStore = new HashMap<>(10);
	}

	/**
	 * Well known configuration for SMART auth
	 * TODO update
	 *
	 * @return Well known configuration
	 */
	@GetMapping("/.well-known/smart-configuration")
	public String wellKnownConfiguration() {
		return "{\n" +
			"  \"token_endpoint\": \"https://bulksandbox.pagekite.me/iis/token\",\n" +
			"  \"token_endpoint_auth_methods_supported\": [\"private_key_jwt\", \"client-confidential-asymmetric\"],\n" +
			"  \"token_endpoint_auth_signing_alg_values_supported\": [\"RS384\", \"ES384\", \"RS512\", \"ES512\"],\n" +
			"  \"scopes_supported\": [\"system/*.rs\"]\n" +
			"}";
	}

	/**
	 * Registers JWK for SMART Auth operations
	 * @param jwkString JSON Web Key in String format
	 * @param authHeader Optional authHeader
	 * @return Confirmation or exception message
	 */
	@PostMapping("/registerClient")
	public String register(@RequestBody String jwkString, @RequestHeader("Authorization") Optional<String> authHeader) { //TODO TLS config
//		Session dataSession = null;
//		UserAccess userAccess = null;
//		try {
//			dataSession = PopServlet.getDataSession();
//			if (authHeader != null && authHeader.startsWith("Basic ")) {
//				String base64 = authHeader.substring("Basic ".length());
//				String base64decoded = new String(Base64.decodeBase64(base64));
//				String[] parts = base64decoded.split(":");
//				userAccess = ServletHelper.authenticateUserAccessUsernamePassword(parts[0], parts[1], dataSession);
//			}
//		} finally {
//			dataSession.close();
//		}
//		if (userAccess == null || !userAccess.getAccessName().equals("admin")) {
//			throw new AuthenticationException();
//		}
		//		assert(ServletHelper.getUserAccess().getAccessName().equals("admin")); TODO safely define admin user
		try {
			logger.info("registering {}", jwkString);
			JWK parsedJwk = JWK.parse(jwkString);

			//		String alg = (String) parsedJwk.get("alg");
			String kty = parsedJwk.getKeyType().getValue();
			logger.info("Registering client JWK: {}", parsedJwk); //TODO not log key
			String kid = parsedJwk.getKeyID();
			switch (kty) {
				case "RSA":
					RSAPublicKey rsaJwk = ((RSAKey) parsedJwk).toRSAPublicKey();
					keystore.put(kid, rsaJwk); // TODO change key ?
					break;
				case "EC":
				case "EC384":
					ECPublicKey ecJwk = ((ECKey) parsedJwk).toECPublicKey();
					keystore.put(kid, ecJwk); // TODO change key ?
					break;
//				case "EC" :
//					ECPublicKey ecJwk = ((ECKey) parsedJwk).toECPublicKey();;
//					keystore.put(kid,ecJwk); // TODO change key ?
//					break;
				default:
					throw new RuntimeException("Unsupported Algorithm");
			}
			logger.info("Registered client key id: {}, {}", parsedJwk.getKeyID(), keystore.size());
		} catch (ParseException | JOSEException e) {
			throw new RuntimeException(e);
		}
		return "JWK REGISTERED";
	}


	/**
	 * Only allowed for connectathon users
	 */
	@PostMapping("/token")
	public String smartJwtAuth(@RequestParam Map<String, String> map) throws ParseException, JOSEException {
//		String client_assertion_type = map.get("client_assertion_type");
//		String client_assertion = map.get("client_assertion");
		return smartJwtAuthGet(map);
	}

	/**
	 * SMART Auth get JWT
	 * @param map HTTP Parameters map
	 * @return JWT
	 * @throws ParseException Parsing exception
	 * @throws JOSEException Invalid Key exception
	 */
	@GetMapping("/token")
	public String smartJwtAuthGet(@RequestParam Map<String, String> map) throws ParseException, JOSEException {
		String client_assertion_type = map.get("client_assertion_type");
		String client_assertion = map.get("client_assertion");
		String scope = map.get("scope");
		String grant_type = map.get("grant_type");
		if (!client_assertion_type.equals(CLIENT_ASSERTION_TYPE) ) {
			throw new InvalidRequestException("Unsupported Client Assertion type,supporting only " + CLIENT_ASSERTION_TYPE);
		}
		SignedJWT signedJWT = SignedJWT.parse(client_assertion);
		/*
		 * Reading Jwt Headers
		 */
		String alg = signedJWT.getHeader().getAlgorithm().getName();
		String typ = signedJWT.getHeader().getType().getType();
		String kid = signedJWT.getHeader().getKeyID();
//		String jku = signedJWT.getHeader().getJWKURL(); not supported

		if (!typ.equals("JWT")) {
			throw new InvalidRequestException("Unsupported type header,supporting only JWT");
		}
		if(!keystore.containsKey(kid)) {
			throw new InvalidRequestException("Unknown key id " + kid);
		}
		JWSVerifier verifier;
		/*
		 * Checking supported algorithms
		 */
		switch (alg) {
			case "RS256":
			case "RS384":
			case "RS512":
				verifier = new RSASSAVerifier((RSAPublicKey) keystore.get(kid));
				break;
			case  "ES384" :
			case  "ES512" :
				verifier = new ECDSAVerifier((ECPublicKey) keystore.get(kid));
				break;
			default:
				throw new RuntimeException("Unsupported Algorithm " + alg);
		}
		if (!signedJWT.verify(verifier)) {
			throw new InvalidRequestException("Authentication Error, Token not verified by public key");
		}
		JwtParser jwtParser = Jwts.parser().verifyWith(keystore.get(kid)).build();
		Jws<Claims> claimsJws = jwtParser.parseSignedClaims(client_assertion);
		if(claimsJws.getPayload().get("jti") == null) {
			throw new InvalidRequestException("invalid jti claim : " + claimsJws.getPayload().get("jti"));
		}
		/*
		 * check that the jti value has not been previously encountered for the given iss within the maximum allowed authentication JWT lifetime (e.g., 5 minutes). This check prevents replay attacks.
		 */
		String olderToken = jwtStore.get((String) claimsJws.getPayload().get("jti"));
		if (olderToken != null){
			Jws<Claims> olderJwt = jwtParser.parseSignedClaims(olderToken);
			if (olderJwt.getPayload().get("iss").equals(claimsJws.getPayload().get("iss")) && ((long) olderJwt.getPayload().get("exp")) < System.currentTimeMillis() / 1000) {
				throw new RuntimeException("Token already used");
			}
		}
		jwtStore.put((String) signedJWT.getJWTClaimsSet().getClaim("jti"), client_assertion);
		Session dataSession = null;
		try {
			dataSession = ServletHelper.getDataSession();
			UserAccess userAccess = ServletHelper.authenticateUserAccessUsernamePassword(CONNECTATHON_USER,"SundaysR0ck!",dataSession);
			Map<String, String> result = new HashMap<>(5);
			result.put("access_token", jwtUtils.generateJwtToken(userAccess));
			result.put("token_type", "bearer");
			result.put("expires_in", "300");
			result.put("scope", scope);
			Gson gson = new Gson();
			String gsonData = gson.toJson(result, new TypeToken<HashMap>(){}.getType());
			return gsonData;
		} finally {
			if (dataSession != null) {
				dataSession.close();
			}
		}
	}

}
