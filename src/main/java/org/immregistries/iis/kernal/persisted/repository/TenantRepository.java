package org.immregistries.iis.kernal.persisted.repository;

import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenantRepository extends JpaRepository<Tenant, Integer> {
	List<Tenant> findByUserAccessId(Integer i);
}
