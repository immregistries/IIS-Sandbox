package org.immregistries.iis.kernal.mapping.Interfaces;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.immregistries.iis.kernal.model.ObservationMaster;
import org.immregistries.iis.kernal.model.ObservationReported;
import org.immregistries.iis.kernal.repository.FhirRequester;

public interface ObservationMapper<Observation> {
	public ObservationReported getReported(Observation i);
	public ObservationMaster getMaster(Observation i);
	public Observation getFhirResource(ObservationReported vr);
	public ObservationReported getReportedWithMaster(Observation observation, FhirRequester fhirRequests, IGenericClient fhirClient);
}
