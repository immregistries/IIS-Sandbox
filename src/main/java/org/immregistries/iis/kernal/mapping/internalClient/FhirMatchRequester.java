package org.immregistries.iis.kernal.mapping.internalClient;

import org.immregistries.iis.kernal.model.IisPatient;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;

import java.util.Date;
import java.util.List;

public interface FhirMatchRequester {
	double MINIMAL_MATCHING_SCORE = 0.75;

	/**
	 * Fills multiple matched list and return Single Match
	 * Used for RSP
	 *
	 * @param multipleMatches      List to add multiple matches in
	 * @param patientForMatchQuery patient Information to match
	 * @param cutoff               cutoff date to ignore old records
	 * @return Single match result
	 */
	PatientMaster matchPatient(List<PatientReported> multipleMatches, IisPatient patientForMatchQuery,
										Date cutoff);
}
