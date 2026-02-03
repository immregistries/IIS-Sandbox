package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.jpa.ips.api.IpsSectionContext;
import ca.uhn.fhir.jpa.ips.jpa.JpaSectionSearchStrategy;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.SortOrderEnum;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.ReferenceParam;
import jakarta.annotation.Nonnull;
import org.hl7.fhir.r4.model.Immunization;

import static org.immregistries.iis.kernal.mapping.requesters.FhirSaveRequester.GOLDEN_RECORD;
import static org.immregistries.iis.kernal.mapping.requesters.FhirSaveRequester.GOLDEN_SYSTEM_TAG;

/**
 * Retains only golden record
 */
public class ImmunizationsJpaSectionSearchStrategyR4 extends JpaSectionSearchStrategy<Immunization> {

	@Override
	public void massageResourceSearch(
		@Nonnull IpsSectionContext<Immunization> theIpsSectionContext,
		@Nonnull SearchParameterMap theSearchParameterMap) {
		theSearchParameterMap.setSort(new SortSpec(Immunization.SP_DATE).setOrder(SortOrderEnum.DESC));
		theSearchParameterMap.addInclude(Immunization.INCLUDE_MANUFACTURER);
		ReferenceParam referenceParam = (ReferenceParam) theSearchParameterMap.get("patient").get(0).get(0);
		referenceParam.setMdmExpand(true);
	}

	@SuppressWarnings("RedundantIfStatement")
	@Override
	public boolean shouldInclude(
		@Nonnull IpsSectionContext<Immunization> theIpsSectionContext, @Nonnull Immunization theCandidate) {
		if (theCandidate.getStatus() == Immunization.ImmunizationStatus.ENTEREDINERROR) {
			return false;
		}
		if (theCandidate.getMeta().getTag(GOLDEN_SYSTEM_TAG, GOLDEN_RECORD) == null) {
			return false;
		}

		return true;
	}

}
