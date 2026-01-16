package org.immregistries.iis.kernal.logic.logicInterceptors;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.logic.CodeMapManagerService;
import org.immregistries.iis.kernal.logic.ProcessingException;
import org.immregistries.iis.kernal.logic.ack.IisReportable;
import org.immregistries.iis.kernal.logic.ack.IisReportableSeverity;
import org.immregistries.iis.kernal.logic.ack.ReportableUtil;
import org.immregistries.iis.kernal.mapping.mappers.resources.IisResourceMasterReportedMapper;
import org.immregistries.iis.kernal.mapping.mappers.resources.ImmunizationMapper;
import org.immregistries.iis.kernal.model.IisMappedToFhirResource;
import org.immregistries.iis.kernal.model.ProcessingFlavor;
import org.immregistries.iis.kernal.model.VaccinationReported;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static ca.uhn.fhir.interceptor.api.Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED;

@Interceptor
@Service
public class ImmunizationProcessingInterceptor extends IisLogicInterceptor {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private ImmunizationMapper immunizationMapper;
	@Autowired
	private CodeMapManagerService codeMapManagerService;
	@Autowired
	private FhirContext fhirContext;
	private Random random = new Random();

	@Hook(value = SERVER_INCOMING_REQUEST_PRE_HANDLED, order = 2001)
	public void handle(RequestDetails requestDetails) throws InvalidRequestException, ProcessingException {
		Set<ProcessingFlavor> processingFlavorSet = ProcessingFlavor.getProcessingStyle(requestDetails.getTenantId());
		List<IisReportable> iisReportableList = iisReportableList(requestDetails);
		if (requestDetails.getResource() == null || requestDetails.getRestOperationType() == null) {
			return;
		}
		IAnyResource result = (IAnyResource) requestDetails.getResource();
		if (requestDetails.getRestOperationType().equals(RestOperationTypeEnum.UPDATE) || requestDetails.getRestOperationType().equals(RestOperationTypeEnum.CREATE)) {
			if (requestDetails.getResource() instanceof org.hl7.fhir.r4.model.Immunization || requestDetails.getResource() instanceof org.hl7.fhir.r5.model.Immunization) {
				testMappingFhir((IisResourceMasterReportedMapper<IisMappedToFhirResource, IisMappedToFhirResource, IisMappedToFhirResource, IAnyResource>) immunizationMapper, (IAnyResource) requestDetails.getResource(), fhirContext.newJsonParser());
				VaccinationReported vaccinationReported = immunizationMapper.localObjectReported((IAnyResource) requestDetails.getResource());
				vaccinationReported = processAndValidateVaccinationReported(vaccinationReported, iisReportableList, processingFlavorSet, -1, -1, -1, "");
				result = immunizationMapper.fhirObject(vaccinationReported);
			}
		}
		requestDetails.setResource(result);
		requestDetails.setAttribute(IIS_REPORTABLE_LIST, iisReportableList);
	}

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
				pe.setErrorCode(IisReportableSeverity.WARN);
				iisReportableList.add(ReportableUtil.fromProcessingException(pe));
			}
		}


		if (processingFlavorSet.contains(ProcessingFlavor.GRAPEFRUIT) && random.nextBoolean()) {
			throw new ProcessingException("Vaccination randomly rejected, Patient Accepted", "RXR", 0, 0, IisReportableSeverity.NOTICE);
		}


		String fundingEligibility = vaccinationReported.getFundingEligibility();
		if (!fundingEligibility.isEmpty()) {
			Code fundingEligibilityCode = codeMap.getCodeForCodeset(CodesetType.FINANCIAL_STATUS_CODE, fundingEligibility);
			if (fundingEligibilityCode == null) {
				ProcessingException pe = new ProcessingException("Funding eligibility '" + fundingEligibility + "' was not recognized", "OBX", fundingEligibilityObxCount, 5, IisReportableSeverity.WARN);
				iisReportableList.add(ReportableUtil.fromProcessingException(pe));
				vaccinationReported.setFundingEligibility("");
			}
		}

		String fundingSource = vaccinationReported.getFundingSource();
		if (!fundingSource.isEmpty()) {
			Code fundingSourceCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_FUNDING_SOURCE, fundingSource);
			if (fundingSourceCode == null) {
				ProcessingException pe = new ProcessingException("Funding source '" + fundingSource + "' was not recognized", "OBX", fundingSourceObxCount, 5, IisReportableSeverity.WARN);
				iisReportableList.add(ReportableUtil.fromProcessingException(pe));
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
					ProcessingException pe = new ProcessingException("Unrecognized NDC " + vaccineNdcCode, "RXA", rxaCount, 5, IisReportableSeverity.WARN);
					iisReportableList.add(ReportableUtil.fromProcessingException(pe));
				} else {
					if (StringUtils.isBlank(vaccineCvxCode)) {
						vaccineCvxCode = cvxCode.getValue();
					} else if (!vaccineCvxCode.equals(cvxCode.getValue())) {
						// NDC doesn't map to the CVX code that was submitted!
						ProcessingException pe = new ProcessingException("NDC " + vaccineNdcCode + " maps to " + cvxCode.getValue() + " but CVX " + vaccineCvxCode + " was also reported, preferring CVX code", "RXA", rxaCount, 5, IisReportableSeverity.WARN);
						iisReportableList.add(ReportableUtil.fromProcessingException(pe));
					}
				}
			}
		}
		{
			Code cptCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CPT_CODE, vaccineCptCode);
			if (cptCode != null) {
				Code cvxCode = codeMap.getRelatedCode(cptCode, CodesetType.VACCINATION_CVX_CODE);
				if (cvxCode == null) {
					ProcessingException pe = new ProcessingException("Unrecognized CPT " + cptCode, "RXA", rxaCount, 5, IisReportableSeverity.WARN);
					iisReportableList.add(ReportableUtil.fromProcessingException(pe));
				} else {
					if (StringUtils.isBlank(vaccineCvxCode)) {
						vaccineCvxCode = cvxCode.getValue();
					} else if (!vaccineCvxCode.equals(cvxCode.getValue())) {
						// CPT doesn't map to the CVX code that was submitted!
						ProcessingException pe = new ProcessingException("CPT " + vaccineCptCode + " maps to " + cvxCode.getValue() + " but CVX " + vaccineCvxCode + " was also reported, preferring CVX code", "RXA", rxaCount, 5, IisReportableSeverity.WARN);
						iisReportableList.add(ReportableUtil.fromProcessingException(pe));
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
