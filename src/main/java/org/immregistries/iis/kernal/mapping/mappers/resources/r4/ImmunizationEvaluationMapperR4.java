package org.immregistries.iis.kernal.mapping.mappers.resources.r4;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.logic.CodeMapManagerService;
import org.immregistries.iis.kernal.mapping.mappers.fields.BusinessIdentifierMapper;
import org.immregistries.iis.kernal.mapping.mappers.resources.ImmunizationEvaluationMapper;
import org.immregistries.iis.kernal.mapping.mappers.resources.ImmunizationMapper;
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
	private ImmunizationMapper<Immunization> immunizationMapper;
	@Autowired
	private BusinessIdentifierMapper<Identifier> businessIdentifierMapper;
	@Autowired
	private CodeMapManagerService codeMapManagerService;


	public IisEvaluation localObject(ImmunizationEvaluation immunizationEvaluation) {
		IisEvaluation iisEvaluation = new IisEvaluation();
		return iisEvaluation;
	}

	@Override
	public Class<ImmunizationEvaluation> fhirType() {
		return ImmunizationEvaluation.class;
	}

	public ImmunizationEvaluation fhirObject(IisEvaluation iisEvaluation) {
		IisVaccination iisVaccination = iisEvaluation.getIisVaccination();
		Date date = iisEvaluation.getDate();
		return toFhir(iisVaccination, date);
	}

	public ImmunizationEvaluation toFhir(IisVaccination iisVaccination, Date date) {
		CodeMap codeMap = codeMapManagerService.getCodeMap();
		ImmunizationEvaluation immunizationEvaluation = new ImmunizationEvaluation();
		if (iisVaccination.getPatientReported() != null) {
			immunizationEvaluation.setPatient(new Reference().setIdentifier(
				businessIdentifierMapper.fhirObject(iisVaccination.getPatientReported().getMainBusinessIdentifier())));
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
