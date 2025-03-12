package org.immregistries.iis.kernal.mapping.forR5;

import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.ImmunizationRecommendation;
import org.hl7.fhir.r5.model.Reference;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
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
@Conditional(OnR5Condition.class)
public class RecommendationMapperR5 implements IRecommendationMapper {

	public ImmunizationRecommendation toFhir(List<ForecastActual> forecastActualList, Date date, PatientMaster patientMaster) {
		ImmunizationRecommendation immunizationRecommendation = toFhir(forecastActualList, date);
		immunizationRecommendation.setPatient(new Reference().setIdentifier(patientMaster.getMainBusinessIdentifier().toR5()));
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
			Code cvx = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, forecastActual.getVaccineCvx());
			if (cvx != null) {
				component.addVaccineCode(new CodeableConcept(new Coding(CVX_SYSTEM, cvx.getValue(), cvx.getLabel())));
			}
			/*
			 * Vaccine Group
			 */
			Code vaccineGroup = codeMap.getCodeForCodeset(CodesetType.VACCINE_GROUP, forecastActual.getVaccineGroup().getVaccineCvx());
			if (vaccineGroup != null) {
				component.addTargetDisease(new CodeableConcept(new Coding("", vaccineGroup.getValue(), vaccineGroup.getLabel())));
			}
//			component.setContraindicatedVaccineCode();
			/*
			 * ForecastStatus
			 */
			component.setForecastStatus(new CodeableConcept().addCoding(VaccinePlanStatus.fromForecastActual(forecastActual).toR5()));
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
					.setCode(new CodeableConcept().addCoding(VaccinationRecommendationDateCode.LATEST.toR5()));
			}
			if (forecastActual.getDueDate() != null) {
				component
					.addDateCriterion()
					.setValue(forecastActual.getDueDate())
					.setCode(new CodeableConcept().addCoding(VaccinationRecommendationDateCode.DUE.toR5()));
			}
			if (forecastActual.getOverdueDate() != null) {
				component
					.addDateCriterion()
					.setValue(forecastActual.getOverdueDate())
					.setCode(new CodeableConcept().addCoding(VaccinationRecommendationDateCode.OVERDUE.toR5()));
			}
			if (forecastActual.getValidDate() != null) {
				component
					.addDateCriterion()
					.setValue(forecastActual.getValidDate())
					.setCode(new CodeableConcept().addCoding(VaccinationRecommendationDateCode.EARLIEST.toR5()));
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
