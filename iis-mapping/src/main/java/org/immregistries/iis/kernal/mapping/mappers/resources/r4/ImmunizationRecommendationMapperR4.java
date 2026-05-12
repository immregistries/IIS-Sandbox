package org.immregistries.iis.kernal.mapping.mappers.resources.r4;

import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ImmunizationRecommendation;
import org.hl7.fhir.r4.model.Reference;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.enums.VaccinationRecommendationDateCode;
import org.immregistries.iis.kernal.enums.VaccinePlanStatus;
import org.immregistries.iis.kernal.mapping.mappers.fields.r4.BusinessIdentifierMapperR4;
import org.immregistries.iis.kernal.mapping.mappers.resources.RecommendationMapper;
import org.immregistries.iis.kernal.model.IisRecommendation;
import org.immregistries.iis.kernal.services.CodeMapManagerService;
import org.immregistries.vfa.connect.model.ForecastActual;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.immregistries.iis.kernal.mapping.mappers.resources.ImmunizationMapper.CVX_SYSTEM;

@Service
@Conditional(OnR4Condition.class)
public class ImmunizationRecommendationMapperR4 extends RecommendationMapper<ImmunizationRecommendation, ImmunizationRecommendation.ImmunizationRecommendationRecommendationComponent> implements IR4ResourceMapper<IisRecommendation, ImmunizationRecommendation> {
	@Autowired
	private CodeMapManagerService codeMapManagerService;
	@Autowired
	private BusinessIdentifierMapperR4 businessIdentifierMapper;

	public IisRecommendation localObject(ImmunizationRecommendation immunizationRecommendation) {
		IisRecommendation iisRecommendation = new IisRecommendation();
		return iisRecommendation;
	}

	@Override
	public Class<ImmunizationRecommendation> fhirType() {
		return ImmunizationRecommendation.class;
	}

	public ImmunizationRecommendation fhirObject(IisRecommendation iisRecommendation) {
		ImmunizationRecommendation immunizationRecommendation = toFhir(iisRecommendation.getForecastActualList(), iisRecommendation.getDate());
		if (iisRecommendation.getIisPatient() != null) {
			immunizationRecommendation.setPatient(new Reference()
				.setIdentifier(businessIdentifierMapper.fhirObject(iisRecommendation.getIisPatient().getMainBusinessIdentifier())));
		}
		immunizationRecommendation.setId(iisRecommendation.getId());
		immunizationRecommendation.setAuthority(new Reference().setIdentifier(businessIdentifierMapper.fhirObject(iisRecommendation.getAuthority())));
		immunizationRecommendation.setIdentifier(iisRecommendation.getBusinessIdentifierList().stream().map(businessIdentifier -> businessIdentifierMapper.fhirObject(businessIdentifier)).collect(Collectors.toList()));
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
			ImmunizationRecommendation.ImmunizationRecommendationRecommendationComponent component = recommendationComponent(forecastActual, codeMap);
			immunizationRecommendation.addRecommendation(component);
		}
		return immunizationRecommendation;
	}

	public ImmunizationRecommendation.@NotNull ImmunizationRecommendationRecommendationComponent recommendationComponent(ForecastActual forecastActual) {
		CodeMap codeMap = codeMapManagerService.getCodeMap();
		return recommendationComponent(forecastActual, codeMap);
	}

	public ImmunizationRecommendation.@NotNull ImmunizationRecommendationRecommendationComponent recommendationComponent(ForecastActual forecastActual, CodeMap codeMap) {
		ImmunizationRecommendation.ImmunizationRecommendationRecommendationComponent component = new ImmunizationRecommendation.ImmunizationRecommendationRecommendationComponent();
		component.setSeries(forecastActual.getScheduleName());
		component.setDescription(forecastActual.getExplanationHtml());
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
			component.setTargetDisease(
				new CodeableConcept(new Coding("", vaccineGroup.getValue(), vaccineGroup.getLabel())));
		}
		// component.setContraindicatedVaccineCode();
		/*
		 * ForecastStatus
		 */
		VaccinePlanStatus vaccinePlanStatus = VaccinePlanStatus.fromForecastActual(forecastActual);
		if (vaccinePlanStatus != null) {
			component.setForecastStatus(
				new CodeableConcept().addCoding(vaccinePlanStatus.toR4()));
		}

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
				.setCode(new CodeableConcept().addCoding(VaccinationRecommendationDateCode.EARLIEST.toR4()));
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
		if (forecastActual.getFinishedDate() != null) {
			component
				.addDateCriterion()
				.setValue(forecastActual.getFinishedDate())
				.setCode(new CodeableConcept().addCoding(VaccinationRecommendationDateCode.LATEST.toR4()));
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
		return component;
	}

}
