package org.immregistries.iis.kernal.logic.validation;

import org.apache.commons.lang3.StringUtils;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.enums.ProcessingFlavor;
import org.immregistries.iis.kernal.logic.hl7v2.ack.IisReportableUtilService;
import org.immregistries.iis.kernal.mapping.mappers.resources.ImmunizationMapper;
import org.immregistries.iis.kernal.model.VaccinationReported;
import org.immregistries.iis.kernal.model.ack.IisReportable;
import org.immregistries.iis.kernal.model.ack.IisReportableSeverityLevel;
import org.immregistries.iis.kernal.services.CodeMapManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Service
public class ImmunizationValidator extends IisValidator{
	@Autowired
	private ImmunizationMapper immunizationMapper;

	@Autowired
	private IisReportableUtilService iisReportableUtilService;
	@Autowired
	private CodeMapManagerService codeMapManagerService;

	private Random random = new Random();

	public VaccinationReported processAndValidateVaccinationReported(VaccinationReported vaccinationReported, List<IisReportable> iisReportableList, Set<ProcessingFlavor> processingFlavorSet, int fundingSourceObxCount, int fundingEligibilityObxCount, int rxaCount, String vaccineCptCode) throws ProcessingException {
		testMapping(immunizationMapper, vaccinationReported);
		CodeMap codeMap = codeMapManagerService.getCodeMap();

		Date administrationDate = vaccinationReported.getAdministeredDate();
		if (administrationDate.after(new Date()) && !processingFlavorSet.contains(ProcessingFlavor.MANDARINE)) {
			throw new ProcessingException("Vaccination is indicated as occurring in the future, unable to accept future vaccination events", "RXA", rxaCount, 3);
		}


		vaccinationReported = processNdcAndCvx(vaccinationReported, iisReportableList, processingFlavorSet, rxaCount, vaccineCptCode);

		if (StringUtils.isNotBlank(vaccinationReported.getRefusalReasonCode())) {
			Code refusalCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_REFUSAL, vaccinationReported.getRefusalReasonCode());
			if (refusalCode == null) {
				ProcessingException pe = new ProcessingException("Unrecognized refusal reason", "RXA", rxaCount, 18);
				pe.setErrorCode(IisReportableSeverityLevel.WARN);
				iisReportableList.add(iisReportableUtilService.fromProcessingException(pe));
			}
		}


		if (processingFlavorSet.contains(ProcessingFlavor.GRAPEFRUIT) && random.nextBoolean()) {
			throw new ProcessingException("Vaccination randomly rejected, Patient Accepted", "RXR", 0, 0, IisReportableSeverityLevel.NOTICE);
		}


		String fundingEligibility = vaccinationReported.getFundingEligibility();
		if (!fundingEligibility.isEmpty()) {
			Code fundingEligibilityCode = codeMap.getCodeForCodeset(CodesetType.FINANCIAL_STATUS_CODE, fundingEligibility);
			if (fundingEligibilityCode == null) {
				ProcessingException pe = new ProcessingException("Funding eligibility '" + fundingEligibility + "' was not recognized", "OBX", fundingEligibilityObxCount, 5, IisReportableSeverityLevel.WARN);
				iisReportableList.add(iisReportableUtilService.fromProcessingException(pe));
				vaccinationReported.setFundingEligibility("");
			}
		}

