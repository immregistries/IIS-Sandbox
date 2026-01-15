package org.immregistries.iis.kernal.mapping.resourceMappers;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.model.IisVaccination;

import java.util.Date;

public interface IImmunizationEvaluationMapper<ImmunizationEvaluation extends IAnyResource> {

	ImmunizationEvaluation toFhir(IisVaccination iisVaccination, Date date);

}
