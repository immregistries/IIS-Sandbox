package org.immregistries.iis.kernal.mapping.Interfaces;

import org.immregistries.iis.kernal.model.OrgMaster;

public interface OrganizationMapper<Organization> {
	public Organization getFhirResource(OrgMaster orgMaster);
	public OrgMaster getOrgMaster(Organization organization);
}
