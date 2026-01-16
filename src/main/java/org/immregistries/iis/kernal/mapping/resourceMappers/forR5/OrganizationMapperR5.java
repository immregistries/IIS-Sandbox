package org.immregistries.iis.kernal.mapping.resourceMappers.forR5;

import org.hl7.fhir.r5.model.Organization;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.mapping.resourceMappers.OrganizationMapper;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Service
@Conditional(OnR5Condition.class)
public class OrganizationMapperR5 implements OrganizationMapper<Organization>, IR5Mapper<Tenant, Organization> {
//	TODO

	public Organization fhirResource(Tenant tenant) {
		Organization o = new Organization();
		o.addIdentifier(MappingHelper.getFhirIdentifierR5("Tenant", Integer.toString(tenant.getOrgId())));
		o.setName(tenant.getOrganizationName());
		return  o;
	}

	public Tenant localObject(Organization organization) {
		Tenant tenant = new Tenant();
		tenant.setOrganizationName(organization.getName());
		tenant.setOrgId(Integer.parseInt(MappingHelper.filterIdentifierR5(organization.getIdentifier(),"Tenant").getValue()));
		return tenant;
	}
}
