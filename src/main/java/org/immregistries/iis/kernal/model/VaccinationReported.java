package org.immregistries.iis.kernal.model;

import java.io.Serializable;

/**
 * Created by Eric on 12/20/17.
 */
public class VaccinationReported extends VaccinationMaster implements Serializable {
	private static final long serialVersionUID = 1L;

	private VaccinationMaster vaccination = null;


	public VaccinationMaster getVaccination() {
		return vaccination;
	}

	public void setVaccination(VaccinationMaster vaccination) {
		this.vaccination = vaccination;
	}

}
