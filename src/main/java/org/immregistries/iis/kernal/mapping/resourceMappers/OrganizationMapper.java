package org.immregistries.iis.kernal.mapping.resourceMappers;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.persisted.model.Tenant;

public interface OrganizationMapper<Organization extends IAnyResource>
		extends IisResourceMasterMapper<Tenant, Organization> {
	default String fhirType() {
		return ORGANIZATION;
	}

	String ORGANIZATION = "Organization";

	default Class<Tenant> localType() {
		return Tenant.class;
	}

	Organization fhirResource(Tenant tenant);

	Tenant localObject(Organization organization);
}
