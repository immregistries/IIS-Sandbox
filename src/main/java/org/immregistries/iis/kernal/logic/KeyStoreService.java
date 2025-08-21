package org.immregistries.iis.kernal.logic;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import jakarta.persistence.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.model.persisted.IisKey;
import org.immregistries.iis.kernal.model.persisted.Tenant;
import org.immregistries.iis.kernal.model.persisted.UserAccess;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class KeyStoreService {

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
		try (Session dataSession = ServletHelper.getDataSession()) {
			Query query = dataSession.createQuery(
				"from IisKey where userAccess = :user and keyId = :kid", IisKey.class);
			query.setParameter("user", userAccess);
			query.setParameter("kid", keyId);
			return (IisKey) query.getSingleResult();
		}
	}

	public IisKey saveKey(JWK keyString, Tenant tenant, UserAccess userAccess) {
		IisKey iisKey = new IisKey();
		iisKey.setKeyId(keyString.getKeyID());
		iisKey.setKeyString(keyString.toJSONString());
		iisKey.setUserAccess(userAccess);
		recordIisKey(iisKey);
		return iisKey;
	}

	private void recordIisKey(IisKey iisKey) {
		try (Session dataSession = ServletHelper.getDataSession()) {
			Transaction transaction = dataSession.beginTransaction();
			dataSession.persist(iisKey);
			transaction.commit();
		}
	}

}
