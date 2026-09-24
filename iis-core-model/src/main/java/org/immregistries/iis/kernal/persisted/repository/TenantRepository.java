package org.immregistries.iis.kernal.persisted.repository;

import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Integer> {
	List<Tenant> findByUserAccessId(Integer i);

	Optional<Tenant> findByOrgIdAndUserAccessId(Integer orgId, Integer userAccessId);

	Optional<Tenant> findByOrganizationNameAndUserAccessId(String organizationName, Integer userAccessId);

	Optional<Tenant> findByOrganizationName(String organizationName);

	/**
	 * Owner id plus tenant name for every tenant, in one query. Used by the admin user
	 * listing: Tenant.userAccess is an EAGER @ManyToOne, so iterating findAll() would load
	 * each owner separately.
	 */
	@Query("select t.userAccess.id as userAccessId, t.organizationName as organizationName from Tenant t")
	List<TenantOwnerView> findAllTenantOwners();

	interface TenantOwnerView {
		Integer getUserAccessId();

		String getOrganizationName();
	}
}
