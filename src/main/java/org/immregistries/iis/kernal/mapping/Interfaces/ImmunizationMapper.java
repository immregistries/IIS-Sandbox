package org.immregistries.iis.kernal.mapping.Interfaces;

import org.hl7.fhir.r4.model.Immunization;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.model.VaccinationReported;

public interface ImmunizationMapper<Immunization> {
	public VaccinationReported getReportedWithMaster(Immunization i);
	public VaccinationReported getReported(Immunization i);
	public VaccinationMaster getMaster(Immunization i);
	public Immunization getFhirResource(VaccinationReported vr);
}
