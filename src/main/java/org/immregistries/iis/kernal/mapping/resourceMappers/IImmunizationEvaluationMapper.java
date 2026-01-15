package org.immregistries.iis.kernal.mapping.resourceMappers;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.model.IisVaccination;

import java.util.Date;

public interface IImmunizationEvaluationMapper<ImmunizationEvaluation extends IBaseResource> {

	ImmunizationEvaluation toFhir(IisVaccination iisVaccination, Date date);

}
