package org.immregistries.iis.kernal.mapping.forR4;

import org.immregistries.iis.kernal.fhir.annotations.OnR4Condition;
import org.hl7.fhir.r4.model.Organization;
import org.immregistries.iis.kernal.mapping.Interfaces.OrganizationMapper;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.model.OrgMaster;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Service
@Conditional(OnR4Condition.class)
public class OrganisationMapperR4 implements OrganizationMapper<Organization> {
//	TODO

	public Organization getFhirResource(OrgMaster orgMaster) {
		Organization o = new Organization();
		o.addIdentifier(MappingHelper.getFhirR4Identifier("OrgMaster", Integer.toString(orgMaster.getOrgId())));
		o.setName(orgMaster.getOrganizationName());
		return  o;
	}

	public OrgMaster getOrgMaster(Organization organization) {
		OrgMaster orgMaster = new OrgMaster();
		orgMaster.setOrganizationName(organization.getName());
		orgMaster.setOrgId(Integer.parseInt(MappingHelper.filterR4Identifier(organization.getIdentifier(),"OrgMaster").getValue()));
		return orgMaster;
	}
}
