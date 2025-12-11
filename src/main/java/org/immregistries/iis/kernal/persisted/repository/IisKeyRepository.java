package org.immregistries.iis.kernal.persisted.repository;

import java.util.List;
import java.util.Optional;

import org.immregistries.iis.kernal.persisted.model.IisKey;
import org.immregistries.iis.kernal.persisted.model.UserAccess;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IisKeyRepository extends JpaRepository<IisKey, Integer> {
    Optional<IisKey> findByUserAccessAndKeyId(UserAccess userAccess, String keyId);

    List<IisKey> findByUserAccess(UserAccess userAccess);
}
