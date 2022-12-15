package org.immregistries.iis.kernal.mapping.Interfaces;

import org.immregistries.iis.kernal.model.ObservationMaster;
import org.immregistries.iis.kernal.model.ObservationReported;

public interface ObservationMapper<Observation> {
	public ObservationReported getReported(Observation i);
	public ObservationMaster getMaster(Observation i);
	public Observation getFhirResource(ObservationReported vr);
}
