package org.immregistries.iis.kernal.security;

import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.jetbrains.annotations.NotNull;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Temporary fix to allow the legacy client to query the Fhir
 */
@Service
public class FhirSecretService {

	private final String secret;

	/**
	 * Generating a random
	 * TODO change it regularly dynamically ?
	 */
	public FhirSecretService() {
		SecureRandom secureRandom = new SecureRandom();
		byte[] randomBytes = new byte[16]; // 256 bits
		secureRandom.nextBytes(randomBytes);
		secret = new String(randomBytes);
//		secret = Base64.getEncoder().encodeToString(randomBytes);
//		secret = RandomStringUtils.random(32,1,1,true,true,secureRandom);
	}

	public String secretToken(UserAccess userAccess) {
		return BCrypt.hashpw(fakePassword(userAccess), BCrypt.gensalt(5));
	}

	private @NotNull String fakePassword(UserAccess userAccess) {
		return "pass";
//		return userAccess.getAccessName() + secret;
	}

	public boolean checkToken(UserAccess userAccess, String hashed) {
		return BCrypt.checkpw(fakePassword(userAccess), hashed);
	}
}
