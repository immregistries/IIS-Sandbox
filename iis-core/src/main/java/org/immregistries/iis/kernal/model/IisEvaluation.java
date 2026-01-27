package org.immregistries.iis.kernal.model;

import java.util.Date;

public class IisEvaluation extends IisMappedToFhirResource {
	public IisVaccination iisVaccination;
	public Date date;

	public IisEvaluation(IisVaccination iisVaccination, Date date) {
		this.iisVaccination = iisVaccination;
		this.date = date;
	}

	public IisEvaluation() {
	}

	public IisVaccination getIisVaccination() {
		return iisVaccination;
	}

	public void setIisVaccination(IisVaccination iisVaccination) {
		this.iisVaccination = iisVaccination;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}
}
