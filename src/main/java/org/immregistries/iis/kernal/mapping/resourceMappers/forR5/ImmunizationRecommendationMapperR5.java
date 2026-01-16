package org.immregistries.iis.kernal.mapping.resourceMappers.forR5;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.ImmunizationRecommendation;
import org.hl7.fhir.r5.model.Reference;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.immregistries.iis.kernal.logic.CodeMapManagerService;
import org.immregistries.iis.kernal.logic.VaccinationRecommendationDateCode;
import org.immregistries.iis.kernal.logic.VaccinePlanStatus;
import org.immregistries.iis.kernal.mapping.fieldsMappers.BusinessIdentifierMapper;
import org.immregistries.iis.kernal.mapping.resourceMappers.IRecommendationMapper;
import org.immregistries.iis.kernal.model.IisPatient;
import org.immregistries.iis.kernal.model.IisRecommendation;
import org.immregistries.vfa.connect.model.ForecastActual;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

import static org.immregistries.iis.kernal.mapping.resourceMappers.ImmunizationMapper.CVX_SYSTEM;

@Service
@Conditional(OnR5Condition.class)
public class ImmunizationRecommendationMapperR5 extends IRecommendationMapper<ImmunizationRecommendation> implements IR5Mapper<IisRecommendation, ImmunizationRecommendation> {

	@Autowired
	private CodeMapManagerService codeMapManagerService;
	@Autowired
	private BusinessIdentifierMapper businessIdentifierMapper;

	public ImmunizationRecommendation fhirResource(IisRecommendation iisRecommendation) {
		ImmunizationRecommendation immunizationRecommendation = toFhir(iisRecommendation.getForecastActualList(),
				iisRecommendation.getDate(), iisRecommendation.getIisPatient());
		immunizationRecommendation.setId(iisRecommendation.getId());
		return immunizationRecommendation;
	}

	public IisRecommendation localObject(ImmunizationRecommendation immunizationRecommendation) {
		IisRecommendation iisRecommendation = new IisRecommendation();

		return iisRecommendation;
	}

	public ImmunizationRecommendation toFhir(List<ForecastActual> forecastActualList, Date date,
			IisPatient iisPatient) {
		ImmunizationRecommendation immunizationRecommendation = toFhir(forecastActualList, date);
		immunizationRecommendation.setPatient(new Reference()
				.setIdentifier(businessIdentifierMapper.toR5(iisPatient.getMainBusinessIdentifier())));
		return immunizationRecommendation;
	}

	public ImmunizationRecommendation toFhir(List<ForecastActual> forecastActualList, Date date) {
		if (forecastActualList == null) {
			return null;
		}
		CodeMap codeMap = codeMapManagerService.getCodeMap();
		ImmunizationRecommendation immunizationRecommendation = new ImmunizationRecommendation();
		immunizationRecommendation.setDate(date);
		for (ForecastActual forecastActual : forecastActualList) {
			ImmunizationRecommendation.ImmunizationRecommendationRecommendationComponent component = immunizationRecommendation
					.addRecommendation();
			/*
			 * CVX
			 */
			String cvx = forecastActual.getVaccineCvx();
			if (StringUtils.isBlank(cvx)) {
				cvx = forecastActual.getVaccineGroup().getVaccineCvx();
			}
			Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, cvx);
			if (cvxCode != null) {
				component.addVaccineCode(
						new CodeableConcept(new Coding(CVX_SYSTEM, cvxCode.getValue(), cvxCode.getLabel())));
			} else {
				component.addVaccineCode(new CodeableConcept(new Coding(CVX_SYSTEM, cvx, "")));
			}
			/*
			 * Vaccine Group
			 */
			Code vaccineGroup = codeMap.getCodeForCodeset(CodesetType.VACCINE_GROUP,
					forecastActual.getVaccineGroup().getVaccineCvx());
			if (vaccineGroup != null) {
				component.addTargetDisease(
						new CodeableConcept(new Coding("", vaccineGroup.getValue(), vaccineGroup.getLabel())));
			}
			// component.setContraindicatedVaccineCode();
			/*
			 * ForecastStatus
			 */
			component.setForecastStatus(
					new CodeableConcept().addCoding(VaccinePlanStatus.fromForecastActual(forecastActual).toR5()));
			/*
			 * ForecastReason
			 */
			// component.addForecastReason()

			/*
			 * Dates
			 */
			if (forecastActual.getValidDate() != null) {
				component
						.addDateCriterion()
						.setValue(forecastActual.getValidDate())
						.setCode(new CodeableConcept().addCoding(VaccinationRecommendationDateCode.EARLIEST.toR5()));
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
			if (forecastActual.getFinishedDate() != null) {
				component
						.addDateCriterion()
						.setValue(forecastActual.getFinishedDate())
						.setCode(new CodeableConcept().addCoding(VaccinationRecommendationDateCode.LATEST.toR5()));
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
			// component.setDoseNumber();
			/*
			 * Series Doses
			 */
			// component.setSeriesDoses();
			/*
			 * SupportingImmunization
			 */
			// component.setSupportingImmunization()
			/*
			 * SupportingPatientInformation(
			 */
			// component.setSupportingPatientInformation();

		}
		return immunizationRecommendation;
	}

}
