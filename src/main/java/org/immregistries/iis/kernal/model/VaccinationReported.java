package org.immregistries.iis.kernal.model;

import java.io.Serializable;

/**
 * Internal Standard agnostic representation of a record, extending VaccinationMaster with Reference to the actual Master patient or Golden Record,
 * Used when dealing with a report
 */
public class VaccinationReported extends IisVaccination implements Serializable, IReportedObject<VaccinationMaster> {
	/**
	 * Master Immunization reference or Golden Record
	 */
	private VaccinationMaster vaccinationMaster = null;

	public VaccinationMaster getMasterRecord() {
		return vaccinationMaster;
	}

	public void setMasterRecord(VaccinationMaster vaccination) {
		this.vaccinationMaster = vaccination;
	}

}
