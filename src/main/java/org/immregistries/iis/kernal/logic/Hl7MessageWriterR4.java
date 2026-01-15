package org.immregistries.iis.kernal.logic;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.immregistries.iis.kernal.model.IisPatient;
import org.immregistries.iis.kernal.model.IisVaccination;
import org.immregistries.iis.kernal.model.ObservationReported;

//@Service
//@Conditional(OnR4Condition.class)
@SuppressWarnings({"unchecked"})
/**
 * Retired
 */
public class Hl7MessageWriterR4 extends Hl7MessageWriter {

	public void printStoredObservations(StringBuilder sb, IisPatient iisPatient, IisVaccination iisVaccination, int obsSubId, int obxSetId) {
		IGenericClient fhirClient = iisFhirClientFactory.getOrCreateFhirClientFromContext();
		try {
			Bundle bundle = fhirClient.search().forResource(Observation.class)
				.where(Observation.PART_OF.hasId(iisPatient.getPatientId()))
				.and(Observation.PART_OF.hasId(iisVaccination.getVaccinationId()))
				.returnBundle(Bundle.class).execute();
			if (bundle.hasEntry()) {
				obsSubId++;
				for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
					ObservationReported observationReported = observationMapper.localObjectReported(entry.getResource());
					obxSetId++;
					printObx(sb, obxSetId, obsSubId, observationReported);
				}
			}
		} catch (ResourceNotFoundException ignored) {
		}
	}


}
