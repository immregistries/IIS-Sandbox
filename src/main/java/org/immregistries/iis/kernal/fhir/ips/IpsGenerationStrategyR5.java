package org.immregistries.iis.kernal.fhir.ips;

import ca.uhn.fhir.jpa.ips.jpa.DefaultJpaIpsGenerationStrategy;
import ca.uhn.fhir.jpa.provider.BaseJpaResourceProviderPatient;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.Organization;
import org.hl7.fhir.r5.model.Patient;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.mapping.forR5.OrganizationMapperR5;
import org.immregistries.iis.kernal.mapping.internalClient.RepositoryClientFactory;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * prototype, unusable
 */
public class IpsGenerationStrategyR5 extends DefaultJpaIpsGenerationStrategy implements ICustomIpsGenerationStrategy {

	@Autowired
	OrganizationMapperR5 organizationMapper;
	@Autowired
	BaseJpaResourceProviderPatient<Patient> baseJpaResourceProviderPatient;
	@Autowired
	RepositoryClientFactory repositoryClientFactory;

	/**
	 * Constructor
	 */
	public IpsGenerationStrategyR5() {
		super();
//		this.setSectionRegistry(new SectionRegistryR5());
	}

	@Override
	public IBaseResource createAuthor() {
		Organization organization = organizationMapper.getFhirResource(ServletHelper.getTenant());
		return organization;
	}

//	public IBaseBundle everything(IIdType theOriginalSubjectId, SectionRegistry.Section theSection) {
//		Parameters inParams = new Parameters();
//		inParams.addParameter("_mdm", true);
//		inParams.addParameter("type", StringUtils.join(theSection.getResourceTypes(),","));
//		Bundle bundle = repositoryClientFactory.getFhirClient().operation().onServer().named(JpaConstants.OPERATION_EVERYTHING).withParameters(inParams)
//			.returnResourceType(Bundle.class).execute();
//		return bundle;
//	}
//
//
//	public List<IBaseResource> extractResourcesFromBundle(IpsContext.IpsSectionContext theIpsSectionContext, IBaseBundle iBaseBundle) {
//		Bundle bundle = (Bundle) iBaseBundle;
//		return bundle.getEntry().stream()
//			.filter((bundleEntryComponent -> bundleEntryComponent.hasResource() && theIpsSectionContext.getResourceType().equals(bundleEntryComponent.getResource().getResourceType().name())))
//			.map(Bundle.BundleEntryComponent::getResource).collect(Collectors.toList());
//	}
//
//	public String mdmLinksParameterIds(IIdType theOriginalSubjectId, SectionRegistry.Section theSection) {
//		Parameters inParams = new Parameters();
//		inParams.addParameter("resourceId", theOriginalSubjectId.getValue());
//		Bundle bundle = repositoryClientFactory.getFhirClient().operation().onServer().named("$mdm-query-links").withParameters(inParams)
//			.returnResourceType(Bundle.class).execute();
//		return bundle.getEntry().stream().map(bundleEntryComponent -> bundleEntryComponent.getResource().getId()).collect(Collectors.joining(","));
//	}
//
//	@Override
//	public boolean shouldInclude(IpsContext.IpsSectionContext theIpsSectionContext, IBaseResource theCandidate) {
//		if (Objects.requireNonNull(theIpsSectionContext.getSection()) == IpsSectionEnum.IMMUNIZATIONS) {
//			if (theIpsSectionContext.getResourceType().equals(ResourceType.Immunization.name())) {
//				Immunization immunization = (Immunization) theCandidate;
//				if (immunization.getMeta().getTag(GOLDEN_SYSTEM_TAG, GOLDEN_RECORD) == null) {
//					return false;
//				}
//			}
//		}
//		return super.shouldInclude(theIpsSectionContext,theCandidate);
//	}
}
