package org.immregistries.iis.kernal.logic.v2.writing;

import org.immregistries.iis.kernal.model.VaccinationReported;
import org.immregistries.iis.kernal.persisted.model.Tenant;

public interface IExampleMessageWriter {

	String buildVxu(VaccinationReported vaccinationReported, Tenant tenant);
}
