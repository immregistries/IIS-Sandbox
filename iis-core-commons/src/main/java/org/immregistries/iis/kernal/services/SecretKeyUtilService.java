package org.immregistries.iis.kernal.services;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class SecretKeyUtilService {

	public @NotNull SecretKeySpec getSecretEncryptionKeyOrCreate(String secretKey) throws NoSuchAlgorithmException {
		SecretKeySpec encryptionKeySpec;
		if (StringUtils.isNotBlank(secretKey)) {
			encryptionKeySpec = new SecretKeySpec(Base64.getDecoder().decode(secretKey), 0, secretKey.length(), "AES");
		} else {
			encryptionKeySpec = generateSecretKey();
		}
		return encryptionKeySpec;
	}

	public @NotNull SecretKeySpec generateSecretKey() throws NoSuchAlgorithmException {
		byte[] randomBytes = new byte[32];
		SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(randomBytes);
		return new SecretKeySpec(randomBytes, "AES");
	}

}
