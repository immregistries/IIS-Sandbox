package org.immregistries.iis.kernal.logic;

import org.immregistries.iis.kernal.model.Tenant;
import org.immregistries.iis.kernal.model.VaccinationReported;

public interface IExampleMessageWriter {

	String buildVxu(VaccinationReported vaccinationReported, Tenant tenant);
}
