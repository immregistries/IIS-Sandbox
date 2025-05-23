package org.immregistries.iis.kernal.logic;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.model.ObservationReported;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Service
@Conditional(OnR4Condition.class)
public class Hl7MessageWriterR4 extends AbstractHl7MessageWriter {

	public void printStoredObservations(StringBuilder sb, PatientMaster patientMaster, VaccinationMaster vaccination, int obsSubId, int obxSetId) {
		IGenericClient fhirClient = repositoryClientFactory.getFhirClient();
		try {
			Bundle bundle = fhirClient.search().forResource(Observation.class)
				.where(Observation.PART_OF.hasId(patientMaster.getPatientId()))
				.and(Observation.PART_OF.hasId(vaccination.getVaccinationId()))
				.returnBundle(Bundle.class).execute();
			if (bundle.hasEntry()) {
				obsSubId++;
				for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
					ObservationReported observationReported =
						observationMapper.localObjectReported((Observation) entry.getResource());
					obxSetId++;
					printObx(sb, obxSetId, obsSubId, observationReported);
				}
			}
		} catch (ResourceNotFoundException ignored) {
		}
	}


}
