
package org.immregistries.iis.kernal.persisted.repository;

import java.util.List;

import org.immregistries.iis.kernal.persisted.model.UserAccess;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccessRepository extends JpaRepository<UserAccess, Integer> {
    List<UserAccess> findByAccessName(String accessName);
}
