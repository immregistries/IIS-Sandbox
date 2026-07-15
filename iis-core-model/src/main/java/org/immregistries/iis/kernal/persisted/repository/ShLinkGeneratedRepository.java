package org.immregistries.iis.kernal.persisted.repository;

import org.immregistries.iis.kernal.persisted.entities.ShLinkGenerated;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShLinkGeneratedRepository extends JpaRepository<ShLinkGenerated, String> {
	List<ShLinkGenerated> findByTenantAndPatientId(Tenant tenant, String patientId);

	List<ShLinkGenerated> findByTenant(Tenant tenant);
}
