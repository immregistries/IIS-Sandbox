package org.immregistries.iis.kernal.mapping.forR5;

import org.hl7.fhir.r5.model.ImmunizationEvaluation;
import org.hl7.fhir.r5.model.Reference;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.iis.kernal.logic.CodeMapManager;
import org.immregistries.iis.kernal.model.VaccinationReported;
import org.immregistries.vfa.connect.model.EvaluationActual;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

public class ImmunizationEvaluationMapperR5 {

	@Autowired
	private ImmunizationMapperR5 immunizationMapperR5;

	public ImmunizationEvaluation toFhir(VaccinationReported vaccinationReported, Date date) {
		CodeMap codeMap = CodeMapManager.getCodeMap();
		ImmunizationEvaluation immunizationEvaluation = new ImmunizationEvaluation();
		if (vaccinationReported.getPatientReported() != null) {
			immunizationEvaluation.setPatient(new Reference().setIdentifier(vaccinationReported.getPatientReported().getMainBusinessIdentifier().toR5()));
		}
		immunizationEvaluation.setImmunizationEvent(new Reference("Immunization/" + vaccinationReported.getVaccinationId()));

		if (vaccinationReported.getTestEvent() != null) {

			immunizationEvaluation.setStatus(ImmunizationEvaluation.ImmunizationEvaluationStatusCodes.COMPLETED);

			for (EvaluationActual evaluationActual : vaccinationReported.getTestEvent().getEvaluationActualList()) {
				immunizationEvaluation.setSeries(evaluationActual.getSeriesUsedCode());
			}
		} else {
			immunizationEvaluation.setStatus(ImmunizationEvaluation.ImmunizationEvaluationStatusCodes.ENTEREDINERROR);
		}
		return immunizationEvaluation;
	}
}
