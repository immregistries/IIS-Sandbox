package org.immregistries.iis.kernal.mapping.resourceMappers;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.persisted.model.Tenant;

public abstract class OrganizationMapper<Organization extends IAnyResource>
		implements IisResourceMasterMapper<Tenant, Organization> {
	public String fhirType() {
		return ORGANIZATION;
	}

	public static final String ORGANIZATION = "Organization";

	public Class<Tenant> localType() {
		return Tenant.class;
	}

	public abstract Organization fhirResource(Tenant tenant);

	public abstract Tenant localObject(Organization organization);
}
