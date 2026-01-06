package org.immregistries.iis.kernal.mapping.resourceMappers;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.persisted.model.Tenant;

public interface OrganizationMapper<Organization extends IBaseResource>
		extends IisFhirMapperMaster<Tenant, Organization> {
	default String fhirType() {
		return ORGANIZATION;
	}

	public static final String ORGANIZATION = "Organization";

	default Class<Tenant> localMasterType() {
		return Tenant.class;
	}

	public Organization fhirResource(Tenant tenant);

	public Tenant localObject(Organization organization);
}
