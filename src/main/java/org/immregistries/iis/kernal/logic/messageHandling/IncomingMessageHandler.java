package org.immregistries.iis.kernal.logic.messageHandling;

import org.hl7.fhir.instance.model.api.IIdType;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.iis.kernal.fhir.interceptors.PartitionTenantCreationInterceptor;
import org.immregistries.iis.kernal.logic.*;
import org.immregistries.iis.kernal.logic.ack.IisReportable;
import org.immregistries.iis.kernal.logic.ack.ReportableUtil;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.ProcessingFlavor;
import org.immregistries.iis.kernal.model.VaccinationReported;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class IncomingMessageHandler<ParsedSource, ValidationResult> implements IIncomingMessageHandler<ParsedSource> {

	Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	PartitionTenantCreationInterceptor partitionTenantCreationInterceptor;
	@Autowired
	ValidationService validationService;
	@Autowired
	MessageRecordingService messageRecordingService;
	@Autowired
	private CodeMapManagerService codeMapManagerService;

	@Override
	public String process(String message, Tenant tenant, String sendingFacilityName) {
		/*
		 * Anticipating the partition creation, to prevent conflict when multiple FHIR Request try to create the same partition
		 */
		partitionTenantCreationInterceptor.getOrCreatePartitionId(tenant.getOrganizationName());

		ParsedSource parsedSource = parseSource(message);
		String messageType = extractMessageType(parsedSource);
		String responseMessage;
		Set<ProcessingFlavor> processingFlavorSet = null;
		try {
			processingFlavorSet = tenant.getProcessingFlavorSet();
			IIdType organizationIdType = readResponsibleOrganizationIIdType(tenant, parsedSource, sendingFacilityName, processingFlavorSet);
			switch (messageType) {
				case "VXU":
					responseMessage = processVXU(tenant, parsedSource, message, organizationIdType);
					break;
				case "ORU":
					responseMessage = processORU(tenant, parsedSource, message, organizationIdType);
					break;
				case "QBP":
					responseMessage = processQBP(tenant, parsedSource, message, organizationIdType);
					break;
				default:
					ProcessingException pe = new ProcessingException("Unsupported message", "", 0, 0);
					List<IisReportable> iisReportableList = List.of(ReportableUtil.fromProcessingException(pe));
					responseMessage = buildResultWithoutValidation(parsedSource, iisReportableList, processingFlavorSet);
					messageRecordingService.recordMessageReceived(message, null, responseMessage, "Unknown", "NAck", tenant);
					break;
			}

		} catch (Exception e) {
			e.printStackTrace(System.err);
			List<IisReportable> iisReportableList = new ArrayList<>();
			iisReportableList.add(ReportableUtil.fromProcessingException(new ProcessingException("Internal error prevented processing: " + e.getMessage(), null, 0, 0)));
			responseMessage = buildResultWithoutValidation(parsedSource, iisReportableList, processingFlavorSet);
		}
		return responseMessage;
	}

	public abstract String extractMessageType(ParsedSource parsedSource);

	public abstract ParsedSource parseSource(String message);

	abstract @Nullable IIdType readResponsibleOrganizationIIdType(Tenant tenant, ParsedSource parsedSource, String sendingFacilityName, Set<ProcessingFlavor> processingFlavorSet) throws ProcessingException;

	//	public abstract String processVXU(Tenant tenant, SourceType sourceType, String message, IIdType managingOrganizationId) throws Exception;
	public String processVXU(Tenant tenant, ParsedSource parsedSource, String message, IIdType managingOrganizationId) throws Exception {
		List<IisReportable> iisReportableList = new ArrayList<>();
		Set<ProcessingFlavor> processingFlavorSet = tenant.getProcessingFlavorSet();
		/**
		 * Typically uses mqe and validation tool and fill the iisReportableList
		 * TODO FHIR
		 */
		ValidationResult validationResult = validation(message, iisReportableList);

		try {
			CodeMap codeMap = codeMapManagerService.getCodeMap();
			boolean strictDate = !processingFlavorSet.contains(ProcessingFlavor.CANTALOUPE);
			PatientReported patientReported = processPatient(tenant, parsedSource, iisReportableList, processingFlavorSet, codeMap, strictDate, managingOrganizationId);

			List<VaccinationReported> vaccinationReportedList = processVaccinations(parsedSource, tenant, iisReportableList, patientReported, processingFlavorSet, strictDate);
			String ack = buildResultWithValidation(parsedSource, validationResult, iisReportableList, processingFlavorSet);
			messageRecordingService.recordMessageReceived(message, patientReported, ack, "Update", "Ack", tenant);
			return ack;
		} catch (ProcessingException e) {
			IisReportable exceptionReportable = ReportableUtil.fromProcessingException(e);
			if (!iisReportableList.contains(exceptionReportable)) {
				iisReportableList.add(exceptionReportable);
			}
			String ack = buildResultWithValidation(parsedSource, validationResult, iisReportableList, processingFlavorSet);
			messageRecordingService.recordMessageReceived(message, null, ack, "Update", "Exception", tenant);
			return ack;
		}
	}


	public abstract PatientReported processPatient(Tenant tenant, ParsedSource parsedSource, List<IisReportable> iisReportableList, Set<ProcessingFlavor> processingFlavorSet, CodeMap codeMap, boolean strictDate, IIdType managingOrganizationId) throws ProcessingException;

	public abstract List<VaccinationReported> processVaccinations(ParsedSource parsedSource, Tenant tenant, List<IisReportable> iisReportableList, PatientReported patientReported, Set<ProcessingFlavor> processingFlavorSet, boolean strictDate) throws ProcessingException;

	public abstract String processORU(Tenant tenant, ParsedSource parsedSource, String message, IIdType managingOrganizationId);

	public abstract String processQBP(Tenant tenant, ParsedSource parsedSource, String messageReceived, IIdType managingOrganizationId) throws Exception;


	public abstract String buildResultWithoutValidation(ParsedSource parsedSource, List<IisReportable> iisReportableList, Set<ProcessingFlavor> processingFlavorSet);

	public abstract String buildResultWithValidation(ParsedSource parsedSource, ValidationResult validationResult, List<IisReportable> iisReportableList, Set<ProcessingFlavor> processingFlavorSet);

	public abstract ValidationResult validation(String message, List<IisReportable> iisReportableList) throws Exception;

//	public abstract ModelPerson processPersonPractitioner(ParsedSource parsedSource, Tenant tenant, int fieldNum);


}
