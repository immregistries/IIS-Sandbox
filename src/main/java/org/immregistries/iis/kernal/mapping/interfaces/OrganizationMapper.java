package org.immregistries.iis.kernal.mapping.interfaces;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.model.Tenant;

public interface OrganizationMapper<Organization extends IBaseResource> {
	public Organization getFhirResource(Tenant tenant);
	public Tenant getTenant(Organization organization);
}
