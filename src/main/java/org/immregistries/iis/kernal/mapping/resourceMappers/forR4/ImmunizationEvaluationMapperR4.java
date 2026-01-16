package org.immregistries.iis.kernal.mapping.resourceMappers.forR4;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ImmunizationEvaluation;
import org.hl7.fhir.r4.model.Reference;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.logic.CodeMapManagerService;
import org.immregistries.iis.kernal.mapping.fieldsMappers.forR4.BusinessIdentifierMapperR4;
import org.immregistries.iis.kernal.mapping.resourceMappers.ImmunizationEvaluationMapper;
import org.immregistries.iis.kernal.model.IisEvaluation;
import org.immregistries.iis.kernal.model.IisVaccination;
import org.immregistries.vfa.connect.model.EvaluationActual;
import org.immregistries.vfa.connect.model.TestEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Not complete
 */
@Service
@Conditional(OnR4Condition.class)
public class ImmunizationEvaluationMapperR4 extends ImmunizationEvaluationMapper<ImmunizationEvaluation> implements IR4Mapper<IisEvaluation, ImmunizationEvaluation> {

	@Autowired
	private ImmunizationMapperR4 immunizationMapperR4;
	@Autowired
	private CodeMapManagerService codeMapManagerService;
	@Autowired
	private BusinessIdentifierMapperR4 businessIdentifierMapper;

	public IisEvaluation localObject(ImmunizationEvaluation immunizationEvaluation) {
		IisEvaluation iisEvaluation = new IisEvaluation();
		return iisEvaluation;
	}

	public ImmunizationEvaluation fhirResource(IisEvaluation iisEvaluation) {
		IisVaccination iisVaccination = iisEvaluation.getIisVaccination();
		Date date = iisEvaluation.getDate();
		return toFhir(iisVaccination, date);
	}

	public ImmunizationEvaluation toFhir(IisVaccination iisVaccination, Date date) {
		CodeMap codeMap = codeMapManagerService.getCodeMap();
		ImmunizationEvaluation immunizationEvaluation = new ImmunizationEvaluation();
		if (iisVaccination.getPatientReported() != null) {
			immunizationEvaluation.setPatient(new Reference().setIdentifier(
				businessIdentifierMapper.toFhir(iisVaccination.getPatientReported().getMainBusinessIdentifier())));
		}
		immunizationEvaluation
			.setImmunizationEvent(new Reference("Immunization/" + iisVaccination.getVaccinationId()));

		if (iisVaccination.getTestEvent() != null
			&& iisVaccination.getTestEvent().getEvaluationActualList() != null) {
			TestEvent testEvent = iisVaccination.getTestEvent();
			immunizationEvaluation.setStatus(ImmunizationEvaluation.ImmunizationEvaluationStatus.COMPLETED);
			for (EvaluationActual evaluationActual : testEvent.getEvaluationActualList()) {
				immunizationEvaluation.setSeries(evaluationActual.getSeriesUsedCode());
				String cvx = evaluationActual.getVaccineCvx();
				if (StringUtils.isBlank(cvx)) {
					cvx = evaluationActual.getVaccineGroup().getVaccineCvx();
				}
				Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, cvx);
				immunizationEvaluation
						.setTargetDisease(new CodeableConcept().addCoding(new Coding("cvx", cvx, cvxCode.getLabel())));
			}
			// immunizationEvaluation.setDoseStatusReason()
			immunizationEvaluation.setDescription(testEvent.getLabelScreen());
		} else {
			return null;
		}
		return immunizationEvaluation;
	}
}
