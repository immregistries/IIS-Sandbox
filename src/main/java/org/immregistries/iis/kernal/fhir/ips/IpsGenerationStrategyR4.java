package org.immregistries.iis.kernal.fhir.ips;

import ca.uhn.fhir.jpa.ips.jpa.DefaultJpaIpsGenerationStrategy;

public class IpsGenerationStrategyR4 extends DefaultJpaIpsGenerationStrategy implements ICustomIpsGenerationStrategy {
//
//	@Autowired
//	OrganizationMapperR4 organizationMapper;
//	@Autowired
//	RepositoryClientFactory repositoryClientFactory;
//
//	/**
//	 * Constructor
//	 */
//	public IpsGenerationStrategyR4() {
//		super();
//		this.setSectionRegistry(new SectionRegistryR4());
//	}
//
//	@Override
//	public IBaseResource createAuthor() {
//		Tenant tenant = ServletHelper.getTenant();
//		Organization organization = organizationMapper.getFhirResource(tenant);
//		return organization;
//	}
//
//	public IBaseBundle everything(IIdType theOriginalSubjectId, SectionRegistry.Section theSection) {
//		Parameters inParams = new Parameters();
//		inParams.addParameter("_mdm", true);
//		inParams.addParameter("type", StringUtils.join(theSection.getResourceTypes(),","));
//		Bundle bundle = repositoryClientFactory.getFhirClient().operation().onServer().named(JpaConstants.OPERATION_EVERYTHING).withParameters(inParams)
//			.returnResourceType(Bundle.class).execute();
//		return bundle;
//	}
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
