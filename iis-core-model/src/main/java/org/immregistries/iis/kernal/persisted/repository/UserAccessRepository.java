
package org.immregistries.iis.kernal.persisted.repository;

import org.immregistries.iis.kernal.enums.UserRole;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserAccessRepository extends JpaRepository<UserAccess, Integer> {
    Optional<UserAccess> findByAccessName(String accessName);

	Optional<UserAccess> findByAccessNameIgnoreCase(String accessName);

	/**
	 * roles is an EAGER element collection, so a plain findAll() issues one extra select per
	 * user. The join fetch collapses the listing to a single query.
	 */
	@Query("select distinct u from UserAccess u left join fetch u.roles")
	List<UserAccess> findAllWithRoles();

	/**
	 * Backs the last-admin guard. A NULL enabled means enabled, hence the explicit predicate
	 * rather than a derived query (SQL would drop NULLs from {@code enabled <> false}).
	 */
	@Query("select count(u) from UserAccess u join u.roles r where r = :role and (u.enabled is null or u.enabled = true)")
	long countEnabledWithRole(@Param("role") UserRole role);
}
