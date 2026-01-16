package org.immregistries.iis.kernal.mapping.resourceMappers.forR5;

import org.hl7.fhir.r5.model.ImmunizationEvaluation;
import org.hl7.fhir.r5.model.Reference;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.immregistries.iis.kernal.mapping.fieldsMappers.BusinessIdentifierMapper;
import org.immregistries.iis.kernal.mapping.resourceMappers.ImmunizationEvaluationMapper;
import org.immregistries.iis.kernal.model.IisEvaluation;
import org.immregistries.iis.kernal.model.IisVaccination;
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
public class ImmunizationEvaluationMapperR5 extends ImmunizationEvaluationMapper<ImmunizationEvaluation> implements IR5Mapper<IisEvaluation, ImmunizationEvaluation>{

	@Autowired
	private ImmunizationMapperR5 immunizationMapperR5;

	@Autowired
	private BusinessIdentifierMapper businessIdentifierMapper;

	public IisEvaluation localObject(ImmunizationEvaluation immunizationEvaluation) {
		IisEvaluation iisEvaluation = new IisEvaluation();
		return iisEvaluation;
	}

	public ImmunizationEvaluation fhirResource(IisEvaluation iisEvaluation) {
		IisVaccination iisVaccination = iisEvaluation.getIisVaccination();
		Date date = iisEvaluation.getDate();
		return toFhir(iisVaccination, date);
	}

	public ImmunizationEvaluation toFhir(IisVaccination vaccinationMaster, Date date) {
		ImmunizationEvaluation immunizationEvaluation = new ImmunizationEvaluation();
		if (vaccinationMaster.getPatientReported() != null) {
			immunizationEvaluation.setPatient(new Reference().setIdentifier(
					businessIdentifierMapper.toR5(vaccinationMaster.getPatientReported().getMainBusinessIdentifier())));
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
