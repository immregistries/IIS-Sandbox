package org.immregistries.iis.kernal;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import io.jsonwebtoken.*;
import org.hibernate.Session;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.model.UserAccess;
import org.immregistries.iis.kernal.servlet.PopServlet;
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

import static org.immregistries.iis.kernal.fhir.interceptors.SessionAuthorizationInterceptor.CONNECTATHON_USER;

@RestController()
public class JwtAuthProvider {
	@Autowired
	JwtUtils jwtUtils;
	Map<String, PublicKey> keystore;
	Map<String,String> jwtStore;
	Logger logger = LoggerFactory.getLogger(JwtAuthProvider.class);

	private final static String CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

	public JwtAuthProvider() {
		this.keystore = new HashMap<>(10);
		this.jwtStore = new HashMap<>(10);
	}

	@GetMapping("/.well-known/smart-configuration")
	public String wellKnownConfiguration() {
		return "{\n" +
			"  \"token_endpoint\": \"/token\",\n" +
			"  \"token_endpoint_auth_methods_supported\": [\"private_key_jwt\"],\n" +
			"  \"token_endpoint_auth_signing_alg_values_supported\": [\"RS384\", \"ES384\"],\n" +
			"  \"scopes_supported\": [\"system/*.rs\"]\n" +
			"}";
	}

	@PostMapping("/registerClient")
	public String register(@RequestBody String jwkString) { //TODO TLS config
		//		assert(ServletHelper.getUserAccess().getAccessName().equals("admin")); TODO safely define admin user
		try {
			JWK parsedJwk = JWK.parse(jwkString);

			//		String alg = (String) parsedJwk.get("alg");
			String kty = parsedJwk.getKeyType().getValue();
			logger.info("Registering client JWK: {}", parsedJwk); //TODO not log key
			String kid = parsedJwk.getKeyID();
			switch (kty) {
				case "RSA":
					RSAPublicKey rsaJwk = ((RSAKey) parsedJwk).toRSAPublicKey();
					keystore.put(kid,rsaJwk); // TODO change key ?
					break;
				case "EC" :
					ECPublicKey ecJwk = ((ECKey) parsedJwk).toECPublicKey();;
					keystore.put(kid,ecJwk); // TODO change key ?
					break;
				default:
					throw new RuntimeException("Unsupported Algorithm");
			}
			logger.info("Registered client key id: {}, {}", parsedJwk.getKeyID(), keystore.size());
		} catch (ParseException e) {
			throw new RuntimeException(e);
		} catch (JOSEException e) {
			throw new RuntimeException(e);
		}
		return "JWK REGISTERED";
	}


	/**
	 * Only allowed for connectathons users
	 */
	@PostMapping("/token")
	public String smartJwtAuth(@RequestParam String client_assertion_type,  @RequestParam String client_assertion) throws ParseException, JOSEException {
		if (!client_assertion_type.equals(CLIENT_ASSERTION_TYPE) ) {
			throw new InvalidRequestException("Unsupported Client Assertion type,supporting only " + CLIENT_ASSERTION_TYPE);
		}
		SignedJWT signedJWT = SignedJWT.parse(client_assertion);
		/**
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
		/**
		 * Checking supported algorithms
		 */
		switch (alg) {
			case "RS384":
				verifier = new RSASSAVerifier((RSAPublicKey) keystore.get(kid));
				break;
			case  "ES384" :
				verifier = new ECDSAVerifier((ECPublicKey) keystore.get(kid));
				break;
			default:
				throw new RuntimeException("Unsupported Algorithm");
		}
		if (!signedJWT.verify(verifier)) {
			throw new InvalidRequestException("Authentication Error");
		}
		JwtParser jwtParser = Jwts.parser().verifyWith(keystore.get(kid)).build();
		Jws<Claims> claimsJws = jwtParser.parseSignedClaims(client_assertion);
		if(claimsJws.getPayload().get("jti") == null) {
			throw new InvalidRequestException("jti NULL");
		}
		/**
		 * check that the jti value has not been previously encountered for the given iss within the maximum allowed authentication JWT lifetime (e.g., 5 minutes). This check prevents replay attacks.
		 */
		String olderToken = jwtStore.get((String) claimsJws.getPayload().get("jti"));
		if (olderToken != null){
			Jws<Claims> olderJwt = jwtParser.parseSignedClaims(olderToken);
			if (olderJwt.getPayload().get("iss").equals(claimsJws.getPayload().get("iss")) && ((long) olderJwt.getPayload().get("exp")) < System.currentTimeMillis()) {
				throw new RuntimeException("Token already used");
			}
		}
		jwtStore.put((String) signedJWT.getJWTClaimsSet().getClaim("jti"), client_assertion);
		Session dataSession = null;
		try {
			dataSession = PopServlet.getDataSession();
			UserAccess userAccess = ServletHelper.authenticateUserAccessUsernamePassword(CONNECTATHON_USER,CONNECTATHON_USER,dataSession);
			return jwtUtils.generateJwtToken(userAccess);
		} finally {
			if (dataSession != null) {
				dataSession.close();
			}
		}
	}

}
