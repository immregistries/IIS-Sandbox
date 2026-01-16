package org.immregistries.iis.kernal.mapping.resourceMappers;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.model.IisEvaluation;
import org.immregistries.iis.kernal.model.IisVaccination;

import java.util.Date;

public abstract class ImmunizationEvaluationMapper<ImmunizationEvaluation extends IAnyResource>
		implements IisResourceMapper<IisEvaluation, ImmunizationEvaluation> {

	public Class<IisEvaluation> localType() {
		return IisEvaluation.class;
	}

	public String fhirTypeName() {
		return IMMUNIZATION_EVALUATION;
	}

	public static final String IMMUNIZATION_EVALUATION = "ImmunizationEvaluation";

	public abstract ImmunizationEvaluation toFhir(IisVaccination iisVaccination, Date date);

}
