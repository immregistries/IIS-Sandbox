package org.immregistries.iis.kernal.fhir.ips;

import ca.uhn.fhir.jpa.ips.api.IpsContext;
import ca.uhn.fhir.jpa.ips.strategy.DefaultIpsGenerationStrategy;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.model.api.Include;
import com.google.common.collect.Sets;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.mapping.forR5.OrganizationMapperR5;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * prototype, unusable
 */
public class IpsGenerationStrategyR5 extends DefaultIpsGenerationStrategy {

	@Autowired
	OrganizationMapperR5 organizationMapper;
	/**
	 * Constructor
	 */
	public IpsGenerationStrategyR5() {
		super();
		this.setSectionRegistry(new SectionRegistryR5());
	}

	@Override
	public IBaseResource createAuthor() {
		Organization organization = organizationMapper.getFhirResource(ServletHelper.getTenant());
		return organization;
	}


	@SuppressWarnings("EnhancedSwitchMigration")
	@Override
	public boolean shouldInclude(IpsContext.IpsSectionContext theIpsSectionContext, IBaseResource theCandidate) {

		switch (theIpsSectionContext.getSection()) {
			case IMMUNIZATIONS:
				if (theIpsSectionContext.getResourceType().equals(ResourceType.Immunization.name())) {
					Immunization immunization = (Immunization) theCandidate;
					return immunization.getStatus() != Immunization.ImmunizationStatusCodes.ENTEREDINERROR;
				}
				break;
		}

		return true;
	}
}
