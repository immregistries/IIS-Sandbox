package org.immregistries.iis.kernal.logic;

import org.immregistries.iis.kernal.model.VaccinationReported;
import org.immregistries.iis.kernal.model.persisted.Tenant;

public interface IExampleMessageWriter {

	String buildVxu(VaccinationReported vaccinationReported, Tenant tenant);
}
