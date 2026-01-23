package org.immregistries.iis.kernal.mapping.mappers.resources;

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
		return IMMUNIZATION_EVALUATION_FHIR_TYPE_NAME;
	}

	public static final String IMMUNIZATION_EVALUATION_FHIR_TYPE_NAME = "ImmunizationEvaluation";

	public abstract ImmunizationEvaluation toFhir(IisVaccination iisVaccination, Date date);

}
