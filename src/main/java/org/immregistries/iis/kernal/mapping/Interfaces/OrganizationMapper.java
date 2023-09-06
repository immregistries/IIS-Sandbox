package org.immregistries.iis.kernal.mapping.Interfaces;

import org.immregistries.iis.kernal.model.Tenant;

public interface OrganizationMapper<Organization> {
	public Organization getFhirResource(Tenant tenant);
	public Tenant getTenant(Organization organization);
}
