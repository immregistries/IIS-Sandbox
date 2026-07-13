package org.immregistries.iis.kernal.persisted.repository;

import org.immregistries.iis.kernal.persisted.entities.ShLinkManifest;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShlinkManifestRepository extends JpaRepository<ShLinkManifest, String> {
	List<ShLinkManifest> findByTenantAndPatientId(Tenant tenant, String patientId);

	List<ShLinkManifest> findByTenant(Tenant tenant);
}
