package org.immregistries.iis.kernal.persisted.repository;

import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Integer> {
	List<Tenant> findByUserAccessId(Integer i);

	Optional<Tenant> findByIdAndUserAccessId(Integer i, Integer userAccessId);

	Optional<Tenant> findByOrganizationNameAndUserAccessId(String organizationName, Integer userAccessId);

	Optional<Tenant> findByOrganizationName(String organizationName);

}
