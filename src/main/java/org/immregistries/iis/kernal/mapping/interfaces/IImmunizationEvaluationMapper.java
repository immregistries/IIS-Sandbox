package org.immregistries.iis.kernal.mapping.interfaces;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.model.VaccinationMaster;

import java.util.Date;

public interface IImmunizationEvaluationMapper<ImmunizationEvaluation extends IBaseResource> {

	ImmunizationEvaluation toFhir(VaccinationMaster vaccinationMaster, Date date);

}
