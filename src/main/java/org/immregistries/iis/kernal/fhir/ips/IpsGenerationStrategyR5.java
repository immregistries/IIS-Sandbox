package org.immregistries.iis.kernal.fhir.ips;

import ca.uhn.fhir.jpa.ips.api.IpsContext;
import ca.uhn.fhir.jpa.ips.api.SectionRegistry;
import ca.uhn.fhir.jpa.ips.strategy.DefaultIpsGenerationStrategy;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.model.api.Include;
import com.google.common.collect.Sets;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r5.model.*;

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
	/**
	 * Constructor
	 */
	public IpsGenerationStrategyR5() {
		super();
	}

	@Override
	public IBaseResource createAuthor() {
		Organization organization = new Organization();
		organization
			.setName("eHealthLab - University of Cyprus")
			.setId(IdType.newRandomUuid());
		organization.addContact().setAddress(new Address()
				.addLine("1 University Avenue")
				.setCity("Nicosia")
				.setPostalCode("2109")
				.setCountry("CY"));
		return organization;
	}

	@Override
	public String createTitle(IpsContext theContext) {
		return "Patient Summary as of "
			+ DateTimeFormatter.ofPattern("MM/dd/yyyy").format(LocalDate.now());
	}

	@Override
	public IIdType massageResourceId(@Nullable IpsContext theIpsContext, @Nonnull IBaseResource theResource) {
		return IdType.newRandomUuid();
	}

	@Override
	public void massageResourceSearch(
		IpsContext.IpsSectionContext theIpsSectionContext, SearchParameterMap theSearchParameterMap) {
		switch (theIpsSectionContext.getSection()) {
			case ALLERGY_INTOLERANCE:
			case PROBLEM_LIST:
			case IMMUNIZATIONS:
			case PROCEDURES:
			case MEDICAL_DEVICES:
			case ILLNESS_HISTORY:
			case FUNCTIONAL_STATUS:
			default:
				return;

		}
		// Shouldn't happen: This means none of the above switches handled the Section+resourceType combination
//		assert false
//			: "Don't know how to handle " + theIpsSectionContext.getSection() + "/"
//			+ theIpsSectionContext.getResourceType();
	}

	@Nonnull
	@Override
	public Set<Include> provideResourceSearchIncludes(IpsContext.IpsSectionContext theIpsSectionContext) {
		switch (theIpsSectionContext.getSection()) {
			case MEDICATION_SUMMARY:
				if (ResourceType.MedicationStatement.name().equals(theIpsSectionContext.getResourceType())) {
					return Sets.newHashSet(MedicationStatement.INCLUDE_MEDICATION);
				}
				if (ResourceType.MedicationRequest.name().equals(theIpsSectionContext.getResourceType())) {
					return Sets.newHashSet(MedicationRequest.INCLUDE_MEDICATION);
				}
				if (ResourceType.MedicationAdministration.name().equals(theIpsSectionContext.getResourceType())) {
					return Sets.newHashSet(MedicationAdministration.INCLUDE_MEDICATION);
				}
				if (ResourceType.MedicationDispense.name().equals(theIpsSectionContext.getResourceType())) {
					return Sets.newHashSet(MedicationDispense.INCLUDE_MEDICATION);
				}
				break;
			case MEDICAL_DEVICES:
			case ALLERGY_INTOLERANCE:
			case PROBLEM_LIST:
			case IMMUNIZATIONS:
			case PROCEDURES:
			case DIAGNOSTIC_RESULTS:
			case VITAL_SIGNS:
			case ILLNESS_HISTORY:
			case PREGNANCY:
			case SOCIAL_HISTORY:
			case FUNCTIONAL_STATUS:
			case PLAN_OF_CARE:
			case ADVANCE_DIRECTIVES:
				break;
		}
		return Collections.emptySet();
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
