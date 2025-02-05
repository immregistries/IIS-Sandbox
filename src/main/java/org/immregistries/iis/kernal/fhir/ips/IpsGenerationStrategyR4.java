package org.immregistries.iis.kernal.fhir.ips;

import ca.uhn.fhir.jpa.ips.api.IpsContext;
import ca.uhn.fhir.jpa.ips.api.IpsSectionEnum;
import ca.uhn.fhir.jpa.ips.api.SectionRegistry;
import ca.uhn.fhir.jpa.ips.strategy.DefaultIpsGenerationStrategy;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.*;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.mapping.forR4.OrganizationMapperR4;
import org.immregistries.iis.kernal.mapping.internalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.model.Tenant;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester.GOLDEN_RECORD;
import static org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester.GOLDEN_SYSTEM_TAG;

public class IpsGenerationStrategyR4 extends DefaultIpsGenerationStrategy  implements  ICustomIpsGenerationStrategy{

	@Autowired
	OrganizationMapperR4 organizationMapper;
	@Autowired
	RepositoryClientFactory repositoryClientFactory;

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

	public IBaseBundle everything(IIdType theOriginalSubjectId, SectionRegistry.Section theSection) {
		Parameters inParams = new Parameters();
		inParams.addParameter("_mdm", true);
		inParams.addParameter("type", StringUtils.join(theSection.getResourceTypes(),","));
		Bundle bundle = repositoryClientFactory.getFhirClient().operation().onServer().named(JpaConstants.OPERATION_EVERYTHING).withParameters(inParams)
			.returnResourceType(Bundle.class).execute();
		return bundle;
	}

	public List<IBaseResource> extractResourcesFromBundle(IpsContext.IpsSectionContext theIpsSectionContext, IBaseBundle iBaseBundle) {
		Bundle bundle = (Bundle) iBaseBundle;
		return bundle.getEntry().stream()
			.filter((bundleEntryComponent -> bundleEntryComponent.hasResource() && theIpsSectionContext.getResourceType().equals(bundleEntryComponent.getResource().getResourceType().name())))
			.map(Bundle.BundleEntryComponent::getResource).collect(Collectors.toList());
	}

	public String mdmLinksParameterIds(IIdType theOriginalSubjectId, SectionRegistry.Section theSection) {
		Parameters inParams = new Parameters();
		inParams.addParameter("resourceId", theOriginalSubjectId.getValue());
		Bundle bundle = repositoryClientFactory.getFhirClient().operation().onServer().named("$mdm-query-links").withParameters(inParams)
			.returnResourceType(Bundle.class).execute();
		return bundle.getEntry().stream().map(bundleEntryComponent -> bundleEntryComponent.getResource().getId()).collect(Collectors.joining(","));
	}

	@Override
	public boolean shouldInclude(IpsContext.IpsSectionContext theIpsSectionContext, IBaseResource theCandidate) {
		if (Objects.requireNonNull(theIpsSectionContext.getSection()) == IpsSectionEnum.IMMUNIZATIONS) {
			if (theIpsSectionContext.getResourceType().equals(ResourceType.Immunization.name())) {
				Immunization immunization = (Immunization) theCandidate;
				if (immunization.getMeta().getTag(GOLDEN_SYSTEM_TAG, GOLDEN_RECORD) == null) {
					return false;
				}
			}
		}
		return super.shouldInclude(theIpsSectionContext,theCandidate);
	}


}
