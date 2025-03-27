package org.immregistries.iis.kernal.mapping.forR4;

import org.hl7.fhir.r4.model.ImmunizationEvaluation;
import org.hl7.fhir.r4.model.Reference;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.logic.CodeMapManager;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.vfa.connect.model.EvaluationActual;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Conditional(OnR4Condition.class)
public class ImmunizationEvaluationMapperR4 {

	@Autowired
	private ImmunizationMapperR4 immunizationMapperR4;

	public ImmunizationEvaluation toFhir(VaccinationMaster vaccinationMaster, Date date) {
		CodeMap codeMap = CodeMapManager.getCodeMap();
		ImmunizationEvaluation immunizationEvaluation = new ImmunizationEvaluation();
		if (vaccinationMaster.getPatientReported() != null) {
			immunizationEvaluation.setPatient(new Reference().setIdentifier(vaccinationMaster.getPatientReported().getMainBusinessIdentifier().toR4()));
		}
		immunizationEvaluation.setImmunizationEvent(new Reference("Immunization/" + vaccinationMaster.getVaccinationId()));

		if (vaccinationMaster.getTestEvent() != null) {

			immunizationEvaluation.setStatus(ImmunizationEvaluation.ImmunizationEvaluationStatus.COMPLETED);

			for (EvaluationActual evaluationActual : vaccinationMaster.getTestEvent().getEvaluationActualList()) {
				immunizationEvaluation.setSeries(evaluationActual.getSeriesUsedCode());
			}
		} else {
			immunizationEvaluation.setStatus(ImmunizationEvaluation.ImmunizationEvaluationStatus.ENTEREDINERROR);
		}
		return immunizationEvaluation;
	}
}
