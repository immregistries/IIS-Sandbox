package org.immregistries.iis.kernal.model.persisted;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Integer> {
}