		String fundingSource = vaccinationReported.getFundingSource();
		if (!fundingSource.isEmpty()) {
			Code fundingSourceCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_FUNDING_SOURCE, fundingSource);
			if (fundingSourceCode == null) {
				ProcessingException pe = new ProcessingException("Funding source '" + fundingSource + "' was not recognized", "OBX", fundingSourceObxCount, 5, IisReportableSeverityLevel.WARN);
				iisReportableList.add(iisReportableUtilService.fromProcessingException(pe));
				vaccinationReported.setFundingSource("");
			}
		}
		return vaccinationReported;
	}

	private VaccinationReported processNdcAndCvx(VaccinationReported vaccinationReported, List<IisReportable> iisReportableList, Set<ProcessingFlavor> processingFlavorSet, int rxaCount, String vaccineCptCode) throws ProcessingException {
		CodeMap codeMap = codeMapManagerService.getCodeMap();
		String vaccineNdcCode = vaccinationReported.getVaccineNdcCode();
		String vaccineCvxCode = vaccinationReported.getVaccineCvxCode();
		{
			Code ndcCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_NDC_CODE, vaccineNdcCode);
			if (ndcCode != null) {
				if (ndcCode.getCodeStatus() != null && ndcCode.getCodeStatus().getDeprecated() != null && ndcCode.getCodeStatus().getDeprecated().getNewCodeValue() != null && !ndcCode.getCodeStatus().getDeprecated().getNewCodeValue().equals("")) {
					vaccineNdcCode = ndcCode.getCodeStatus().getDeprecated().getNewCodeValue();
				}
				Code cvxCode = codeMap.getRelatedCode(ndcCode, CodesetType.VACCINATION_CVX_CODE);
				if (cvxCode == null) {
					ProcessingException pe = new ProcessingException("Unrecognized NDC " + vaccineNdcCode, "RXA", rxaCount, 5, IisReportableSeverityLevel.WARN);
					iisReportableList.add(iisReportableUtilService.fromProcessingException(pe));
				} else {
					if (StringUtils.isBlank(vaccineCvxCode)) {
						vaccineCvxCode = cvxCode.getValue();
					} else if (!vaccineCvxCode.equals(cvxCode.getValue())) {
						// NDC doesn't map to the CVX code that was submitted!
						ProcessingException pe = new ProcessingException("NDC " + vaccineNdcCode + " maps to " + cvxCode.getValue() + " but CVX " + vaccineCvxCode + " was also reported, preferring CVX code", "RXA", rxaCount, 5, IisReportableSeverityLevel.WARN);
						iisReportableList.add(iisReportableUtilService.fromProcessingException(pe));
					}
				}
			}
		}
		{
			Code cptCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CPT_CODE, vaccineCptCode);
			if (cptCode != null) {
				Code cvxCode = codeMap.getRelatedCode(cptCode, CodesetType.VACCINATION_CVX_CODE);
				if (cvxCode == null) {
					ProcessingException pe = new ProcessingException("Unrecognized CPT " + cptCode, "RXA", rxaCount, 5, IisReportableSeverityLevel.WARN);
					iisReportableList.add(iisReportableUtilService.fromProcessingException(pe));
				} else {
					if (StringUtils.isBlank(vaccineCvxCode)) {
						vaccineCvxCode = cvxCode.getValue();
					} else if (!vaccineCvxCode.equals(cvxCode.getValue())) {
						// CPT doesn't map to the CVX code that was submitted!
						ProcessingException pe = new ProcessingException("CPT " + vaccineCptCode + " maps to " + cvxCode.getValue() + " but CVX " + vaccineCvxCode + " was also reported, preferring CVX code", "RXA", rxaCount, 5, IisReportableSeverityLevel.WARN);
						iisReportableList.add(iisReportableUtilService.fromProcessingException(pe));
					}
				}
			}
		}
		if (StringUtils.isBlank(vaccineCvxCode)) {
			throw new ProcessingException("Unable to find a recognized vaccine administration code (CVX, NDC, or CPT)", "RXA", rxaCount, 5);
		} else {
			Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, vaccineCvxCode);
			if (cvxCode != null) {
				vaccineCvxCode = cvxCode.getValue();
			} else {
				throw new ProcessingException("Unrecognized CVX vaccine '" + vaccineCvxCode + "'", "RXA", rxaCount, 5);
			}
		}
		vaccinationReported.setVaccineNdcCode(vaccineNdcCode);
		vaccinationReported.setVaccineCvxCode(vaccineCvxCode);
		return vaccinationReported;

	}
}
