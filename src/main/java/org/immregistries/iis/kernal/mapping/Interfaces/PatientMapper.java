package org.immregistries.iis.kernal.mapping.Interfaces;

import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;

public interface PatientMapper<Patient> {

	public PatientReported getReportedWithMaster(Patient patient);
	public PatientReported getReported(Patient patient);
	public PatientMaster getMaster(Patient patient);
	public Patient getFhirResource(PatientReported pr);
}
