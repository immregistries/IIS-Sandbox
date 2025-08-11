package org.immregistries.iis.kernal.model.persisted;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyType;
import org.apache.commons.lang3.StringUtils;

import java.security.PublicKey;
import java.text.ParseException;

public class IisKey {

	private String id;
	private UserAccess userAccess;
	private String keyString;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public UserAccess getUserAccess() {
		return userAccess;
	}

	public void setUserAccess(UserAccess userAccess) {
		this.userAccess = userAccess;
	}

	public String getKeyString() {
		return keyString;
	}

	public void setKeyString(String keyString) {
		this.keyString = keyString;
	}

	@JsonIgnore
	public JWK jwk() {
		JWK jwk = null;
		if (StringUtils.isNotBlank(keyString)) {
			try {
				jwk = JWK.parse(keyString);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}
		return jwk;
	}

	@JsonIgnore
	public PublicKey publicKey() {
		PublicKey publicKey = null;
		JWK jwk = jwk();
		if (jwk != null) {
			try {
				KeyType keyType = jwk.getKeyType();
				if (keyType.equals(KeyType.EC)) {
					publicKey = jwk.toECKey().toPublicKey();
				} else if (keyType.equals(KeyType.RSA)) {
					publicKey = jwk.toRSAKey().toPublicKey();
				} else if (keyType.equals(KeyType.OCT) || keyType.equals(KeyType.OKP)) {
					publicKey = jwk.toOctetKeyPair().toPublicKey();
				}
			} catch (JOSEException e) {
				throw new RuntimeException(e);
			}
		}
		return publicKey;
	}


}
