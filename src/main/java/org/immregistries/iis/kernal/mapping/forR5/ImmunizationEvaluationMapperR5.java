package org.immregistries.iis.kernal.mapping.forR5;

import org.hl7.fhir.r5.model.ImmunizationEvaluation;
import org.hl7.fhir.r5.model.Reference;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.immregistries.iis.kernal.logic.CodeMapManager;
import org.immregistries.iis.kernal.mapping.interfaces.IImmunizationEvaluationMapper;
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

	public ImmunizationEvaluation toFhir(VaccinationMaster vaccinationMaster, Date date) {
		CodeMap codeMap = CodeMapManager.getCodeMap();
		ImmunizationEvaluation immunizationEvaluation = new ImmunizationEvaluation();
		if (vaccinationMaster.getPatientReported() != null) {
			immunizationEvaluation.setPatient(new Reference().setIdentifier(vaccinationMaster.getPatientReported().getMainBusinessIdentifier().toR5()));
		}
		immunizationEvaluation.setImmunizationEvent(new Reference("Immunization/" + vaccinationMaster.getVaccinationId()));

		if (vaccinationMaster.getTestEvent() != null && vaccinationMaster.getTestEvent().getEvaluationActualList() != null) {

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
