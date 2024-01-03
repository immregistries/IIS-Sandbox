package org.immregistries.iis.kernal;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
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

@RestController()
public class JwtAuthProvider {
	@Autowired
	JwtUtils jwtUtils;
	Map<String, PublicKey> keystore;
	Map<String,SignedJWT> jwtStore;
	Logger logger = LoggerFactory.getLogger(JwtAuthProvider.class);

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
		logger.info("Registering client JWK: {}", jwkString); //TODO not log key
		Map<String,Object> parsedJwk = new Gson().fromJson(jwkString, new TypeToken<HashMap<String, Object>>() {}.getType());
		String alg = (String) parsedJwk.get("alg");
		String kid = (String) parsedJwk.get("kid");
		switch (alg) {
			case "RS384":
				RSAPublicKey rsaJwk = new Gson().fromJson(jwkString, RSAPublicKey.class);
				keystore.put(kid,rsaJwk); // TODO change key ?
				break;
			case  "ES384" :
				ECPublicKey ecJwk = new Gson().fromJson(jwkString, ECPublicKey.class);
				keystore.put(kid,ecJwk); // TODO change key ?
				break;
			default:
				throw new RuntimeException("Unsupported Algorithm");
		}
		return "ok";
	}

	/**
	 * Only allowed for connectathons users
	 */
	@PostMapping("/token")
	public String smartJwtAuth(@RequestParam String client_assertion_type,  @RequestParam String client_assertion) throws ParseException, JOSEException {
		SignedJWT signedJWT = SignedJWT.parse(client_assertion);
		/**
		 * Reading Jwt Headers
		 */
		String alg = signedJWT.getHeader().getAlgorithm().getName();
		String typ = signedJWT.getHeader().getType().getType();
		String kid = signedJWT.getHeader().getKeyID();
//		String jku = signedJWT.getHeader().getJWKURL(); not supported

		assert (typ.equals("JWT"));
		assert (keystore.containsKey(kid));
		JWSVerifier verifier;
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
		assert(signedJWT.verify(verifier));
		assert(signedJWT.getJWTClaimsSet().getClaim("jti") != null);
		/**
		 * check that the jti value has not been previously encountered for the given iss within the maximum allowed authentication JWT lifetime (e.g., 5 minutes). This check prevents replay attacks.
		 */
		SignedJWT olderJwt = jwtStore.get((String) signedJWT.getJWTClaimsSet().getClaim("jti"));
		if (olderJwt != null && olderJwt.verify(verifier) && olderJwt.getJWTClaimsSet().getClaim("iss").equals(signedJWT.getJWTClaimsSet().getClaim("iss")) && ((long) olderJwt.getJWTClaimsSet().getClaim("exp")) < System.currentTimeMillis()) {
			throw new RuntimeException("Token already used");
		}
		jwtStore.put((String) signedJWT.getJWTClaimsSet().getClaim("jti"), signedJWT);
		Session dataSession = null;
		try {
			dataSession = PopServlet.getDataSession();
			UserAccess userAccess = ServletHelper.authenticateUserAccessUsernamePassword("Connectathon","Connectathon",dataSession);
			return jwtUtils.generateJwtToken(userAccess);
		} finally {
			if (dataSession != null) {
				dataSession.close();
			}
		}
	}

}
