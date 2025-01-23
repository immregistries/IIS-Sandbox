package org.immregistries.iis.kernal.model;

import java.io.Serializable;

/**
 * Internal Standard agnostic representation of a patient's report or record, extending PatientMaster with Reference to the actual Master patient or Golden Record
 */
public class PatientReported extends PatientMaster implements Serializable {
	/**
	 * Master Patient reference or Golden Record
	 */
	PatientMaster patient;

	public PatientMaster getPatient() {
		return patient;
	}

	public void setPatient(PatientMaster patient) {
		this.patient = patient;
	}

}
