package org.immregistries.iis.kernal.mapping;

import org.hl7.fhir.r5.model.Organization;
import org.immregistries.iis.kernal.model.OrgMaster;

public class OrganisationMapper {
//	TODO

	public static Organization getFhirOrganization(OrgMaster orgMaster) {
		Organization o = new Organization();
		o.addIdentifier(MappingHelper.getFhirIdentifier("OrgMaster", Integer.toString(orgMaster.getOrgId())));
		o.setName(orgMaster.getOrganizationName());
		return  o;
	}

	public static OrgMaster getOrgMaster(Organization organization) {
		OrgMaster orgMaster = new OrgMaster();
		orgMaster.setOrganizationName(organization.getName());
		orgMaster.setOrgId(Integer.parseInt(MappingHelper.filterIdentifier(organization.getIdentifier(),"OrgMaster").getValue()));
		return orgMaster;
	}
}
