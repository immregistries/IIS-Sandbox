package org.immregistries.iis.kernal.mapping.forR4;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ImmunizationRecommendation;
import org.hl7.fhir.r4.model.Reference;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.logic.CodeMapManager;
import org.immregistries.iis.kernal.logic.VaccinationRecommendationDateCode;
import org.immregistries.iis.kernal.logic.VaccinePlanStatus;
import org.immregistries.iis.kernal.mapping.interfaces.IRecommendationMapper;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.vfa.connect.model.ForecastActual;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

import static org.immregistries.iis.kernal.mapping.interfaces.ImmunizationMapper.CVX_SYSTEM;

@Service
@Conditional(OnR4Condition.class)
public class RecommendationMapperR4 implements IRecommendationMapper {

	public ImmunizationRecommendation toFhir(List<ForecastActual> forecastActualList, Date date, PatientMaster patientMaster) {
		ImmunizationRecommendation immunizationRecommendation = toFhir(forecastActualList, date);
		immunizationRecommendation.setPatient(new Reference().setIdentifier(patientMaster.getMainBusinessIdentifier().toR4()));
		return immunizationRecommendation;
	}

	public ImmunizationRecommendation toFhir(List<ForecastActual> forecastActualList, Date date) {
		CodeMap codeMap = CodeMapManager.getCodeMap();
		ImmunizationRecommendation immunizationRecommendation = new ImmunizationRecommendation();
		immunizationRecommendation.setDate(date);
		for (ForecastActual forecastActual : forecastActualList) {
			ImmunizationRecommendation.ImmunizationRecommendationRecommendationComponent component = immunizationRecommendation.addRecommendation();
			/*
			 * CVX
			 */
			String cvx = forecastActual.getVaccineCvx();
			if (StringUtils.isBlank(cvx)) {
				cvx = forecastActual.getVaccineGroup().getVaccineCvx();
			}
			Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, cvx);
			if (cvxCode != null) {
				component.addVaccineCode(new CodeableConcept(new Coding(CVX_SYSTEM, cvxCode.getValue(), cvxCode.getLabel())));
			} else {
				component.addVaccineCode(new CodeableConcept(new Coding(CVX_SYSTEM, cvx, "")));
			}
			/*
			 * Vaccine Group
			 */
			Code vaccineGroup = codeMap.getCodeForCodeset(CodesetType.VACCINE_GROUP, forecastActual.getVaccineGroup().getVaccineCvx());
			if (vaccineGroup != null) {
				component.setTargetDisease(new CodeableConcept(new Coding("", vaccineGroup.getValue(), vaccineGroup.getLabel())));
			}
//			component.setContraindicatedVaccineCode();
			/*
			 * ForecastStatus
			 */
			component.setForecastStatus(new CodeableConcept().addCoding(VaccinePlanStatus.fromForecastActual(forecastActual).toR4()));
			/*
			 * ForecastReason
			 */
//			component.addForecastReason()

			/*
			 * Dates
			 */
			if (forecastActual.getFinishedDate() != null) {
				component
					.addDateCriterion()
					.setValue(forecastActual.getFinishedDate())
					.setCode(new CodeableConcept().addCoding(VaccinationRecommendationDateCode.LATEST.toR4()));
			}
			if (forecastActual.getDueDate() != null) {
				component
					.addDateCriterion()
					.setValue(forecastActual.getDueDate())
					.setCode(new CodeableConcept().addCoding(VaccinationRecommendationDateCode.DUE.toR4()));
			}
			if (forecastActual.getOverdueDate() != null) {
				component
					.addDateCriterion()
					.setValue(forecastActual.getOverdueDate())
					.setCode(new CodeableConcept().addCoding(VaccinationRecommendationDateCode.OVERDUE.toR4()));
			}
			if (forecastActual.getValidDate() != null) {
				component
					.addDateCriterion()
					.setValue(forecastActual.getValidDate())
					.setCode(new CodeableConcept().addCoding(VaccinationRecommendationDateCode.EARLIEST.toR4()));
			}
			/*
			 * Description
			 */
			component.setDescription(forecastActual.getExplanationHtml());
			/*
			 * Series
			 */
			component.setSeries(forecastActual.getScheduleName());
			/*
			 * Dose Number
			 */
//			component.setDoseNumber();
			/*
			 * Series Doses
			 */
//			component.setSeriesDoses();
			/*
			 * SupportingImmunization
			 */
//			component.setSupportingImmunization()
			/*
			 * SupportingPatientInformation(
			 */
//			component.setSupportingPatientInformation();

		}
		return immunizationRecommendation;
	}

}
