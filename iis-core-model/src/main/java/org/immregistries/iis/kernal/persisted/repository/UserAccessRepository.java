
package org.immregistries.iis.kernal.persisted.repository;

import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccessRepository extends JpaRepository<UserAccess, Integer> {
    Optional<UserAccess> findByAccessName(String accessName);

}
