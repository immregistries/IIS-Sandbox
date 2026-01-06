package org.immregistries.iis.kernal.mapping.resourceMappers.forR5;

import org.hl7.fhir.r5.model.ImmunizationEvaluation;
import org.hl7.fhir.r5.model.Reference;
import org.immregistries.iis.kernal.mapping.fieldsMappers.BusinessIdentifierMapper;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.immregistries.iis.kernal.logic.CodeMapManagerService;
import org.immregistries.iis.kernal.mapping.resourceMappers.IImmunizationEvaluationMapper;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.vfa.connect.model.EvaluationActual;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Not Complete
 */
@Service
@Conditional(OnR5Condition.class)
public class ImmunizationEvaluationMapperR5 implements IImmunizationEvaluationMapper<ImmunizationEvaluation> {

	@Autowired
	private ImmunizationMapperR5 immunizationMapperR5;
	@Autowired
	private CodeMapManagerService codeMapManagerService;

	public ImmunizationEvaluation toFhir(VaccinationMaster vaccinationMaster, Date date) {
		ImmunizationEvaluation immunizationEvaluation = new ImmunizationEvaluation();
		if (vaccinationMaster.getPatientReported() != null) {
			immunizationEvaluation.setPatient(new Reference().setIdentifier(
					BusinessIdentifierMapper.toR5(vaccinationMaster.getPatientReported().getMainBusinessIdentifier())));
		}
		immunizationEvaluation
				.setImmunizationEvent(new Reference("Immunization/" + vaccinationMaster.getVaccinationId()));

		if (vaccinationMaster.getTestEvent() != null
				&& vaccinationMaster.getTestEvent().getEvaluationActualList() != null) {

			immunizationEvaluation.setStatus(ImmunizationEvaluation.ImmunizationEvaluationStatusCodes.COMPLETED);

			for (EvaluationActual evaluationActual : vaccinationMaster.getTestEvent().getEvaluationActualList()) {
				immunizationEvaluation.setSeries(evaluationActual.getSeriesUsedCode());
			}
		} else {
			return null;
		}
		return immunizationEvaluation;
	}
}
