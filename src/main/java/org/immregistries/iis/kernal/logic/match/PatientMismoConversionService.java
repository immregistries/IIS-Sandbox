package org.immregistries.iis.kernal.logic.match;

import ca.uhn.fhir.mdm.api.MdmMatchOutcome;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.mismo.match.PatientMatchResult;
import org.immregistries.mismo.match.model.Patient;

public interface PatientMismoConversionService<FhirPatient extends IAnyResource> {

	Patient convert(FhirPatient patient);

	Patient convertIncludingLink(FhirPatient patient);

	default MdmMatchOutcome mismoResultToMdmMatchOutcome(PatientMatchResult patientMatchResult) {
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
