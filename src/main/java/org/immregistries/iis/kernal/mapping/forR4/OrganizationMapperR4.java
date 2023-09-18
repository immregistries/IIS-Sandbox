package org.immregistries.iis.kernal.mapping.forR4;

import org.immregistries.iis.kernal.fhir.annotations.OnR4Condition;
import org.hl7.fhir.r4.model.Organization;
import org.immregistries.iis.kernal.mapping.Interfaces.OrganizationMapper;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.model.Tenant;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Service
@Conditional(OnR4Condition.class)
public class OrganizationMapperR4 implements OrganizationMapper<Organization> {
//	TODO

	public Organization getFhirResource(Tenant tenant) {
		Organization o = new Organization();
		o.addIdentifier(MappingHelper.getFhirR4Identifier("Tenant", Integer.toString(tenant.getOrgId())));
		o.setName(tenant.getOrganizationName());
		return  o;
	}

	public Tenant getTenant(Organization organization) {
		Tenant tenant = new Tenant();
		tenant.setOrganizationName(organization.getName());
		tenant.setOrgId(Integer.parseInt(MappingHelper.filterR4Identifier(organization.getIdentifier(),"Tenant").getValue()));
		return tenant;
	}
}