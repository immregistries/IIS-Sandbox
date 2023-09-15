package org.immregistries.iis.kernal.mapping.Interfaces;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.immregistries.iis.kernal.model.ObservationMaster;
import org.immregistries.iis.kernal.model.ObservationReported;
import org.immregistries.iis.kernal.InternalClient.FhirRequester;

public interface ObservationMapper<Observation> {
	public static final String IDENTIFIER_CODE = "identifierCode";
	public static final String OBSERVATION_DATE = "observationDate";
	public static final String RESULT_STATUS = "resultStatus";
	public ObservationReported getReported(Observation i);
	public ObservationMaster getMaster(Observation i);
	public Observation getFhirResource(ObservationReported vr);
	public ObservationReported getReportedWithMaster(Observation observation, IGenericClient fhirClient);
}
