package org.immregistries.iis.kernal.logic;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.persisted.model.IisKey;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.persisted.model.UserAccess;
import org.immregistries.iis.kernal.persisted.repository.IisKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class KeyStoreService {

	@Autowired
	public IisKeyRepository iisKeyRepository;

	public JWK generateEc() {
		try {
			// Generate a P-256 EC key pair with a random key ID
			return new ECKeyGenerator(Curve.P_256)
					.keyID(UUID.randomUUID().toString())
					.generate();
		} catch (JOSEException exception) {
			throw new RuntimeException(exception);
		}
	}

	public IisKey getKey(String keyId, UserAccess userAccess) {
		return iisKeyRepository.findByUserAccessAndKeyId(userAccess, keyId).orElse(null);
	}

	public IisKey getAnyKey(UserAccess userAccess) {
		return iisKeyRepository.findByUserAccess(userAccess).stream().findAny().orElse(null);
	}

	public List<IisKey> getKeys(UserAccess userAccess) {
		return iisKeyRepository.findByUserAccess(userAccess);
	}

	public IisKey saveKey(JWK keyString, Tenant tenant, UserAccess userAccess) {
		IisKey iisKey = new IisKey();
		iisKey.setKeyId(keyString.getKeyID());
		iisKey.setKeyString(keyString.toJSONString());
		iisKey.setUserAccess(userAccess);
		return iisKeyRepository.save(iisKey);
	}

	public IisKey getIisSigningKeyOrCreate(String keyId, UserAccess userAccess, Tenant tenant) {
		IisKey iisSigningKey;
		if (StringUtils.isNotBlank(keyId)) {
			iisSigningKey = getKey(keyId, userAccess);
		} else {
			iisSigningKey = getAnyKey(userAccess);
		}
		if (iisSigningKey == null) {
			iisSigningKey = saveKey(generateEc(), tenant, userAccess);
		}
		return iisSigningKey;
	}

}
