package org.immregistries.iis.kernal.fhir.ips;

import ca.uhn.fhir.jpa.ips.api.SectionRegistry;
import ca.uhn.fhir.jpa.ips.strategy.DefaultIpsGenerationStrategy;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Organization;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.mapping.forR4.OrganizationMapperR4;
import org.immregistries.iis.kernal.model.Tenant;
import org.springframework.beans.factory.annotation.Autowired;

public class IpsGenerationStrategyR4 extends DefaultIpsGenerationStrategy {

	@Autowired
	OrganizationMapperR4 organizationMapper;

	/**
	 * Constructor
	 */
	public IpsGenerationStrategyR4() {
		super();
		this.setSectionRegistry(new SectionRegistryR4());
	}

	@Override
	public IBaseResource createAuthor() {
		Tenant tenant = ServletHelper.getTenant();

		Organization organization = organizationMapper.getFhirResource(tenant);
		return organization;
	}

}
