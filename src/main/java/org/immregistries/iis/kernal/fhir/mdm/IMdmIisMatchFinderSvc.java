package org.immregistries.iis.kernal.fhir.mdm;

import ca.uhn.fhir.mdm.api.MdmMatchOutcome;
import org.immregistries.mismo.match.PatientMatchResult;

public interface IMdmIisMatchFinderSvc {

	static MdmMatchOutcome mismoResultToMdmMatchOutcome(PatientMatchResult patientMatchResult) {
		switch (patientMatchResult.getDetermination()) {
			case MATCH: {
				return MdmMatchOutcome.EID_MATCH;
//				return MdmMatchOutcome.NEW_GOLDEN_RESOURCE_MATCH;
			}
			case POSSIBLE_MATCH: {
				return MdmMatchOutcome.POSSIBLE_MATCH;
			}
			case NO_MATCH: {
				return MdmMatchOutcome.NO_MATCH;
			}
			default: {
				return MdmMatchOutcome.NO_MATCH;
			}
		}
	}
}
