package org.immregistries.iis.kernal.persisted.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyType;
import jakarta.persistence.*;
import org.apache.commons.lang3.StringUtils;

import java.security.KeyPair;
import java.security.PublicKey;
import java.text.ParseException;

/**
 * Persisting generated key for smart health links and cards
 */
@Entity
@Table
public class IisKey {


	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	private int id;

	private String keyId;
	@JsonIgnore
	@ManyToOne
	private UserAccess userAccess;
	private String keyString;

	public int getId() {
		return id;
	}

	public void setId(int id) {
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
	public KeyPair keyPair() {
		KeyPair publicKey = null;
		JWK jwk = jwk();
		if (jwk != null) {
			try {
				KeyType keyType = jwk.getKeyType();
				if (keyType.equals(KeyType.EC)) {
					publicKey = jwk.toECKey().toKeyPair();
				} else if (keyType.equals(KeyType.RSA)) {
					publicKey = jwk.toRSAKey().toKeyPair();
				} else if (keyType.equals(KeyType.OCT) || keyType.equals(KeyType.OKP)) {
					publicKey = jwk.toOctetKeyPair().toKeyPair();
				}
			} catch (JOSEException e) {
				throw new RuntimeException(e);
			}
		}
		return publicKey;
	}

	@JsonIgnore
	public PublicKey publicKey() {
		return keyPair().getPublic();
	}

	public String getKeyId() {
		return keyId;
	}

	public void setKeyId(String keyId) {
		this.keyId = keyId;
	}
}
