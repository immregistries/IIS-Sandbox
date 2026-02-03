package ca.uhn.fhir.jpa.starter.ips;

import ca.uhn.fhir.jpa.ips.api.IpsSectionContext;
import ca.uhn.fhir.jpa.ips.api.Section;
import ca.uhn.fhir.jpa.ips.jpa.DefaultJpaIpsGenerationStrategy;
import ca.uhn.fhir.jpa.ips.jpa.JpaSectionSearchStrategyCollection;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.jpa.provider.BaseJpaResourceProviderPatient;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.*;
import org.immregistries.iis.kernal.mapping.IisFhirClientFactory;
import org.immregistries.iis.kernal.mapping.mappers.resources.r4.OrganizationMapperR4;
import org.immregistries.iis.kernal.security.CurrentTenantUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;


public class IpsGenerationStrategyR4 extends DefaultJpaIpsGenerationStrategy implements ICustomIpsGenerationStrategy {

	@Autowired
	OrganizationMapperR4 organizationMapper;
	@Autowired
	BaseJpaResourceProviderPatient<Patient> baseJpaResourceProviderPatient;
	@Autowired
	IisFhirClientFactory iisFhirClientFactory;

	/**
	 * Constructor
	 */
	public IpsGenerationStrategyR4() {
		super();
	}

	@Override
	protected void addJpaSectionImmunizations() {
		Section section = Section.newBuilder()
			.withTitle("History of Immunizations")
			.withSectionSystem(SECTION_SYSTEM_LOINC)
			.withSectionCode(SECTION_CODE_IMMUNIZATIONS)
			.withSectionDisplay("History of Immunization Narrative")
			.withResourceType(Immunization.class)
			.withProfile(
				"https://hl7.org/fhir/uv/ips/StructureDefinition-Composition-uv-ips-definitions.html#Composition.section:sectionImmunizations")
			.build();

		JpaSectionSearchStrategyCollection searchStrategyCollection = JpaSectionSearchStrategyCollection.newBuilder()
			.addStrategy(Immunization.class, new ImmunizationsJpaSectionSearchStrategyR4())
			.build();

		addJpaSection(section, searchStrategyCollection);
	}

	@Override
	protected void addSections() {
		addJpaSectionAllergyIntolerance();
		addJpaSectionMedicationSummary();
		addJpaSectionProblemList();
		addJpaSectionImmunizations();
//		addJpaSectionProcedures();
//		addJpaSectionMedicalDevices();
//		addJpaSectionDiagnosticResults();
//		addJpaSectionVitalSigns();
//		addJpaSectionPregnancy();
//		addJpaSectionSocialHistory();
//		addJpaSectionIllnessHistory();
//		addJpaSectionFunctionalStatus();
//		addJpaSectionPlanOfCare();
//		addJpaSectionAdvanceDirectives();
	}

	@Override
	public IAnyResource createAuthor() {
		Organization organization = organizationMapper.fhirObject(CurrentTenantUtil.getTenant());
		return organization;
	}

	public IBaseBundle everything(IIdType theOriginalSubjectId, Section theSection) {
		Parameters inParams = new Parameters();
		inParams.addParameter("_mdm", true);
		inParams.addParameter("type", StringUtils.join(theSection.getResourceTypes(), ","));
		Bundle bundle = iisFhirClientFactory.getOrCreateFhirClientFromContext().operation().onServer().named(JpaConstants.OPERATION_EVERYTHING).withParameters(inParams)
			.returnResourceType(Bundle.class).execute();
		return bundle;
	}


	public List<IAnyResource> extractResourcesFromBundle(IpsSectionContext theIpsSectionContext, IBaseBundle iBaseBundle) {
		Bundle bundle = (Bundle) iBaseBundle;
		return bundle.getEntry().stream()
			.filter((bundleEntryComponent -> bundleEntryComponent.hasResource() && theIpsSectionContext.getResourceType().equals(bundleEntryComponent.getResource().getResourceType().name())))
			.map(Bundle.BundleEntryComponent::getResource).collect(Collectors.toList());
	}

	public String mdmLinksParameterIds(IIdType theOriginalSubjectId, Section theSection) {
		Parameters inParams = new Parameters();
		inParams.addParameter("resourceId", theOriginalSubjectId.getValue());
		Bundle bundle = iisFhirClientFactory.getOrCreateFhirClientFromContext().operation().onServer().named($_MDM_QUERY_LINKS).withParameters(inParams)
			.returnResourceType(Bundle.class).execute();
		return bundle.getEntry().stream().map(bundleEntryComponent -> bundleEntryComponent.getResource().getId()).collect(Collectors.joining(","));
	}


}
