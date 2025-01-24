package org.immregistries.iis.kernal.model;

import java.io.Serializable;

public class ObservationReported extends ObservationMaster implements Serializable {
	private ObservationMaster observationMaster;

	public ObservationMaster getObservationMaster() {
		return observationMaster;
	}

	public void setObservationMaster(ObservationMaster observationMaster) {
		this.observationMaster = observationMaster;
	}
}
