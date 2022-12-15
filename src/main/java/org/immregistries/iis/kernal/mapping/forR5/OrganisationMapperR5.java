package org.immregistries.iis.kernal.mapping.forR5;

import org.hl7.fhir.r5.model.Organization;
import org.immregistries.iis.kernal.mapping.Interfaces.OrganizationMapper;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.model.OrgMaster;
import org.springframework.stereotype.Service;

@Service
public class OrganisationMapperR5 implements OrganizationMapper<Organization> {
//	TODO

	public Organization getFhirResource(OrgMaster orgMaster) {
		Organization o = new Organization();
		o.addIdentifier(MappingHelper.getFhirIdentifier("OrgMaster", Integer.toString(orgMaster.getOrgId())));
		o.setName(orgMaster.getOrganizationName());
		return  o;
	}

	public OrgMaster getOrgMaster(Organization organization) {
		OrgMaster orgMaster = new OrgMaster();
		orgMaster.setOrganizationName(organization.getName());
		orgMaster.setOrgId(Integer.parseInt(MappingHelper.filterIdentifier(organization.getIdentifier(),"OrgMaster").getValue()));
		return orgMaster;
	}
}
