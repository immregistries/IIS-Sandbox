package org.immregistries.iis.kernal.logic.messageHandling;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IIdType;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.iis.kernal.SoftwareVersion;

import org.immregistries.iis.kernal.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.logic.*;
import org.immregistries.iis.kernal.logic.ack.*;
import org.immregistries.iis.kernal.logic.logicInterceptors.ImmunizationProcessingInterceptor;
import org.immregistries.iis.kernal.logic.logicInterceptors.ObservationProcessingInterceptor;
import org.immregistries.iis.kernal.logic.logicInterceptors.PatientProcessingInterceptor;
import org.immregistries.iis.kernal.mapping.internalClient.FhirSaveRequester;
import org.immregistries.iis.kernal.mapping.internalClient.FhirSearchRequester;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.mqe.validator.MqeMessageServiceResponse;
import org.immregistries.mqe.validator.engine.ValidationRuleResult;
import org.immregistries.mqe.vxu.MqeMessageHeader;
import org.immregistries.smm.tester.manager.HL7Reader;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Processes the Incoming Hl7v2 Messages, parsing into local objects and saving into database through FHIR Requester
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class V2IncomingMessageHandler extends IncomingMessageHandler<HL7Reader, MqeMessageServiceResponse> {

	protected final Logger logger = LoggerFactory.getLogger(V2IncomingMessageHandler.class);

	@Autowired
	ValidationService validationService;
	@Autowired
	FhirSaveRequester fhirSaveRequester;
	@Autowired
	FhirSearchRequester fhirSearchRequester;
	@Autowired
	AbstractHl7MessageWriter hl7MessageWriter;
	@Autowired
	PatientProcessingInterceptor patientProcessingInterceptor; // TODO decide how/where to implement the execution of interceptors, currently using DAO so some interceptors are skipped by the v2 process and need to be manually triggered
	@Autowired
	ObservationProcessingInterceptor observationProcessingInterceptor;
	@Autowired
	ImmunizationProcessingInterceptor immunizationProcessingInterceptor;
	@Autowired
	IncomingQueryHandler incomingQueryHandler;

	@Autowired
	MessageRecordingService messageRecordingService;
	@Autowired
	CodeMapManagerService codeMapManagerService;


	public V2IncomingMessageHandler() {
	}

	public String buildResultWithoutValidation(HL7Reader reader, List<IisReportable> iisReportableList, Set<ProcessingFlavor> processingFlavorSet) {
		StringBuilder sb = new StringBuilder();
		{
			String messageType = "ACK^V04^ACK";
			String profileId = Z23_ACKNOWLEDGEMENT;
			hl7MessageWriter.createMSH(messageType, profileId, reader, sb, processingFlavorSet);
		}

		// if processing flavor contains MEDLAR then all the non E errors have to be removed from the processing list
		if (processingFlavorSet != null && processingFlavorSet.contains(ProcessingFlavor.MEDLAR)) {
			List<IisReportable> tempIisReportableList = new ArrayList<>();
			for (IisReportable reportable : iisReportableList) {
				if (reportable.isError()) {
					tempIisReportableList.add(reportable);
				}
			}
			iisReportableList = tempIisReportableList;
		}

		String sendersUniqueId;
		reader.resetPostion();
		if (reader.advanceToSegment(MSH)) {
			sendersUniqueId = reader.getValue(10);
		} else {
			sendersUniqueId = "MSH NOT FOUND";
		}
		if (sendersUniqueId.isBlank()) {
			sendersUniqueId = "MSH-10 NOT VALUED";
		}
		String overallStatus = "AA";
		for (IisReportable reportable : iisReportableList) {
			if (reportable.isError() || reportable.isWarning()) {
				overallStatus = "AE";
				break;
			}
		}

		sb.append("MSA|").append(overallStatus).append("|").append(sendersUniqueId).append("\r");
		for (IisReportable reportable : iisReportableList) {
			sb.append(IisHL7Util.makeERRSegment(reportable, false));
		}
		return sb.toString();
	}

	public String buildResultWithValidation(HL7Reader reader, MqeMessageServiceResponse mqeMessageServiceResponse, List<IisReportable> iisReportableList, Set<ProcessingFlavor> processingFlavorSet) {
		IisAckBuilder ackBuilder = IisAckBuilder.INSTANCE;
		IisAckData data = new IisAckData();
		MqeMessageHeader header = mqeMessageServiceResponse.getMessageObjects().getMessageHeader();
		String profileId = header.getMessageProfile();
		reader.resetPostion();
		reader.advanceToSegment(MSH);
		String profileExtension = null;
		int count = reader.getRepeatCount(21);
		if (count > 1) {
			profileExtension = reader.getValueRepeat(21, 1, 2);
			if (profileExtension.equals(ADVANCED_ACK)) {
				data.setProfileExtension(ADVANCED_ACK);
			} // else not supported
		}
		if (profileId.equals(VXU_Z22)) {
			data.setProfileId(Z23_ACKNOWLEDGEMENT);
		} else if (profileId.equals(QBP_Z34)) {
			data.setProfileId(RSP_Z33_NO_MATCH);
		} else {
			data.setProfileId(Z23_ACKNOWLEDGEMENT);
		}


		List<ValidationRuleResult> resultList = mqeMessageServiceResponse.getValidationResults();
		List<IisReportable> reportables = iisReportableList;
		/* This code needs to get put somewhere better. */
		for (ValidationRuleResult result : resultList) {
			reportables.addAll(result.getValidationDetections().stream().map(IisReportable::new).collect(Collectors.toList()));
		}
		// if processing flavor contains MEDLAR then all the non E errors have to removed from the processing list
		if (processingFlavorSet != null && processingFlavorSet.contains(ProcessingFlavor.MEDLAR)) {
			reportables = reportables.stream().filter(reportable -> !IisReportableSeverity.ERROR.equals(reportable.getSeverity())).collect(Collectors.toList());
		}
		data.setReportables(reportables);

		String messageType = "ACK^V04^ACK";
		if (processingFlavorSet != null && processingFlavorSet.contains(ProcessingFlavor.MELON)) {
			int pos = messageType.indexOf("^");
			if (pos > 0) {
				messageType = messageType.substring(0, pos);
				if (System.currentTimeMillis() % 2 == 0) {
					messageType += "^ZZZ";
				}
			}
		}

		StringBuilder receivingApp = new StringBuilder("IIS Sandbox");
		if (processingFlavorSet != null) {
			for (ProcessingFlavor processingFlavor : ProcessingFlavor.values()) {
				if (processingFlavorSet.contains(processingFlavor)) {
					receivingApp.append(" ").append(processingFlavor.getKey());
				}
			}
		}
		receivingApp.append(" v").append(SoftwareVersion.VERSION);
		data.setReceivingFacility(receivingApp.toString());


		String sendersUniqueId = header.getMessageControl();
//		if (reader.advanceToSegment("MSH")) {
//			header.getMessageStructure()
//			sendersUniqueId = reader.getValue(10);
//		} else {
//			sendersUniqueId = "MSH NOT FOUND";
//		}
		if (sendersUniqueId.isBlank()) {
			sendersUniqueId = "MSH-10 NOT VALUED";
		}
		data.setReceivingApplication(receivingApp.toString());
		data.setReceivingFacility(CurrentTenantUtil.getTenant().getOrganizationName());

		data.setMessageControlId(sendersUniqueId);
		data.setMessageDate(header.getMessageDate());
		data.setMessageProfileId(header.getMessageProfile());
		data.setMessageVersionId(header.getMessageVersion());
		data.setProcessingControlId(header.getProcessingStatus());
		data.setSendingFacility(header.getSendingFacility());
		data.setSendingApplication(header.getSendingApplication());
		data.setResponseType("?");
		data.setReportables(reportables);

		return ackBuilder.buildAckFrom(data, processingFlavorSet);
	}


	public String extractMessageType(HL7Reader reader) {
		return reader.getValue(9);
	}

	public int readAndCreateObservations(HL7Reader reader, List<IisReportable> iisReportableList, Set<ProcessingFlavor> processingFlavorSet, PatientReported patientReported, boolean strictDate, int obxCount, VaccinationReported vaccinationReported, VaccinationMaster vaccination) throws ProcessingException {
		String previousSubId = "";
		ObservationReported currentMainObservation = null;
		while (reader.advanceToSegment(OBX, ORC)) {
			obxCount++;
			String subId = reader.getValue(4);
			/*
			 * If no sub id or sub id changed, no main observation is set in iteration,
			 * Saving if changing the main observation
			 */
			if (StringUtils.isBlank(subId) || !StringUtils.equals(previousSubId, subId)) {
				if (currentMainObservation != null) {
					observationProcessingInterceptor.processAndValidateObservationReported(currentMainObservation, iisReportableList, processingFlavorSet, obxCount, patientReported.getBirthDate());
					fhirSaveRequester.saveObservationReported(currentMainObservation);
					currentMainObservation = null;
				}
			}
			/*
			 * Reading Observation or Sub Observation
			 */
			ObservationReported observationReported = readObservations(reader, iisReportableList, patientReported, strictDate, obxCount, vaccinationReported, vaccination);
//			if (currentMainObservation != null) {
//				observationReported.setPartOfObservationId(currentMainObservation.getPartOfObservationId());
//			}
			/*
			 * if subId Changed, new Main Observation, else add as subObservation
			 */
			if (!StringUtils.equals(previousSubId, subId) && StringUtils.isNotBlank(subId)) {
				currentMainObservation = observationReported;
			} else if (currentMainObservation != null) {
				currentMainObservation.addComponent(observationReported);
			}
			previousSubId = subId;
		}
		if (currentMainObservation != null) {
			observationProcessingInterceptor.processAndValidateObservationReported(currentMainObservation, iisReportableList, processingFlavorSet, obxCount, patientReported.getBirthDate());
			fhirSaveRequester.saveObservationReported(currentMainObservation);
		}
		return obxCount;
	}


	public String processORU(Tenant tenant, HL7Reader reader, String message, IIdType managingOrganizationId) {
		List<IisReportable> iisReportableList = new ArrayList<>();
		Set<ProcessingFlavor> processingFlavorSet = tenant.getProcessingFlavorSet();
		try {
			CodeMap codeMap = codeMapManagerService.getCodeMap();

			boolean strictDate = !processingFlavorSet.contains(ProcessingFlavor.CANTALOUPE);
			PatientReported patientReported = processPatient(tenant, reader, iisReportableList, processingFlavorSet, codeMap, strictDate, managingOrganizationId);

			int orcCount = 0;
			int obxCount = 0;
			while (reader.advanceToSegment(ORC)) {
				orcCount++;
				if (reader.advanceToSegment(OBR, ORC)) {
					obxCount = readAndCreateObservations(reader, iisReportableList, processingFlavorSet, patientReported, strictDate, obxCount, null, null);
				} else {
					throw new ProcessingException("OBR segment was not found after ORC segment", ORC, orcCount, 0);
				}
			}
			String ack = buildResultWithoutValidation(reader, iisReportableList, processingFlavorSet);
			messageRecordingService.recordMessageReceived(message, patientReported, ack, "Update", "Ack", tenant);
			return ack;
		} catch (ProcessingException e) {
			IisReportable exceptionReportable = ReportableUtil.fromProcessingException(e);
			if (!iisReportableList.contains(exceptionReportable)) {
				iisReportableList.add(exceptionReportable);
			}
			String ack = buildResultWithoutValidation(reader, iisReportableList, processingFlavorSet);
			messageRecordingService.recordMessageReceived(message, null, ack, "Update", "Exception", tenant);
			return ack;
		}
	}

	public String processQBP(Tenant tenant, HL7Reader reader, String messageReceived, IIdType managingOrganizationId) throws Exception {
		return incomingQueryHandler.processQBP(tenant, reader, messageReceived, managingOrganizationId);
	}

	public PatientReported processPatient(Tenant tenant, HL7Reader reader, List<IisReportable> iisReportableList, Set<ProcessingFlavor> processingFlavorSet, CodeMap codeMap, boolean strictDate, IIdType managingOrganizationId) throws ProcessingException {
//		PatientReported patientReported = null; // TODO figure out process of merging information in golden record
//			fhirSearchRequester.searchPatientReported(new SearchParameterMap("identifier", new TokenParam().setValue(patientReportedExternalLink)));
//		if (patientReported == null) {
		PatientReported patientReported;
		patientReported = new PatientReported();
		patientReported.setTenant(tenant);
		patientReported.setReportedDate(new Date());
		if (managingOrganizationId != null && managingOrganizationId.hasIdPart()) {
			patientReported.setManagingOrganizationId("Organization/" + managingOrganizationId.getIdPart());
		}
		/*
		 * PID processing
		 */
		if (reader.advanceToSegment("PID")) {
			processPID(reader, patientReported, iisReportableList, strictDate);
		} else {
			throw new ProcessingException("No PID segment found, required for accepting vaccination report", "", 0, 0);
		}

		if (reader.advanceToSegment("PD1")) {
			processPD1(reader, patientReported, tenant, iisReportableList, strictDate);
		}
		reader.resetPostion();

		while (reader.advanceToSegment("NK1")) {
			processNK1(reader, patientReported);
		}
		reader.resetPostion();

		patientProcessingInterceptor.processAndValidatePatient(patientReported, iisReportableList, processingFlavorSet);
		IIncomingMessageHandler.verifyNoErrors(iisReportableList);

		patientReported.setUpdatedDate(new Date());
		patientReported = fhirSaveRequester.savePatientReported(patientReported);
//		patientReported = fhirRequester.saveRelatedPerson(patientReported);
		iisReportableList.add(ReportableUtil.fromProcessingException(new ProcessingException("Patient record saved", PID, 0, 0, IisReportableSeverity.INFO)));

		/*
		 * checking if request is gathering patients  Ids to create a group, TODO cleaner solution
		 */
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		ArrayList<String> groupPatientIds = (ArrayList<String>) request.getAttribute("groupPatientIds");
		if (groupPatientIds != null) { // If there are numerous patients added and option was activated
			groupPatientIds.add(patientReported.getPatientId());
		}
		request.setAttribute("groupPatientIds", groupPatientIds);
		return patientReported;
	}

	private void processNK1(HL7Reader reader, PatientReported patientReported) {
		PatientGuardian patientGuardian = new PatientGuardian();
		patientReported.addPatientGuardian(patientGuardian);
		String guardianLast = reader.getValue(2, 1);
		patientGuardian.getName().setNameLast(guardianLast);
		String guardianFirst = reader.getValue(2, 2);
		patientGuardian.getName().setNameFirst(guardianFirst);
		String guardianMiddle = reader.getValue(2, 3);
		patientGuardian.getName().setNameMiddle(guardianMiddle);
		String guardianRelationship = reader.getValue(3);
		patientGuardian.setGuardianRelationship(guardianRelationship);
	}

	private void processPID(HL7Reader reader, PatientReported patientReported, List<IisReportable> iisReportableList, boolean strictDate) throws ProcessingException {
		for (int i = 1; i <= reader.getRepeatCount(3); i++) {
			BusinessIdentifier businessIdentifier = new BusinessIdentifier();
			businessIdentifier.setValue(reader.getValueRepeat(3, 1, i));
			businessIdentifier.setSystem(reader.getValueRepeat(3, 4, i));
			businessIdentifier.setType(reader.getValueRepeat(3, 5, i));
			patientReported.addBusinessIdentifier(businessIdentifier);
		}
		if (patientReported.getMainBusinessIdentifier() == null || StringUtils.isBlank(patientReported.getMainBusinessIdentifier().getValue())) {
			throw new ProcessingException("MRN was not found, required for accepting vaccination report", PID, 1, 3);
		}

		List<ModelName> names = new ArrayList<>(reader.getRepeatCount(5));
		for (int i = 1; i <= reader.getRepeatCount(5); i++) {
			String patientNameLast = reader.getValueRepeat(5, 1, i);
			String patientNameFirst = reader.getValueRepeat(5, 2, i);
			String patientNameMiddle = reader.getValueRepeat(5, 3, i);
			String nameType = reader.getValueRepeat(5, 7, i);
			ModelName modelName = new ModelName(patientNameLast, patientNameFirst, patientNameMiddle, nameType);
			names.add(modelName);
		}
		patientReported.setPatientNames(names);

		Date patientBirthDate;
		patientBirthDate = IIncomingMessageHandler.parseDateError(reader.getValue(7), "Bad format for date of birth", PID, 1, 7, strictDate);
		patientReported.setMotherMaidenName(reader.getValue(6));
		patientReported.setBirthDate(patientBirthDate);
		patientReported.setSex(reader.getValue(8));

		for (int i = 1; i <= reader.getRepeatCount(10); i++) {
			patientReported.addRace(reader.getValueRepeat(10, 1, i));
		}

		String zip = reader.getValue(11, 5);
		if (zip.length() > 5) {
			zip = zip.substring(0, 5);
		}
		String addressFragPrep = reader.getValue(11, 1);
		String addressFrag = "";
		{
			int spaceIndex = addressFragPrep.indexOf(" ");
			if (spaceIndex > 0) {
				addressFragPrep = addressFragPrep.substring(0, spaceIndex);
			}
			addressFrag = zip + ":" + addressFragPrep;
		}
		ModelAddress modelAddress = new ModelAddress();
		modelAddress.setAddressLine1(reader.getValue(11, 1));
		modelAddress.setAddressLine2(reader.getValue(11, 2));
		modelAddress.setAddressCity(reader.getValue(11, 3));
		modelAddress.setAddressState(reader.getValue(11, 4));
		modelAddress.setAddressZip(reader.getValue(11, 5));
		modelAddress.setAddressCountry(reader.getValue(11, 6));
		modelAddress.setAddressCountyParish(reader.getValue(11, 9));
		patientReported.addAddress(modelAddress);


		for (int i = 1; i <= reader.getRepeatCount(13); i++) {
			String use = reader.getValueRepeat(13, 2, i);
			if (EMAIL_USE_CODE.equals(use)) {
				patientReported.setEmail(reader.getValueRepeat(13, 4, i));
			} else {
				ModelPhone patientPhone = new ModelPhone();
				patientPhone.setNumber(reader.getValueRepeat(13, 6, i) + reader.getValueRepeat(13, 7, i));
				patientPhone.setUse(use);
				// Logic exported to Patient Processing interceptor
//		if (!"PRN".equals(patientPhone.getUse())) {
//			patientPhone.setUse("");
//		}
				patientReported.addPhone(patientPhone);
			}
		}
		patientReported.setEthnicity(reader.getValue(22));
		patientReported.setBirthFlag(reader.getValue(24));
		patientReported.setBirthOrder(reader.getValue(25));
		patientReported.setDeathDate(IIncomingMessageHandler.parseDateWarn(reader.getValue(29), "Invalid patient death date", PID, 1, 29, strictDate, iisReportableList));
		patientReported.setDeathFlag(reader.getValue(30));
	}

	private void processPD1(HL7Reader reader, PatientReported patientReported, Tenant tenant, List<IisReportable> iisReportableList, boolean strictDate) {
		ModelPerson generalPractitioner = processPersonPractitioner(reader, tenant, 4);
		if (generalPractitioner != null) {
			patientReported.setGeneralPractitionerId("Practitioner/" + generalPractitioner.getPersonId());
		}
		patientReported.setPublicityIndicator(reader.getValue(11));
		patientReported.setProtectionIndicator(reader.getValue(12));
		patientReported.setProtectionIndicatorDate(IIncomingMessageHandler.parseDateWarn(reader.getValue(13), "Invalid protection indicator date", PD_1, 1, 13, strictDate, iisReportableList));
		patientReported.setRegistryStatusIndicator(reader.getValue(16));
		patientReported.setRegistryStatusIndicatorDate(IIncomingMessageHandler.parseDateWarn(reader.getValue(17), "Invalid registry status indicator date", PD_1, 1, 17, strictDate, iisReportableList));
		patientReported.setPublicityIndicatorDate(IIncomingMessageHandler.parseDateWarn(reader.getValue(18), "Invalid publicity indicator date", PD_1, 1, 18, strictDate, iisReportableList));
	}

	public List<VaccinationReported> processVaccinations(HL7Reader reader, Tenant tenant, List<IisReportable> iisReportableList, PatientReported patientReported, Set<ProcessingFlavor> processingFlavorSet, boolean strictDate) throws ProcessingException {
		int orcCount = 0;
		int rxaCount = 0;
		int obxCount = 0;
		int vaccinationCount = 0;
		int refusalCount = 0;
		List<VaccinationReported> vaccinationReportedList = new ArrayList<>(4);
		while (reader.advanceToSegment(ORC)) {
			orcCount++;
			VaccinationReported vaccinationReported = null;
			String vaccineCode = "";
			Date administrationDate = null;
			BusinessIdentifier placerIdentifier = null;
			if (StringUtils.isNotBlank(reader.getValue(2))) {
				placerIdentifier = new BusinessIdentifier();
				placerIdentifier.setValue(reader.getValue(2));
				placerIdentifier.setSystem(reader.getValue(2, 2));
				placerIdentifier.setType("PLAC"); // According to v2 to FHIR
			}
			BusinessIdentifier fillerIdentifier = null;
			if (StringUtils.isNotBlank(reader.getValue(3))) {
				fillerIdentifier = new BusinessIdentifier();
				fillerIdentifier.setValue(reader.getValue(3));
				fillerIdentifier.setSystem(reader.getValue(3, 2));
				fillerIdentifier.setType("FILL"); // According to v2 to FHIR
			}
			ModelPerson enteringProvider = processPersonPractitioner(reader, tenant, 10);

			ModelPerson orderingProvider = processPersonPractitioner(reader, tenant, 12);

			boolean rxaPresent = reader.advanceToSegment(RXA, ORC);
			if (!rxaPresent) {
				throw new ProcessingException("RXA segment was not found after ORC segment", ORC, orcCount, 0);
			}
			rxaCount++;
			vaccineCode = reader.getValue(5, 1);
			if (StringUtils.isBlank(vaccineCode)) {
				throw new ProcessingException("Vaccine code is not indicated in RXA-5.1", RXA, rxaCount, 5);
			}
			if (vaccineCode.equals("998")) {
				obxCount = readAndCreateObservations(reader, iisReportableList, processingFlavorSet, patientReported, strictDate, obxCount, null, null);
				continue;
			}
			if (fillerIdentifier == null) {
				throw new ProcessingException("Vaccination order id was not found, unable to process", ORC, orcCount, 3);
			}
			administrationDate = IIncomingMessageHandler.parseDateError(reader.getValue(3, 1), "Could not read administered date in RXA-5", "RXA", rxaCount, 3, strictDate);
//			if (administrationDate.after(new Date())) {
//				throw new ProcessingException("Vaccination is indicated as occurring in the future, unable to accept future vaccination events", "RXA", rxaCount, 3);
//			}

//			TokenParam fillerIdentifierParam = fillerIdentifier.asTokenParam();
//			if (fillerIdentifierParam != null) {
//				vaccinationReported = fhirSearchRequester.searchVaccinationReported(new SearchParameterMap("identifier", fillerIdentifierParam));
//			}

			/*
			 * Create new vaccine report if null
			 */
			if (vaccinationReported == null) {
				vaccinationReported = new VaccinationReported();
				vaccinationReported.setReportedDate(new Date());
				if (placerIdentifier != null) {
					vaccinationReported.addBusinessIdentifier(placerIdentifier);
				}
				if (fillerIdentifier != null) {
					vaccinationReported.addBusinessIdentifier(fillerIdentifier);
				}
			}

			vaccinationReported.setPatientReported(patientReported);

			vaccinationReported.setEnteredBy(enteringProvider);
			vaccinationReported.setAdministeringProvider(orderingProvider);

			/*
			 * Extracting Vaccine Codes
			 */
			String vaccineCvxCode = "";
			String vaccineNdcCode = "";
			String vaccineCptCode = "";

			/*
			 * Checking Vaccine Type
			 */
			String vaccineCodeType = reader.getValue(5, 3);
			if (vaccineCodeType.equals(VACCINE_CODE_TYPE_NDC)) {
				vaccineNdcCode = vaccineCode;
			} else if (vaccineCodeType.equals(VACCINE_CODE_TYPE_CPT) || vaccineCodeType.equals(VACCINE_CODE_TYPE_C_4) || vaccineCodeType.equals(VACCINE_CODE_TYPE_C_5)) {
				vaccineCptCode = vaccineCode;
			} else {
				vaccineCvxCode = vaccineCode;
			}

			/*
			 * Checking alt Vaccine code field and Type
			 */
			String altVaccineCode = reader.getValue(5, 4);
			String altVaccineCodeType = reader.getValue(5, 6);
			if (StringUtils.isNotBlank(altVaccineCode)) {
				if (altVaccineCodeType.equals(VACCINE_CODE_TYPE_NDC)) {
					if (StringUtils.isBlank(vaccineNdcCode)) {
						vaccineNdcCode = altVaccineCode;
					}
				} else if (altVaccineCodeType.equals(VACCINE_CODE_TYPE_CPT) || altVaccineCodeType.equals(VACCINE_CODE_TYPE_C_4) || altVaccineCodeType.equals(VACCINE_CODE_TYPE_C_5)) {
					if (StringUtils.isBlank(vaccineCptCode)) {
						vaccineCptCode = altVaccineCode;
					}
				} else {
					if (StringUtils.isBlank(vaccineCvxCode)) {
						vaccineCvxCode = altVaccineCode;
					}
				}
			}

			/*
			 * Extract Location
			 */
			OrgLocation orgLocation = processLocation(reader, tenant, processingFlavorSet, rxaCount, 11);
			vaccinationReported.setOrgLocation(orgLocation);

			ModelPerson administeringProvider = processPersonPractitioner(reader, tenant, 10);
			vaccinationReported.setAdministeringProvider(administeringProvider);


			vaccinationReported.setUpdatedDate(new Date());
			vaccinationReported.setAdministeredDate(administrationDate);
			vaccinationReported.setVaccineCvxCode(vaccineCvxCode);
			vaccinationReported.setVaccineNdcCode(vaccineNdcCode);
			vaccinationReported.setAdministeredAmount(reader.getValue(6));
			vaccinationReported.setInformationSource(reader.getValue(9));
			vaccinationReported.setLotnumber(reader.getValue(15));
			vaccinationReported.setExpirationDate(IIncomingMessageHandler.parseDateWarn(reader.getValue(16), "Invalid vaccination expiration date", "RXA", rxaCount, 16, strictDate, iisReportableList));
			vaccinationReported.setVaccineMvxCode(reader.getValue(17));
			vaccinationReported.setRefusalReasonCode(reader.getValue(18));
			vaccinationReported.setCompletionStatus(reader.getValue(20));

			vaccinationReported.setActionCode(reader.getValue(21));
			int segmentPosition = reader.getSegmentPosition();
			if (reader.advanceToSegment(RXR, ORC)) {
				vaccinationReported.setBodyRoute(reader.getValue(1));
				vaccinationReported.setBodySite(reader.getValue(2));
			} else if (processingFlavorSet.contains(ProcessingFlavor.SPRUCE)) {
				if (INFORMATION_SOURCE_NEW.equals(vaccinationReported.getInformationSource())) {
					throw new ProcessingException("RXR segment is required for administered vaccinations", RXA, rxaCount, 0);
				}
			}
			if (vaccinationReported.getAdministeredDate().before(patientReported.getBirthDate()) && !processingFlavorSet.contains(ProcessingFlavor.CLEMENTINE)) {
				throw new ProcessingException("Vaccination is reported as having been administered before the patient was born", RXA, rxaCount, 3);
			}
			if (!vaccinationReported.getVaccineCvxCode().equals("998") && !vaccinationReported.getVaccineCvxCode().equals("999") && (vaccinationReported.getCompletionStatus().equals(COMPLETION_STATUS_COMPLETE) || vaccinationReported.getCompletionStatus().equals(COMPLETION_STATUS_PARTIALLY_ADMINISTERED) || vaccinationReported.getCompletionStatus().isEmpty())) {
				vaccinationCount++;
			}

			if (vaccinationReported.getCompletionStatus().equals("RE")) {
				refusalCount++;
			}

			reader.gotoSegmentPosition(segmentPosition);
			int tempObxCount = obxCount;
			int fundingSourceObxCount = -1;
			int fundingEligibilityObxCount = -1;
			while (reader.advanceToSegment(OBX, ORC)) {
				tempObxCount++;
				String indicator = reader.getValue(3);
				if (indicator.equals(OBX_CODE_FUNDING_ELIGIBILITY)) {
					String fundingEligibility = reader.getValue(5);
					if (StringUtils.isNotBlank(fundingEligibility)) {
						vaccinationReported.setFundingEligibility(fundingEligibility);
						fundingEligibilityObxCount = obxCount;
					}
				} else if (indicator.equals(OBX_CODE_FUNDING_SOURCE)) {
					String fundingSource = reader.getValue(5);
					if (StringUtils.isNotBlank(fundingSource)) {
						vaccinationReported.setFundingSource(fundingSource);
						fundingSourceObxCount = tempObxCount;
					}
				}
			}

			IIncomingMessageHandler.verifyNoErrors(iisReportableList);
			immunizationProcessingInterceptor.processAndValidateVaccinationReported(vaccinationReported, iisReportableList, processingFlavorSet, fundingSourceObxCount, fundingEligibilityObxCount, rxaCount, vaccineCptCode);
			vaccinationReported = fhirSaveRequester.saveVaccinationReported(vaccinationReported);
			vaccinationReportedList.add(vaccinationReported);
			reader.gotoSegmentPosition(segmentPosition);
			obxCount = readAndCreateObservations(reader, iisReportableList, processingFlavorSet, patientReported, strictDate, obxCount, vaccinationReported, vaccinationReported.getVaccinationMaster());

		}
		if (processingFlavorSet.contains(ProcessingFlavor.CRANBERRY) && vaccinationCount == 0) {
			throw new ProcessingException("Patient vaccination history cannot be accepted without at least one administered or historical vaccination specified", "", 0, 0);
		}
		if (processingFlavorSet.contains(ProcessingFlavor.BILBERRY) && (vaccinationCount == 0 && refusalCount == 0)) {
			throw new ProcessingException("Patient vaccination history cannot be accepted without at least one administered, historical, or refused vaccination specified", "", 0, 0);
		}
		return vaccinationReportedList;
	}

	private @Nullable OrgLocation processLocation(HL7Reader reader, Tenant tenant, Set<ProcessingFlavor> processingFlavorSet, int rxaCount, int fieldNum) throws ProcessingException {
		OrgLocation orgLocation = null;
		String administeredAtLocation = reader.getValue(fieldNum, 4);
		if (StringUtils.isNotEmpty(administeredAtLocation)) {
			orgLocation = fhirSearchRequester.searchOrgLocation(new SearchParameterMap("identifier", new TokenParam().setValue(administeredAtLocation)));

			if (orgLocation == null) {
				if (processingFlavorSet.contains(ProcessingFlavor.PEAR)) {
					throw new ProcessingException("Unrecognized administered at location, unable to accept immunization report", "RXA", rxaCount, fieldNum);
				}
				orgLocation = new OrgLocation();
				orgLocation.setOrgFacilityCode(administeredAtLocation);
				orgLocation.setTenant(tenant);
				orgLocation.setOrgFacilityName(administeredAtLocation);
				orgLocation.setLocationType("");
				orgLocation.setAddressLine1(reader.getValue(fieldNum, 9));
				orgLocation.setAddressLine2(reader.getValue(fieldNum, 10));
				orgLocation.setAddressCity(reader.getValue(fieldNum, 11));
				orgLocation.setAddressState(reader.getValue(fieldNum, 12));
				orgLocation.setAddressZip(reader.getValue(fieldNum, 13));
				orgLocation.setAddressCountry(reader.getValue(fieldNum, 14));
				orgLocation = fhirSaveRequester.saveOrgLocation(orgLocation);
			}
		}
		return orgLocation;
	}

	public ModelPerson processPersonPractitioner(HL7Reader reader, Tenant tenant, int fieldNum) {
		ModelPerson modelPerson = null;
		String administeringProvider = reader.getValue(fieldNum);
		if (StringUtils.isNotEmpty(administeringProvider)) {
			modelPerson = fhirSearchRequester.searchPractitioner(new SearchParameterMap("identifier", new TokenParam().setValue(administeringProvider)));
//								Practitioner.IDENTIFIER.exactly().code(administeringProvider));
			if (modelPerson == null) {
				modelPerson = new ModelPerson();
				modelPerson.setPersonExternalLink(administeringProvider);
				modelPerson.setTenant(tenant);
				modelPerson.setNameLast(reader.getValue(fieldNum, 2));
				modelPerson.setNameFirst(reader.getValue(fieldNum, 3));
				modelPerson.setNameMiddle(reader.getValue(fieldNum, 4));
				modelPerson.setAssigningAuthority(reader.getValue(fieldNum, 9));
				modelPerson.setNameTypeCode(reader.getValue(fieldNum, 10));
				modelPerson.setIdentifierTypeCode(reader.getValue(fieldNum, 13));
				modelPerson.setProfessionalSuffix(reader.getValue(fieldNum, 21));
//					  Person  p = PersonMapper.getFhirPerson(modelPerson);
				modelPerson = fhirSaveRequester.savePractitioner(modelPerson);
			}
		}
		return modelPerson;
	}


	public ObservationReported readObservations(HL7Reader reader, List<IisReportable> iisReportableList, PatientReported patientReported, boolean strictDate, int obxCount, VaccinationReported vaccinationReported, VaccinationMaster vaccination) {
		ObservationReported observationReported;
		if (vaccination == null) {
			observationReported = fhirSearchRequester.searchObservationReported(new SearchParameterMap("part-of", new ReferenceParam().setMissing(true))
				.add("subject", new ReferenceParam(patientReported.getPatientId())));
		} else {
			observationReported = fhirSearchRequester.searchObservationReported(new SearchParameterMap("part-of", new ReferenceParam(vaccination.getVaccinationId())).add("subject", new ReferenceParam(patientReported.getPatientId())));
		}
		if (observationReported == null) {
			observationReported = new ObservationReported();
			observationReported.setReportedDate(new Date());
		}

		observationReported.setPatientReportedId(patientReported.getPatientId());
		if (vaccinationReported != null) {
			observationReported.setVaccinationReportedId(vaccinationReported.getVaccinationId());
		}
		observationReported.setUpdatedDate(new Date());
		observationReported.setIdentifierCode(reader.getValue(3));
		observationReported.setValueType(reader.getValue(2));
		observationReported.setIdentifierLabel(reader.getValue(3, 2));
		observationReported.setIdentifierTable(reader.getValue(3, 3));
		observationReported.setValueCode(reader.getValue(5));
		observationReported.setValueLabel(reader.getValue(5, 2));
		observationReported.setValueTable(reader.getValue(5, 3));
		observationReported.setUnitsCode(reader.getValue(6, 1));
		observationReported.setUnitsLabel(reader.getValue(6, 2));
		observationReported.setUnitsTable(reader.getValue(6, 3));
		observationReported.setResultStatus(reader.getValue(11));
		observationReported.setObservationDate(IIncomingMessageHandler.parseDateWarn(reader.getValue(14), "Unparsable date/time of observation", OBX, obxCount, 14, strictDate, iisReportableList));
		observationReported.setMethodCode(reader.getValue(17, 1));
		observationReported.setMethodLabel(reader.getValue(17, 2));
		observationReported.setMethodTable(reader.getValue(17, 3));

		for (int i = 1; i <= reader.getRepeatCount(21); i++) {
			String value = reader.getValueRepeat(21, 1, i);
			String system = reader.getValueRepeat(21, 3, i);
			String type = reader.getValueRepeat(21, 4, i);
			BusinessIdentifier businessIdentifier = new BusinessIdentifier();
			boolean valueChanged = false;
			if (StringUtils.isNotBlank(value)) {
				businessIdentifier.setValue(value);
				valueChanged = true;
			}
			if (StringUtils.isNotBlank(system)) {
				businessIdentifier.setSystem(system);
				valueChanged = true;
			}
			if (StringUtils.isNotBlank(type)) {
				businessIdentifier.setType(type);
				valueChanged = true;
			}
			if (valueChanged) {
				observationReported.addBusinessIdentifier(businessIdentifier);
			}
		}
		// TODO OBX-21 Business identifier
		return observationReported;
	}


	public MqeMessageServiceResponse validation(String message, List<IisReportable> iisReportableList) throws Exception {
		List<IisReportable> nistReportables = validationService.nistValidation(message, VXU);
		iisReportableList.addAll(nistReportables);
		return validationService.getMqeMessageService().processMessage(message);
	}

	public HL7Reader parseSource(String message) {
		return new HL7Reader(message);
	}
}
