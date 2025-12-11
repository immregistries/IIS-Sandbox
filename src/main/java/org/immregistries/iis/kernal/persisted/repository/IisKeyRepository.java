package org.immregistries.iis.kernal.persisted.repository;

import org.immregistries.iis.kernal.persisted.model.IisKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IisKeyRepository extends JpaRepository<IisKey, Integer> {
}
