package org.immregistries.iis.kernal.mapping.resourceMappers;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.model.IisEvaluation;
import org.immregistries.iis.kernal.model.IisVaccination;

import java.util.Date;

public interface IImmunizationEvaluationMapper<ImmunizationEvaluation extends IAnyResource> extends IisResourceMasterMapper<IisEvaluation, ImmunizationEvaluation> {

	default Class<IisEvaluation> localType() {
		return IisEvaluation.class;
	}

	default String fhirType() {
		return IMMUNIZATION_EVALUATION;
	}

	String IMMUNIZATION_EVALUATION = "ImmunizationEvaluation";

	ImmunizationEvaluation toFhir(IisVaccination iisVaccination, Date date);

}
