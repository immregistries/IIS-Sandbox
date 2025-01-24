package org.immregistries.iis.kernal.mapping.Interfaces;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.model.ObservationMaster;
import org.immregistries.iis.kernal.model.ObservationReported;

public interface ObservationMapper<Observation extends IBaseResource> extends IisFhirMapper<ObservationMaster, ObservationReported, Observation> {
	String IDENTIFIER_CODE = "identifierCode";
	String OBSERVATION_DATE = "observationDate";
	String RESULT_STATUS = "resultStatus";

	ObservationReported getReported(Observation i);

	ObservationMaster getMaster(Observation i);

	Observation getFhirResource(ObservationReported vr);

	ObservationReported getReportedWithMaster(Observation observation, IGenericClient fhirClient);
}
