package org.immregistries.iis.kernal.fhir.mdm;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.mdm.svc.MdmResourceFilteringSvc;
import ca.uhn.fhir.mdm.api.IMdmSettings;
import ca.uhn.fhir.mdm.log.Logs;
import ca.uhn.fhir.mdm.rules.json.MdmResourceSearchParamJson;
import ca.uhn.fhir.mdm.svc.MdmSearchParamSvc;
import ca.uhn.fhir.mdm.util.MdmResourceUtil;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Not always active, used for testing
 */
public class MdmCustomResourceFilteringSvc extends MdmResourceFilteringSvc {
	private static final Logger ourLog = Logs.getMdmTroubleshootingLog();

	@Autowired
	private IMdmSettings myMdmSettings;

	@Autowired
	MdmSearchParamSvc myMdmSearchParamSvc;

	@Autowired
	FhirContext myFhirContext;

	public MdmCustomResourceFilteringSvc() {
		super();
	}


	/**
	 * overrides to add extra logs
	 *
	 * @return whether MDM processing should proceed
	 */
	@Override
	public boolean shouldBeProcessed(IAnyResource theResource) {
		ourLog.info("Is {} suitable for MDM processing? TEST", theResource.getId());

		if (MdmResourceUtil.isMdmManaged(theResource)) {
			ourLog.info("MDM Message handler is dropping [{}] as it is MDM-managed.", theResource.getId());
			return false;
		}

		String resourceType = myFhirContext.getResourceType(theResource);
		List<MdmResourceSearchParamJson> candidateSearchParams =
			myMdmSettings.getMdmRules().getCandidateSearchParams();

		if (candidateSearchParams.isEmpty()) {
			return true;
		}

		boolean containsValueForSomeSearchParam = candidateSearchParams.stream()
			.filter(csp ->
				myMdmSearchParamSvc.searchParamTypeIsValidForResourceType(csp.getResourceType(), resourceType))
			.flatMap(csp -> csp.getSearchParams().stream())
			.map(searchParam -> myMdmSearchParamSvc.getValueFromResourceForSearchParam(theResource, searchParam))
			.anyMatch(valueList -> !valueList.isEmpty());

		ourLog.info("Is {} suitable for MDM processing? : {}", theResource.getId(), containsValueForSomeSearchParam);
		return containsValueForSomeSearchParam;
	}
}
