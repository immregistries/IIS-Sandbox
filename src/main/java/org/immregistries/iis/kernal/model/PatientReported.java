package org.immregistries.iis.kernal.model;

import java.io.Serializable;

/**
 * Internal Standard agnostic representation of a patient's report or record, extending PatientMaster with Reference to the actual Master patient or Golden Record,
 * Used when dealing with a report
 */
public class PatientReported extends PatientMaster implements Serializable {
	/**
	 * Master Patient reference or Golden Record
	 */
	private PatientMaster patientMaster;

	public PatientMaster getPatientMaster() {
		return patientMaster;
	}

	public void setPatientMaster(PatientMaster patient) {
		this.patientMaster = patient;
	}

}
