package org.immregistries.iis.kernal.logic;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.nist.validation.report.Entry;
import gov.nist.validation.report.Report;
import hl7.v2.profile.Profile;
import hl7.v2.profile.XMLDeserializer;
import hl7.v2.validation.SyncHL7Validator;
import hl7.v2.validation.content.ConformanceContext;
import hl7.v2.validation.content.DefaultConformanceContext;
import hl7.v2.validation.vs.ValueSetLibrary;
import hl7.v2.validation.vs.ValueSetLibraryImpl;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r5.model.Practitioner;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.SoftwareVersion;
import org.immregistries.iis.kernal.fhir.interceptors.PartitionCreationInterceptor;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.logic.ack.*;
import org.immregistries.iis.kernal.logic.logicInterceptors.ImmunizationProcessingInterceptor;
import org.immregistries.iis.kernal.logic.logicInterceptors.ObservationProcessingInterceptor;
import org.immregistries.iis.kernal.logic.logicInterceptors.PatientProcessingInterceptor;
import org.immregistries.iis.kernal.mapping.interfaces.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.interfaces.LocationMapper;
import org.immregistries.iis.kernal.mapping.interfaces.ObservationMapper;
import org.immregistries.iis.kernal.mapping.interfaces.PatientMapper;
import org.immregistries.iis.kernal.mapping.internalClient.FhirRequester;
import org.immregistries.iis.kernal.mapping.internalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.mqe.hl7util.ReportableSource;
import org.immregistries.mqe.hl7util.SeverityLevel;
import org.immregistries.mqe.hl7util.model.CodedWithExceptions;
import org.immregistries.mqe.hl7util.model.Hl7Location;
import org.immregistries.mqe.validator.MqeMessageService;
import org.immregistries.mqe.validator.MqeMessageServiceResponse;
import org.immregistries.mqe.validator.engine.ValidationRuleResult;
import org.immregistries.mqe.vxu.MqeMessageHeader;
import org.immregistries.smm.tester.manager.HL7Reader;
import org.immregistries.vfa.connect.ConnectFactory;
import org.immregistries.vfa.connect.ConnectorInterface;
import org.immregistries.vfa.connect.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public abstract class IncomingMessageHandler implements IIncomingMessageHandler {
	public static final int NAME_SIZE_LIMIT = 15;
	public static final String RSP_K_11_RSP_K_11 = "RSP^K11^RSP_K11";
	public static final String MATCH = "Match";
	public static final String NO_MATCH = "No Match";
	public static final String POSSIBLE_MATCH = "Possible Match";
	public static final String TOO_MANY_MATCHES = "Too Many Matches";
	protected final Logger logger = LoggerFactory.getLogger(IncomingMessageHandler.class);
	/**
	 * DYNAMIC VALUE SETS for validation
	 */
	public MqeMessageService mqeMessageService;
	protected Session dataSession;
	@Autowired
	RepositoryClientFactory repositoryClientFactory;
	@Autowired
	FhirRequester fhirRequester;
	@Autowired
	Hl7MessageWriter hl7MessageWriter;
	@Autowired
	PatientMapper<IBaseResource> patientMapper;
	@Autowired
	ImmunizationMapper<IBaseResource> immunizationMapper;
	@Autowired
	ObservationMapper<IBaseResource> observationMapper;
	@Autowired
	LocationMapper<IBaseResource> locationMapper;
	@Autowired
	PartitionCreationInterceptor partitionCreationInterceptor;

	@Autowired
	PatientProcessingInterceptor patientProcessingInterceptor; // TODO decide how/where to implement the execution of interceptors, currently using DAO so some interceptors are skipped by the v2 process and need to be manually triggered
	@Autowired
	ObservationProcessingInterceptor observationProcessingInterceptor;
	@Autowired
	ImmunizationProcessingInterceptor immunizationProcessingInterceptor;

	SyncHL7Validator syncHL7ValidatorVxuZ22;
	SyncHL7Validator syncHL7ValidatorQbpZ34;
	SyncHL7Validator syncHL7ValidatorQbpZ44;

	public IncomingMessageHandler() {
		mqeMessageService = MqeMessageService.INSTANCE;
		dataSession = ServletHelper.getDataSession();

		{
			InputStream profileXML = IncomingMessageHandler.class.getResourceAsStream("/export/VXU-Z22_Profile.xml");
			InputStream constraintsXML = IncomingMessageHandler.class.getResourceAsStream("/export/VXU-Z22_Constraints.xml");
			InputStream vsLibraryXML = IncomingMessageHandler.class.getResourceAsStream("/export/VXU-Z22_ValueSetLibrary.xml");

			Profile profile = XMLDeserializer.deserialize(profileXML).get();
			ValueSetLibrary valueSetLibrary = ValueSetLibraryImpl.apply(vsLibraryXML).get();
			ConformanceContext conformanceContext = DefaultConformanceContext.apply(Collections.singletonList(constraintsXML)).get();
			syncHL7ValidatorVxuZ22 = new SyncHL7Validator(profile, valueSetLibrary, conformanceContext);
		}

		{
			InputStream profileXML = IncomingMessageHandler.class.getResourceAsStream("/export/QBP-Z34_Profile.xml");
			InputStream constraintsXML = IncomingMessageHandler.class.getResourceAsStream("/export/QBP-Z34_Constraints.xml");
			InputStream vsLibraryXML = IncomingMessageHandler.class.getResourceAsStream("/export/QBP-Z34_ValueSetLibrary.xml");

			Profile profile = XMLDeserializer.deserialize(profileXML).get();
			ValueSetLibrary valueSetLibrary = ValueSetLibraryImpl.apply(vsLibraryXML).get();
			ConformanceContext conformanceContext = DefaultConformanceContext.apply(Collections.singletonList(constraintsXML)).get();
			syncHL7ValidatorQbpZ34 = new SyncHL7Validator(profile, valueSetLibrary, conformanceContext);
		}

		{
			InputStream profileXML = IncomingMessageHandler.class.getResourceAsStream("/export/QBP-Z44_Profile.xml");
			InputStream constraintsXML = IncomingMessageHandler.class.getResourceAsStream("/export/QBP-Z44_Constraints.xml");
			InputStream vsLibraryXML = IncomingMessageHandler.class.getResourceAsStream("/export/QBP-Z44_ValueSetLibrary.xml");

			Profile profile = XMLDeserializer.deserialize(profileXML).get();
			ValueSetLibrary valueSetLibrary = ValueSetLibraryImpl.apply(vsLibraryXML).get();
			ConformanceContext conformanceContext = DefaultConformanceContext.apply(Collections.singletonList(constraintsXML)).get();
			syncHL7ValidatorQbpZ44 = new SyncHL7Validator(profile, valueSetLibrary, conformanceContext);
		}
	}

	public void verifyNoErrors(List<IisReportable> iisReportableList) throws ProcessingException {
		for (IisReportable reportable : iisReportableList) {
			if (reportable.getSeverity().equals(IisReportableSeverity.ERROR)) {
				throw ProcessingException.fromIisReportable(reportable);
			}
		}
	}


	public boolean hasErrors(List<IisReportable> reportables) {
		for (IisReportable reportable : reportables) {
			if (reportable.getSeverity().equals(IisReportableSeverity.ERROR)) {
				return true;
			}
		}
		return false;
	}

	public void recordMessageReceived(String message, PatientMaster patient, String messageResponse, String categoryRequest, String categoryResponse, Tenant tenant) {
		MessageReceived messageReceived = new MessageReceived();
		messageReceived.setTenant(tenant);
		messageReceived.setMessageRequest(message);
		if (patient != null) {
			messageReceived.setPatientReportedId(patient.getPatientId());
		}
		messageReceived.setMessageResponse(messageResponse);
		messageReceived.setReportedDate(new Date());
		messageReceived.setCategoryRequest(categoryRequest);
		messageReceived.setCategoryResponse(categoryResponse);
		// TODO interact with internal logs and metadata
		Transaction transaction = dataSession.beginTransaction();
		dataSession.save(messageReceived);
		transaction.commit();
	}

	public List<ForecastActual> doForecast(PatientMaster patient, CodeMap codeMap, List<VaccinationMaster> vaccinationMasterList, Tenant tenant) {
		List<ForecastActual> forecastActualList = null;
		Set<ProcessingFlavor> processingFlavorSet = tenant.getProcessingFlavorSet();
		try {
			TestCase testCase = new TestCase();
			testCase.setEvalDate(new Date());
			if (patient != null) {
				testCase.setPatientSex(patient.getSex());
				testCase.setPatientDob(patient.getBirthDate());
			} else {
				testCase.setPatientSex("F");
			}
			List<TestEvent> testEventList = new ArrayList<>();
			for (VaccinationMaster vaccination : vaccinationMasterList) {
				Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, vaccination.getVaccineCvxCode());
				if (cvxCode == null) {
					continue;
				}
				if ("D".equals(vaccination.getActionCode())) {
					continue;
				}
				int cvx;
				try {
					cvx = Integer.parseInt(vaccination.getVaccineCvxCode());
					TestEvent testEvent = new TestEvent(cvx, vaccination.getAdministeredDate());
					testEventList.add(testEvent);
					vaccination.setTestEvent(testEvent);
				} catch (NumberFormatException ignored) {
				}
			}
			testCase.setTestEventList(testEventList);
			Software software = new Software();
			software.setServiceUrl("https://sabbia.westus2.cloudapp.azure.com/lonestar/forecast");
			software.setService(Service.LSVF);
			if (processingFlavorSet.contains(ProcessingFlavor.ICE)) {
				software.setServiceUrl("https://sabbia.westus2.cloudapp.azure.com/opencds-decision-support-service/evaluate");
				software.setService(Service.ICE);
			}

			ConnectorInterface connector = ConnectFactory.createConnecter(software, VaccineGroup.getForecastItemList());
			connector.setLogText(false);
			try {
				forecastActualList = connector.queryForForecast(testCase, new SoftwareResult());
			} catch (IOException ioe) {
				System.err.println("Unable to query for forecast");
				ioe.printStackTrace();
			}
		} catch (Exception e) {
			System.err.println("Unable to query for forecast");
			e.printStackTrace(System.err);
		}
		return forecastActualList;
	}

	public String buildAck(HL7Reader reader, List<IisReportable> iisReportableList, Set<ProcessingFlavor> processingFlavorSet) {
		StringBuilder sb = new StringBuilder();
		{
			String messageType = "ACK^V04^ACK";
			String profileId = Z23_ACKNOWLEDGEMENT;
			hl7MessageWriter.createMSH(messageType, profileId, reader, sb, processingFlavorSet);
		}

		// if processing flavor contains MEDLAR then all the non E errors have to removed from the processing list
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
		if (reader.advanceToSegment("MSH")) {
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

	public String buildAckMqe(HL7Reader reader, MqeMessageServiceResponse mqeMessageServiceResponse, List<IisReportable> iisReportableList, Set<ProcessingFlavor> processingFlavorSet, List<IisReportable> validatorReportables) {
		IisAckBuilder ackBuilder = IisAckBuilder.INSTANCE;
		IisAckData data = new IisAckData();
		MqeMessageHeader header = mqeMessageServiceResponse.getMessageObjects().getMessageHeader();
		String profileId = header.getMessageProfile();
		reader.resetPostion();
		reader.advanceToSegment("MSH");
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
		List<IisReportable> reportables = new ArrayList<>(validatorReportables);
		reportables.addAll(iisReportableList);
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
		receivingApp.append(" v" + SoftwareVersion.VERSION);
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
		data.setReceivingFacility(ServletHelper.getTenant().getOrganizationName());

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

	public Date parseDateWarn(String dateString, String errorMessage, String segmentId, int segmentRepeat, int fieldPosition, boolean strict, List<IisReportable> iisReportableList) {
		try {
			return parseDateInternal(dateString, strict);
		} catch (ParseException e) {
			if (errorMessage != null) {
				ProcessingException pe = new ProcessingException(errorMessage + ": " + e.getMessage(), segmentId, segmentRepeat, fieldPosition, IisReportableSeverity.WARN);
				iisReportableList.add(IisReportable.fromProcessingException(pe));
			}
		}
		return null;
	}

	public Date parseDateError(String dateString, String errorMessage, String segmentId, int segmentRepeat, int fieldPosition, boolean strict) throws ProcessingException {
		try {
			Date date = parseDateInternal(dateString, strict);
			if (date == null) {
				if (errorMessage != null) {
					throw new ProcessingException(errorMessage + ": No date was specified", segmentId, segmentRepeat, fieldPosition);
				}
			}
			return date;
		} catch (ParseException e) {
			if (errorMessage != null) {
				throw new ProcessingException(errorMessage + ": " + e.getMessage(), segmentId, segmentRepeat, fieldPosition);
			}
		}
		return null;
	}

	public Date parseDateInternal(String dateString, boolean strict) throws ParseException {
		if (StringUtils.isBlank(dateString)) {
			return null;
		}
		Date date;
		if (dateString.length() > 8) {
			dateString = dateString.substring(0, 8);
		}
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
		simpleDateFormat.setLenient(!strict);
		date = simpleDateFormat.parse(dateString);
		return date;
	}

	public String processQBP(Tenant tenant, HL7Reader reader, String messageReceived) throws Exception {
		Set<ProcessingFlavor> processingFlavorSet = tenant.getProcessingFlavorSet();
		MqeMessageServiceResponse mqeMessageServiceResponse = mqeMessageService.processMessage(messageReceived);
		List<IisReportable> reportables = nistValidation(messageReceived, mqeMessageServiceResponse.getMessageObjects().getMessageHeader().getMessageProfile());
		PatientMaster patientMasterForMatchQuery = new PatientMaster();
		if (reader.advanceToSegment("QPD")) {
			String mrn = "";
//			for (int i = 1; i <= reader.getRepeatCount(3); i++) {
//
//			}
			{
				mrn = reader.getValueBySearchingRepeats(3, 1, "MR", 5);
				if (StringUtils.isBlank(mrn)) {
					mrn = reader.getValueBySearchingRepeats(3, 1, "PT", 5);
				}
			}
			String problem = null;
			int fieldPosition = 0;
			if (StringUtils.isNotBlank(mrn)) {
				BusinessIdentifier businessIdentifier = new BusinessIdentifier();
				businessIdentifier.setValue(mrn);
				patientMasterForMatchQuery.addBusinessIdentifier(businessIdentifier);// TODO system
//				patientMasterForMatchQuery.setExternalLink(mrn); // TODO system
//				patientReported = fhirRequester.searchPatientReported(
//					Patient.IDENTIFIER.exactly().systemAndCode(MRN_SYSTEM, mrn)
//				);
			}
			String patientNameLast = reader.getValue(4, 1);
			String patientNameFirst = reader.getValue(4, 2);
			String patientNameMiddle = reader.getValue(4, 3);

			if (processingFlavorSet.contains(ProcessingFlavor.MOONFRUIT) && StringUtils.defaultString(patientNameFirst).startsWith("S") || StringUtils.defaultString(patientNameFirst).startsWith("A")) {
				throw new ProcessingException("Immunization History cannot be Accepted because of patient's consent status", "PID", 0, 0, IisReportableSeverity.WARN);
			}
			boolean strictDate = false;

			Date patientBirthDate = parseDateWarn(reader.getValue(6), "Invalid patient birth date", "QPD", 1, 6, strictDate, reportables);
			String patientSex = reader.getValue(7);

			if (StringUtils.isBlank(patientNameLast)) {
				problem = "Last name is missing";
				fieldPosition = 4;
			} else if (StringUtils.isBlank(patientNameFirst)) {
				problem = "First name is missing";
				fieldPosition = 4;
			} else if (patientBirthDate == null) {
				problem = "Date of Birth is missing";
				fieldPosition = 6;
			}
			if (StringUtils.isNotBlank(problem)) {
				reportables.add(IisReportable.fromProcessingException(new ProcessingException(problem, "QPD", 1, fieldPosition)));
			} else {
				PatientName patientName = new PatientName(patientNameLast, patientNameFirst, patientNameMiddle, "");
				patientMasterForMatchQuery.addPatientName(patientName);
				patientMasterForMatchQuery.setBirthDate(patientBirthDate);
			}
		} else {
			reportables.add(IisReportable.fromProcessingException(new ProcessingException("QPD segment not found", null, 0, 0)));
		}

		Date cutoff = null;
		if (processingFlavorSet.contains(ProcessingFlavor.SNAIL) || processingFlavorSet.contains(ProcessingFlavor.SNAIL30) || processingFlavorSet.contains(ProcessingFlavor.SNAIL60) || processingFlavorSet.contains(ProcessingFlavor.SNAIL90)) {
			Calendar calendar = Calendar.getInstance();
			int seconds = -30;
			if (processingFlavorSet.contains(ProcessingFlavor.SNAIL30)) {
				seconds = -30;
			} else if (processingFlavorSet.contains(ProcessingFlavor.SNAIL60)) {
				seconds = -60;
			} else if (processingFlavorSet.contains(ProcessingFlavor.SNAIL90)) {
				seconds = -90;
			} else {
				int delay = calendar.get(Calendar.MINUTE) % 4;
				seconds = delay * -30;
			}

			calendar.add(Calendar.SECOND, seconds);
			cutoff = calendar.getTime();

//			if (patientReported != null) { TODO map this to FHIR
//				if (cutoff.before(patientReported.getReportedDate())) {
//					patientReported = null;
//				}
//			}

//			for (Iterator<PatientReported> it = patientReportedPossibleList.iterator(); it.hasNext(); ) {
//				PatientReported pr = it.next();
//				if (cutoff.before(pr.getReportedDate())) {
//					it.remove();
//				}
//			}
		}
		List<PatientReported> multipleMatches = new ArrayList<>();
		PatientMaster singleMatch = fhirRequester.matchPatient(multipleMatches, patientMasterForMatchQuery, cutoff);
		if (singleMatch == null) {
			throw new ProcessingException("Patient not found", "PID", 1, 1); // TODO position
		}

		return buildRSP(reader, messageReceived, singleMatch, tenant, multipleMatches, reportables);
	}

	@SuppressWarnings("unchecked")
	public String buildRSP(HL7Reader reader, String messageReceived, PatientMaster patientMaster, Tenant tenant, List<PatientReported> patientReportedPossibleList, List<IisReportable> iisReportables) {
		Set<ProcessingFlavor> processingFlavorSet = tenant.getProcessingFlavorSet();
		MqeMessageServiceResponse mqeMessageServiceResponse = mqeMessageService.processMessage(messageReceived);
		boolean sendInformations = true;
		if (processingFlavorSet.contains(ProcessingFlavor.STARFRUIT) && StringUtils.defaultString(patientMaster.getNameFirst()).startsWith("S") || StringUtils.defaultString(patientMaster.getNameFirst()).startsWith("A")) {
			iisReportables.add(IisReportable.fromProcessingException(new ProcessingException("Immunization History cannot be shared because of patient's consent status", "PID", 0, 0, IisReportableSeverity.NOTICE)));
			sendInformations = false;
		}
		IGenericClient fhirClient = repositoryClientFactory.getFhirClient();
		reader.resetPostion();
		reader.advanceToSegment("MSH");

		StringBuilder sb = new StringBuilder();
		String profileIdSubmitted = reader.getValue(21);
		CodeMap codeMap = CodeMapManager.getCodeMap();
		String categoryResponse = NO_MATCH;
		String profileId = RSP_Z33_NO_MATCH;
		boolean sendBackForecast = true;
		if (processingFlavorSet.contains(ProcessingFlavor.COCONUT)) {
			sendBackForecast = false;
		} else if (processingFlavorSet.contains(ProcessingFlavor.ORANGE)) {
			sendBackForecast = false;
		}

		String queryId = "";
		int maxCount = 20;
		if (reader.advanceToSegment("QPD")) {
			queryId = reader.getValue(2);
			if (reader.advanceToSegment("RCP")) {
				String s = reader.getValue(2);
				try {
					int i = Integer.parseInt(s);
					if (i < maxCount) {
						maxCount = i;
					}
				} catch (NumberFormatException nfe) {
					// ignore
				}
			}
		}
		String queryResponse = QUERY_OK;
		{
			if (patientMaster == null) {
				queryResponse = QUERY_NOT_FOUND;
				profileId = RSP_Z33_NO_MATCH;
				categoryResponse = NO_MATCH;
				if (!patientReportedPossibleList.isEmpty()) {
					if (profileIdSubmitted.equals(QBP_Z34)) {
						if (patientReportedPossibleList.size() > maxCount) {
							queryResponse = QUERY_TOO_MANY;
							profileId = RSP_Z33_NO_MATCH;
							categoryResponse = TOO_MANY_MATCHES;
						} else {
							queryResponse = QUERY_OK;
							profileId = RSP_Z31_MULTIPLE_MATCH;
							categoryResponse = POSSIBLE_MATCH;
						}
					} else if (profileIdSubmitted.equals("Z44")) {
						queryResponse = QUERY_NOT_FOUND;
						profileId = RSP_Z33_NO_MATCH;
						categoryResponse = NO_MATCH;
					}
				}
				if (hasErrors(iisReportables)) {
					queryResponse = QUERY_APPLICATION_ERROR;
				}
			} else if (profileIdSubmitted.equals(QBP_Z34)) {
				profileId = RSP_Z32_MATCH;
				categoryResponse = MATCH;
			} else if (profileIdSubmitted.equals(QBP_Z44)) {
				if (processingFlavorSet.contains(ProcessingFlavor.ORANGE)) {
					profileId = RSP_Z32_MATCH;
					categoryResponse = MATCH;
				} else {
					sendBackForecast = true;
					profileId = RSP_Z42_MATCH_WITH_FORECAST;
					categoryResponse = MATCH;
				}
			} else {
				iisReportables.add(IisReportable.fromProcessingException(new ProcessingException("Unrecognized profile id '" + profileIdSubmitted + "'", "MSH", 1, 21)));
			}
			// TODO remove notices ?
			hl7MessageWriter.createMSH(RSP_K_11_RSP_K_11, profileId, reader, sb, processingFlavorSet);
		}

		{
			String sendersUniqueId = reader.getValue(10);
			IisHL7Util.makeMsaAndErr(sb, sendersUniqueId, profileId, profileId, iisReportables, processingFlavorSet);
//			if (hasErrors(iisReportables)) {
//				sb.append("MSA|AE|").append(sendersUniqueId).append("\r");
//			} else {
//				sb.append("MSA|AA|").append(sendersUniqueId).append("\r");
//			}
//			if (iisReportables.size() > 0) {
//				for (IisReportable iisReportable: iisReportables) {
//					sb.append(IisHL7Util.makeERRSegment(iisReportable, false));
//				}
//				sb.append(IisHL7Util.makeERRSegment(IisReportable.fromProcessingException(iisReportableList.get(iisReportableList.size() - 1)), false));
//			}
		}
		if (sendInformations) {
			String profileName = "Request a Complete Immunization History";
			if (StringUtils.isBlank(profileIdSubmitted)) {
				profileIdSubmitted = "Z34";
				profileName = "Request a Complete Immunization History";
			} else if (profileIdSubmitted.equals("Z34")) {
				profileName = "Request a Complete Immunization History";
			} else if (profileIdSubmitted.equals("Z44")) {
				profileName = "Request Evaluated Immunization History and Forecast Query";
			}
			{
				sb.append("QAK|").append(queryId);
				sb.append("|").append(queryResponse);
				sb.append("|");
				sb.append(profileIdSubmitted).append("^").append(profileName).append("^CDCPHINVS\r");
			}
			reader.resetPostion();
			if (reader.advanceToSegment("QPD")) {
				sb.append(reader.getOriginalSegment()).append("\r");
			} else {
				sb.append("QPD|");
			}
			if (profileId.equals(RSP_Z31_MULTIPLE_MATCH)) {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
				int count = 0;
				for (PatientReported pr : patientReportedPossibleList) {
					count++;
					PatientMaster patient = pr.getPatient();
					hl7MessageWriter.printQueryPID(pr, processingFlavorSet, sb, patient, sdf, count);
				}
			} else if (profileId.equals(RSP_Z32_MATCH) || profileId.equals(RSP_Z42_MATCH_WITH_FORECAST)) {
				/**
				 * CONFUSING naming p but no better solution right now but to deal with single match
				 */
				PatientMaster patient = patientMaster;
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
				hl7MessageWriter.printQueryPID(patientMaster, processingFlavorSet, sb, patient, sdf, 1);
				if (profileId.equals(RSP_Z32_MATCH)) {
					hl7MessageWriter.printQueryNK1(patientMaster, sb, codeMap);
				}
				List<VaccinationMaster> vaccinationMasterList = getVaccinationMasterList(patientMaster);

				if (processingFlavorSet.contains(ProcessingFlavor.LEMON)) {
					for (Iterator<VaccinationMaster> it = vaccinationMasterList.iterator(); it.hasNext(); ) {
						it.next();
						if (random.nextInt(4) == 0) {
							it.remove();
						}
					}
				}
				if (processingFlavorSet.contains(ProcessingFlavor.GREEN)) {
					vaccinationMasterList.removeIf(vaccinationMaster -> vaccinationMaster.getVaccineCvxCode().equals("91"));
				}
				List<ForecastActual> forecastActualList = null;
				if (sendBackForecast) {
					forecastActualList = doForecast(patientMaster, codeMap, vaccinationMasterList, tenant);
				}
				int obxSetId = 0;
				int obsSubId = 0;
				for (VaccinationMaster vaccination : vaccinationMasterList) {
					Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, vaccination.getVaccineCvxCode());
					if (cvxCode == null) {
						continue;
					}
					boolean originalReporter = vaccination.getPatientReported().getTenant().equals(tenant);
					if ("D".equals(vaccination.getActionCode())) {
						continue;
					}
					hl7MessageWriter.printORC(tenant, sb, vaccination, originalReporter);
					sb.append("RXA");
					// RXA-1
					sb.append("|0");
					// RXA-2
					sb.append("|1");
					String adminDate = sdf.format(vaccination.getAdministeredDate());
					if (obxSetId == 0 && processingFlavorSet.contains(ProcessingFlavor.CHERRY)) {
						adminDate = "";
					}
					// RXA-3
					sb.append("|").append(adminDate);
					// RXA-4
					sb.append("|");
					// RXA-5
					sb.append("|").append(cvxCode.getValue()).append("^").append(cvxCode.getLabel()).append("^CVX");
					if (StringUtils.isNotBlank(vaccination.getVaccineNdcCode())) {
						Code ndcCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_NDC_CODE, vaccination.getVaccineNdcCode());
						if (ndcCode != null) {
							sb.append("~").append(ndcCode.getValue()).append("^").append(ndcCode.getLabel()).append("^NDC");
						}
					}
					{
						// RXA-6
						sb.append("|");
						double adminAmount = 0.0;
						if (StringUtils.isNotBlank(vaccination.getAdministeredAmount())) {
							try {
								adminAmount = Double.parseDouble(vaccination.getAdministeredAmount());
							} catch (NumberFormatException nfe) {
								adminAmount = 0.0;
							}
						}
						if (adminAmount > 0) {
							if (adminAmount == 999.0) {
								sb.append("999");
							} else {
								sb.append(adminAmount);
							}
						}
						// RXA-7
						sb.append("|");
						if (adminAmount > 0) {
							sb.append("mL^milliliters^UCUM");
						}
					}
					// RXA-8
					sb.append("|");
					// RXA-9
					sb.append("|");
					{
						Code informationCode = null;
						if (vaccination.getInformationSource() != null) {
							informationCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_INFORMATION_SOURCE, vaccination.getInformationSource());
						}
						if (informationCode != null) {
							sb.append(informationCode.getValue()).append("^").append(informationCode.getLabel()).append("^NIP001");
						}
					}
					// RXA-10
					sb.append("|");
					// RXA-11
					sb.append("|");
					if (vaccination.getOrgLocation() == null || vaccination.getOrgLocation().getOrgFacilityCode() == null || "".equals(vaccination.getOrgLocation().getOrgFacilityCode())) {
					} else {
						sb.append("^^^");
						sb.append(vaccination.getOrgLocation().getOrgFacilityCode());
					}
					// RXA-12
					sb.append("|");
					// RXA-13
					sb.append("|");
					// RXA-14
					sb.append("|");
					// RXA-15
					sb.append("|");
					if (vaccination.getLotnumber() != null) {
						sb.append(vaccination.getLotnumber());
					}
					// RXA-16
					sb.append("|");
					if (vaccination.getExpirationDate() != null) {
						sb.append(sdf.format(vaccination.getExpirationDate()));
					}
					// RXA-17
					sb.append("|");
					sb.append(hl7MessageWriter.printCode(vaccination.getVaccineMvxCode(), CodesetType.VACCINATION_MANUFACTURER_CODE, "MVX", codeMap));
					// RXA-18
					sb.append("|");
					sb.append(hl7MessageWriter.printCode(vaccination.getRefusalReasonCode(), CodesetType.VACCINATION_REFUSAL, "NIP002", codeMap));
					// RXA-19
					sb.append("|");
					// RXA-20
					sb.append("|");
					if (!processingFlavorSet.contains(ProcessingFlavor.LIME)) {
						String completionStatus = vaccination.getCompletionStatus();
						if (StringUtils.isBlank(completionStatus)) {
							completionStatus = "CP";
						}
						sb.append(hl7MessageWriter.printCode(completionStatus, CodesetType.VACCINATION_COMPLETION, null, codeMap));
					}

					// RXA-21
					sb.append("|A");
					sb.append("\r");
					if (StringUtils.isNotBlank(vaccination.getBodyRoute())) {
						sb.append("RXR");
						// RXR-1
						sb.append("|");
						sb.append(hl7MessageWriter.printCode(vaccination.getBodyRoute(), CodesetType.BODY_ROUTE, "NCIT", codeMap));
						// RXR-2
						sb.append("|");
						sb.append(hl7MessageWriter.printCode(vaccination.getBodySite(), CodesetType.BODY_SITE, "HL70163", codeMap));
						sb.append("\r");
					}
					TestEvent testEvent = vaccination.getTestEvent();
					if (testEvent != null && testEvent.getEvaluationActualList() != null) {
						for (EvaluationActual evaluationActual : testEvent.getEvaluationActualList()) {
							obsSubId++;
							{
								obxSetId++;
								String loinc = "30956-7";
								String loincLabel = "Vaccine type";
								String value = evaluationActual.getVaccineCvx();
								if (processingFlavorSet.contains(ProcessingFlavor.KUMQUAT)) {
									if (value.length() > 2) {
										value = "BADCVX";
									}
								}
								String valueLabel = evaluationActual.getVaccineCvx();
								String valueTable = "CVX";
								hl7MessageWriter.printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value, valueLabel, valueTable);
							}
							{
								obxSetId++;
								String loinc = "59781-5";
								String loincLabel = "Dose validity";
								String value = evaluationActual.getDoseValid();
								String valueLabel = value;
								String valueTable = "99107";
								hl7MessageWriter.printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value, valueLabel, valueTable);
							}
						}
					}
					try {
						Bundle bundle = fhirClient.search().forResource(Observation.class).where(Observation.PART_OF.hasId(patientMaster.getPatientId())).and(Observation.PART_OF.hasId(vaccination.getVaccinationId())).returnBundle(Bundle.class).execute();
						if (bundle.hasEntry()) {
							obsSubId++;
							for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
//						ObservationMaster observationMaster = ObservationMapper.getMaster((Observation) entry.getResource());
								ObservationReported observationReported = observationMapper.localObjectReported(entry.getResource());
								obxSetId++;
								hl7MessageWriter.printObx(sb, obxSetId, obsSubId, observationReported);
							}
						}
					} catch (ResourceNotFoundException ignored) {
					}
				}
				try {
					Bundle bundle = fhirClient.search().forResource(Observation.class).where(Observation.PART_OF.hasId(patientMaster.getPatientId())).returnBundle(Bundle.class).execute();
					if (bundle.hasEntry()) {
						hl7MessageWriter.printORC(tenant, sb, null, false);
						obsSubId++;
						for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
							obxSetId++;
							ObservationReported observationReported = observationMapper.localObjectReported(entry.getResource());
							hl7MessageWriter.printObx(sb, obxSetId, obsSubId, observationReported);
						}
					}
				} catch (ResourceNotFoundException ignored) {
				}

				if (sendBackForecast && forecastActualList != null && !forecastActualList.isEmpty()) {
					hl7MessageWriter.printORC(tenant, sb, null, false);
					sb.append("RXA");
					// RXA-1
					sb.append("|0");
					// RXA-2
					sb.append("|1");
					// RXA-3
					sb.append("|" + sdf.format(new Date()));
					// RXA-4
					sb.append("|");
					// RXA-5
					sb.append("|998^No Vaccination Administered^CVX");
					// RXA-6
					sb.append("|999");
					// RXA-7
					sb.append("|");
					// RXA-8
					sb.append("|");
					// RXA-9
					sb.append("|");
					// RXA-10
					sb.append("|");
					// RXA-11
					sb.append("|");
					// RXA-12
					sb.append("|");
					// RXA-13
					sb.append("|");
					// RXA-14
					sb.append("|");
					// RXA-15
					sb.append("|");
					// RXA-16
					sb.append("|");
					// RXA-17
					sb.append("|");
					// RXA-18
					sb.append("|");
					// RXA-19
					sb.append("|");
					// RXA-20
					sb.append("|NA");
					sb.append("\r");
					HashSet<String> cvxAddedSet = new HashSet<String>();
					for (ForecastActual forecastActual : forecastActualList) {
						String cvx = forecastActual.getVaccineGroup().getVaccineCvx();
						if (cvxAddedSet.contains(cvx)) {
							continue;
						}
						cvxAddedSet.add(cvx);
						obsSubId++;
						{
							obxSetId++;
							String loinc = "30956-7";
							String loincLabel = "Vaccine type";
							String value = forecastActual.getVaccineGroup().getVaccineCvx();
							String valueLabel = forecastActual.getVaccineGroup().getLabel();
							String valueTable = "CVX";
							hl7MessageWriter.printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value, valueLabel, valueTable);
						}
						{
							obxSetId++;
							String loinc = "59783-1";
							String loincLabel = "Status in series";
							Admin admin = forecastActual.getAdmin();
							String value = admin.getAdminStatus();
							String valueLabel = admin.getLabel();
							String valueTable = "99106";
							hl7MessageWriter.printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value, valueLabel, valueTable);
						}
						if (forecastActual.getDueDate() != null) {
							obxSetId++;
							String loinc = "30981-5";
							String loincLabel = "Earliest date";
							Date value = forecastActual.getValidDate();
							hl7MessageWriter.printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value);
						}
						if (forecastActual.getDueDate() != null) {
							obxSetId++;
							String loinc = "30980-7";
							String loincLabel = "Recommended date";
							Date value = forecastActual.getDueDate();
							hl7MessageWriter.printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value);
						}
						if (forecastActual.getDueDate() != null) {
							obxSetId++;
							String loinc = "59778-1";
							String loincLabel = "Latest date";
							Date value = forecastActual.getOverdueDate();
							hl7MessageWriter.printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value);
						}
					}
				}
			}
		}

		String messageResponse = sb.toString();
		recordMessageReceived(messageReceived, patientMaster, messageResponse, "Query", categoryResponse, tenant);
		return messageResponse;
	}

	public int readAndCreateObservations(HL7Reader reader, List<IisReportable> iisReportableList, PatientReported patientReported, boolean strictDate, int obxCount, VaccinationReported vaccinationReported, VaccinationMaster vaccination) {
		while (reader.advanceToSegment("OBX", "ORC")) {
			obxCount++;
			String identifierCode = reader.getValue(3);
			String valueCode = reader.getValue(5);
			ObservationReported observationReported = readObservations(reader, iisReportableList, patientReported, strictDate, obxCount, vaccinationReported, vaccination, identifierCode, valueCode);
			// Commented contraindication Now checked in Observation Interceptor
			if (observationReported.getIdentifierCode().equals("30945-0")) // contraindication!
			{
				CodeMap codeMap = CodeMapManager.getCodeMap();
//				Code contraCode = codeMap.getCodeForCodeset(CodesetType.CONTRAINDICATION_OR_PRECAUTION, observationReported.getValueCode());
//				if (contraCode == null) {
//					ProcessingException pe = new ProcessingException("Unrecognized contraindication or precaution", "OBX", obxCount, 5, IisReportableSeverity.WARN);
//					iisReportableList.add(IisReportable.fromProcessingException(pe));
//				}
				if (observationReported.getObservationDate() != null) {
//					Date today = new Date();
//					if (observationReported.getObservationDate().after(today)) {
//						ProcessingException pe = new ProcessingException("Contraindication or precaution observed in the future", "OBX", obxCount, 5, IisReportableSeverity.WARN);
//						iisReportableList.add(IisReportable.fromProcessingException(pe));
//					}
					if (patientReported.getBirthDate() != null && observationReported.getObservationDate().before(patientReported.getBirthDate())) {
						ProcessingException pe = new ProcessingException("Contraindication or precaution observed before patient was born", "OBX", obxCount, 14, IisReportableSeverity.WARN);
						iisReportableList.add(IisReportable.fromProcessingException(pe));
					}
				}
			}
			{
				observationReported.setPatientReportedId(patientReported.getPatientId());
				fhirRequester.saveObservationReported(observationReported);
			}
		}
		return obxCount;
	}


	public List<IisReportable> nistValidation(String message, String profileId) throws Exception {
		String id;
		SyncHL7Validator syncHL7Validator;
		if ("Z34".equals(profileId)) {
			id = "89df2062-96c4-4cbc-9ef2-817e4b4bc4f1";
			syncHL7Validator = syncHL7ValidatorQbpZ34;
		} else if ("Z44".equals(profileId)) {
			id = "b760d322-9afd-439e-96f5-43db66937c4e";
			syncHL7Validator = syncHL7ValidatorQbpZ44;
		} else if ("Z22".equals(profileId)) {
			id = "aa72383a-7b48-46e5-a74a-82e019591fe7";
			syncHL7Validator = syncHL7ValidatorVxuZ22;
		} else {
			id = "aa72383a-7b48-46e5-a74a-82e019591fe7";
			syncHL7Validator = syncHL7ValidatorVxuZ22;
		}
		Report report = syncHL7Validator.check(message, id);
		List<IisReportable> reportableList = new ArrayList<>();
		for (Map.Entry<String, List<Entry>> mapEntry : report.getEntries().entrySet()) {
			for (Entry assertion : mapEntry.getValue()) {
//				logger.info("entry {}", assertion.toText());
				String severity = assertion.getClassification();
				SeverityLevel severityLevel = SeverityLevel.ACCEPT;
				if (severity.equalsIgnoreCase("error")) {
					severityLevel = SeverityLevel.WARN;
				}

				if (severityLevel != SeverityLevel.ACCEPT) {
					IisReportable reportable = new IisReportable();
					reportable.setSource(ReportableSource.NIST);
					reportable.setSeverity(IisReportableSeverity.WARN);
					reportableList.add(reportable);
					reportable.setReportedMessage(assertion.getDescription());
//					reportable.setSeverity(severityLevel);
					reportable.getHl7ErrorCode().setIdentifier("0");
					CodedWithExceptions cwe = new CodedWithExceptions();
					cwe.setAlternateIdentifier(assertion.getCategory());
					cwe.setAlternateText(assertion.getDescription());
					cwe.setNameOfAlternateCodingSystem("L");
					reportable.setApplicationErrorCode(cwe);
					String path = assertion.getPath();
					reportable.setDiagnosticMessage(path);
					this.addErrorLocation(reportable, path);
				}
			}
		}


//		ValidationReport validationReport = new ValidationReport(report.toText());

		return reportableList;
	}

	public void addErrorLocation(IisReportable reportable, String path) {
		if (path != null && path.length() >= 3) {
			String segmentid = path.substring(0, 3);
			if (path.length() > 3) {
				path = path.substring(4);
			} else {
				path = "";
			}

			Hl7Location errorLocation = IisReportable.readErrorLocation(path, segmentid);
			if (errorLocation != null) {
				reportable.getHl7LocationList().add(errorLocation);
			}
		}

	}


	public PatientReported processPatientFhirAgnostic(HL7Reader reader, List<IisReportable> iisReportableList, Set<ProcessingFlavor> processingFlavorSet, CodeMap codeMap, boolean strictDate, PatientReported patientReported) throws ProcessingException {

		List<PatientName> names = new ArrayList<>(reader.getRepeatCount(5));
		for (int i = 1; i <= reader.getRepeatCount(5); i++) {
			String patientNameLast = reader.getValueRepeat(5, 1, i);
			String patientNameFirst = reader.getValueRepeat(5, 2, i);
			String patientNameMiddle = reader.getValueRepeat(5, 3, i);
			String nameType = reader.getValueRepeat(5, 7, i);
			PatientName patientName = new PatientName(patientNameLast, patientNameFirst, patientNameMiddle, nameType);
			names.add(patientName);
		}

		PatientPhone patientPhone = new PatientPhone();
		patientPhone.setNumber(reader.getValue(13, 6) + reader.getValue(13, 7));
		patientPhone.setUse(reader.getValue(13, 2));
		// Logic exported to Patient Processing interceptor
//		if (patientPhone.getNumber().length() > 0) {
//			if (!patientPhone.getUse().equals("PRN")) {
//				ProcessingException pe = new ProcessingException("Patient phone telecommunication type must be PRN ", "PID", 1, 13);
//				if (!processingFlavorSet.contains(ProcessingFlavor.QUINZE)) {
//					pe.setErrorCode(IisReportableSeverity.WARN);
//				}
//				iisReportableList.add(IisReportable.fromProcessingException(pe));
//			}
//
//			{
//				int countNums = 0;
//				boolean invalidCharFound = false;
//				char invalidChar = ' ';
//				for (char c : patientPhone.getNumber().toCharArray()) {
//
//					if (c >= '0' && c <= '9') {
//						countNums++;
//					} else if (c != '-' && c != '.' && c != ' ' && c != '(' && c != ')') {
//						if (!invalidCharFound) {
//							invalidCharFound = true;
//							invalidChar = c;
//						}
//					}
//				}
//				if (invalidCharFound) {
//					ProcessingException pe = new ProcessingException("Patient phone number has unexpected character: " + invalidChar, "PID", 1, 13);
//					pe.setErrorCode(IisReportableSeverity.WARN);
//					iisReportableList.add(IisReportable.fromProcessingException(pe));
//				}
////				if (countNums != 10 || patientPhone.startsWith("555") || patientPhone.startsWith("0") || patientPhone.startsWith("1")) {
////					ProcessingException pe = new ProcessingException("Patient phone number does not appear to be valid", "PID", 1, 13);
////					pe.setErrorCode(IisReportableSeverity.WARN);
////					iisReportableList.add(IisReportable.fromProcessingException(pe));
////				}
//			}
//		}
		if (!patientPhone.getUse().equals("PRN")) {
			patientPhone.setUse("");
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
		PatientAddress patientAddress = new PatientAddress();
		patientAddress.setAddressLine1(reader.getValue(11, 1));
		patientAddress.setAddressLine2(reader.getValue(11, 2));
		patientAddress.setAddressCity(reader.getValue(11, 3));
		patientAddress.setAddressState(reader.getValue(11, 4));
		patientAddress.setAddressZip(reader.getValue(11, 5));
		patientAddress.setAddressCountry(reader.getValue(11, 6));
		patientAddress.setAddressCountyParish(reader.getValue(11, 9));

		Date patientBirthDate;
		patientBirthDate = parseDateError(reader.getValue(7), "Bad format for date of birth", "PID", 1, 7, strictDate);
		if (patientBirthDate.after(new Date())) {
			throw new ProcessingException("Patient is indicated as being born in the future, unable to record patients who are not yet born", "PID", 1, 7);
		}

		for (int i = 1; i <= reader.getRepeatCount(3); i++) {
			BusinessIdentifier businessIdentifier = new BusinessIdentifier();
			businessIdentifier.setValue(reader.getValueRepeat(3, 1, i));
			businessIdentifier.setSystem(reader.getValueRepeat(3, 4, i));
			businessIdentifier.setType(reader.getValueRepeat(3, 5, i));
			patientReported.addBusinessIdentifier(businessIdentifier);
		}

		patientReported.setPatientNames(names);
		patientReported.setMotherMaidenName(reader.getValue(6));
		patientReported.setBirthDate(patientBirthDate);
		patientReported.setSex(reader.getValue(8));
		for (int i = 1; i <= reader.getRepeatCount(10); i++) {
			patientReported.addRace(reader.getValueRepeat(10, 1, i));
		}
		patientReported.addAddress(patientAddress);
		patientReported.setEthnicity(reader.getValue(22));
		patientReported.setBirthFlag(reader.getValue(24));
		patientReported.setBirthOrder(reader.getValue(25));
		patientReported.setDeathDate(parseDateWarn(reader.getValue(29), "Invalid patient death date", "PID", 1, 29, strictDate, iisReportableList));
		patientReported.setDeathFlag(reader.getValue(30));
		patientReported.setEmail(reader.getValueBySearchingRepeats(13, 4, "NET", 2));
		patientReported.addPhone(patientPhone);

		if (reader.advanceToSegment("PD1")) {
			patientReported.setPublicityIndicator(reader.getValue(11));
			patientReported.setProtectionIndicator(reader.getValue(12));
			patientReported.setProtectionIndicatorDate(parseDateWarn(reader.getValue(13), "Invalid protection indicator date", "PD1", 1, 13, strictDate, iisReportableList));
			patientReported.setRegistryStatusIndicator(reader.getValue(16));
			patientReported.setRegistryStatusIndicatorDate(parseDateWarn(reader.getValue(17), "Invalid registry status indicator date", "PD1", 1, 17, strictDate, iisReportableList));
			patientReported.setPublicityIndicatorDate(parseDateWarn(reader.getValue(18), "Invalid publicity indicator date", "PD1", 1, 18, strictDate, iisReportableList));
		}
		reader.resetPostion();
		{
			while (reader.advanceToSegment("NK1")) {
				PatientGuardian patientGuardian = new PatientGuardian();
				patientReported.addPatientGuardian(patientGuardian);
				String guardianLast = reader.getValue(2, 1);
				patientGuardian.getName().setNameLast(guardianLast);
				String guardianFirst = reader.getValue(2, 2);
				patientGuardian.getName().setNameFirst(guardianFirst);
				String guardianMiddle = reader.getValue(2, 1);
				patientGuardian.getName().setNameMiddle(guardianMiddle);
				String guardianRelationship = reader.getValue(3);
				patientGuardian.setGuardianRelationship(guardianRelationship);
			}
		}
		reader.resetPostion();

		patientProcessingInterceptor.processAndValidatePatient(patientReported, iisReportableList, processingFlavorSet);
		verifyNoErrors(iisReportableList);

		patientReported.setUpdatedDate(new Date());
		patientReported = fhirRequester.savePatientReported(patientReported);
//		patientReported = fhirRequester.saveRelatedPerson(patientReported);
		iisReportableList.add(IisReportable.fromProcessingException(new ProcessingException("Patient record saved", "PID", 0, 0, IisReportableSeverity.INFO)));

		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		ArrayList<String> groupPatientIds = (ArrayList<String>) request.getAttribute("groupPatientIds");
		if (groupPatientIds != null) { // If there are numerous patients added and option was activated
			groupPatientIds.add(patientReported.getPatientId());
		}
		request.setAttribute("groupPatientIds", groupPatientIds);


		return patientReported;
	}

	public List<VaccinationReported> processVaccinations(Tenant tenant, HL7Reader reader, List<IisReportable> iisReportableList, PatientReported patientReported, boolean strictDate, CodeMap codeMap, Set<ProcessingFlavor> processingFlavorSet) throws ProcessingException {
		int orcCount = 0;
		int rxaCount = 0;
		int obxCount = 0;
		int vaccinationCount = 0;
		int refusalCount = 0;
		List<VaccinationReported> vaccinationReportedList = new ArrayList<>(4);
		while (reader.advanceToSegment("ORC")) {
			orcCount++;
			VaccinationReported vaccinationReported = null;
			String vaccineCode = "";
			Date administrationDate = null;
			String vaccinationReportedExternalLinkPlacer = reader.getValue(2);
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
			boolean rxaPresent = reader.advanceToSegment("RXA", "ORC");
			if (!rxaPresent) {
				throw new ProcessingException("RXA segment was not found after ORC segment", "ORC", orcCount, 0);
			}
			rxaCount++;
			vaccineCode = reader.getValue(5, 1);
			if (StringUtils.isBlank(vaccineCode)) {
				throw new ProcessingException("Vaccine code is not indicated in RXA-5.1", "RXA", rxaCount, 5);
			}
			if (vaccineCode.equals("998")) {
				obxCount = readAndCreateObservations(reader, iisReportableList, patientReported, strictDate, obxCount, null, null);
				continue;
			}
			if (fillerIdentifier == null) {
				throw new ProcessingException("Vaccination order id was not found, unable to process", "ORC", orcCount, 3);
			}
			administrationDate = parseDateError(reader.getValue(3, 1), "Could not read administered date in RXA-5", "RXA", rxaCount, 3, strictDate);
//			if (administrationDate.after(new Date())) {
//				throw new ProcessingException("Vaccination is indicated as occurring in the future, unable to accept future vaccination events", "RXA", rxaCount, 3);
//			}

			vaccinationReported = fhirRequester.searchVaccinationReported(new SearchParameterMap("identifier", new TokenParam().setValue(fillerIdentifier.getValue()))); // TODO system and identifier type

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
			vaccinationReported.setPatientReportedId(patientReported.getPatientId());
			vaccinationReported.setPatientReported(patientReported);

			String vaccineCvxCode = "";
			String vaccineNdcCode = "";
			String vaccineCptCode = "";
			{
				String vaccineCodeType = reader.getValue(5, 3);
				if (vaccineCodeType.equals("NDC")) {
					vaccineNdcCode = vaccineCode;
				} else if (vaccineCodeType.equals("CPT") || vaccineCodeType.equals("C4") || vaccineCodeType.equals("C5")) {
					vaccineCptCode = vaccineCode;
				} else {
					vaccineCvxCode = vaccineCode;
				}
			}

			{
				String altVaccineCode = reader.getValue(5, 4);
				String altVaccineCodeType = reader.getValue(5, 6);
				if (StringUtils.isNotBlank(altVaccineCode)) {
					if (altVaccineCodeType.equals("NDC")) {
						if (StringUtils.isBlank(vaccineNdcCode)) {
							vaccineNdcCode = altVaccineCode;
						}
					} else if (altVaccineCodeType.equals("CPT") || altVaccineCodeType.equals("C4") || altVaccineCodeType.equals("C5")) {
						if (StringUtils.isBlank(vaccineCptCode)) {
							vaccineCptCode = altVaccineCode;
						}
					} else {
						if (StringUtils.isBlank(vaccineCvxCode)) {
							vaccineCvxCode = altVaccineCode;
						}
					}
				}
			}
//
//			{// TODO sort key logic steps integrable in FHIR
//				Code ndcCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_NDC_CODE, vaccineNdcCode);
//				if (ndcCode != null) {
//					if (ndcCode.getCodeStatus() != null && ndcCode.getCodeStatus().getDeprecated() != null && ndcCode.getCodeStatus().getDeprecated().getNewCodeValue() != null && !ndcCode.getCodeStatus().getDeprecated().getNewCodeValue().equals("")) {
//						vaccineNdcCode = ndcCode.getCodeStatus().getDeprecated().getNewCodeValue();
//					}
//					Code cvxCode = codeMap.getRelatedCode(ndcCode, CodesetType.VACCINATION_CVX_CODE);
//					if (cvxCode == null) {
//						ProcessingException pe = new ProcessingException("Unrecognized NDC " + vaccineNdcCode, "RXA", rxaCount, 5, IisReportableSeverity.WARN);
//						iisReportableList.add(IisReportable.fromProcessingException(pe));
//					} else {
//						if (vaccineCvxCode.equals("")) {
//							vaccineCvxCode = cvxCode.getValue();
//						} else if (!vaccineCvxCode.equals(cvxCode.getValue())) {
//							// NDC doesn't map to the CVX code that was submitted!
//							ProcessingException pe = new ProcessingException("NDC " + vaccineNdcCode + " maps to " + cvxCode.getValue() + " but CVX " + vaccineCvxCode + " was also reported, preferring CVX code", "RXA", rxaCount, 5, IisReportableSeverity.WARN);
//							iisReportableList.add(IisReportable.fromProcessingException(pe));
//						}
//					}
//				}
//			}
//			{
//				Code cptCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CPT_CODE, vaccineCptCode);
//				if (cptCode != null) {
//					Code cvxCode = codeMap.getRelatedCode(cptCode, CodesetType.VACCINATION_CVX_CODE);
//					if (cvxCode == null) {
//						ProcessingException pe = new ProcessingException("Unrecognized CPT " + cptCode, "RXA", rxaCount, 5, IisReportableSeverity.WARN);
//						iisReportableList.add(IisReportable.fromProcessingException(pe));
//					} else {
//						if (vaccineCvxCode.equals("")) {
//							vaccineCvxCode = cvxCode.getValue();
//						} else if (!vaccineCvxCode.equals(cvxCode.getValue())) {
//							// CPT doesn't map to the CVX code that was submitted!
//							ProcessingException pe = new ProcessingException("CPT " + vaccineCptCode + " maps to " + cvxCode.getValue() + " but CVX " + vaccineCvxCode + " was also reported, preferring CVX code", "RXA", rxaCount, 5, IisReportableSeverity.WARN);
//							iisReportableList.add(IisReportable.fromProcessingException(pe));
//						}
//					}
//				}
//			}
//			if (vaccineCvxCode.equals("")) {
//				throw new ProcessingException("Unable to find a recognized vaccine administration code (CVX, NDC, or CPT)", "RXA", rxaCount, 5);
//			} else {
//				Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, vaccineCvxCode);
//				if (cvxCode != null) {
//					vaccineCvxCode = cvxCode.getValue();
//				} else {
//					throw new ProcessingException("Unrecognized CVX vaccine '" + vaccineCvxCode + "'", "RXA", rxaCount, 5);
//				}
//			}


			{
				String administeredAtLocation = reader.getValue(11, 4);
//				if (StringUtils.isEmpty(administeredAtLocation)) {
//
//				}
				if (StringUtils.isNotEmpty(administeredAtLocation)) {
					OrgLocation orgLocation = fhirRequester.searchOrgLocation(new SearchParameterMap("identifier", new TokenParam().setValue(administeredAtLocation)));

					if (orgLocation == null) {
						if (processingFlavorSet.contains(ProcessingFlavor.PEAR)) {
							throw new ProcessingException("Unrecognized administered at location, unable to accept immunization report", "RXA", rxaCount, 11);
						}
						orgLocation = new OrgLocation();
						orgLocation.setOrgFacilityCode(administeredAtLocation);
						orgLocation.setTenant(tenant);
						orgLocation.setOrgFacilityName(administeredAtLocation);
						orgLocation.setLocationType("");
						orgLocation.setAddressLine1(reader.getValue(11, 9));
						orgLocation.setAddressLine2(reader.getValue(11, 10));
						orgLocation.setAddressCity(reader.getValue(11, 11));
						orgLocation.setAddressState(reader.getValue(11, 12));
						orgLocation.setAddressZip(reader.getValue(11, 13));
						orgLocation.setAddressCountry(reader.getValue(11, 14));
						orgLocation = fhirRequester.saveOrgLocation(orgLocation);
					}
					vaccinationReported.setOrgLocation(orgLocation);
				}
			}
			{
				String administeringProvider = reader.getValue(10);
				if (StringUtils.isNotEmpty(administeringProvider)) {
					ModelPerson modelPerson = fhirRequester.searchPractitioner(new SearchParameterMap(Practitioner.SP_IDENTIFIER, new TokenParam().setValue(administeringProvider)));
//								Practitioner.IDENTIFIER.exactly().code(administeringProvider));
					if (modelPerson == null) {
						modelPerson = new ModelPerson();
						modelPerson.setPersonExternalLink(administeringProvider);
						modelPerson.setTenant(tenant);
						modelPerson.setNameLast(reader.getValue(10, 2));
						modelPerson.setNameFirst(reader.getValue(10, 3));
						modelPerson.setNameMiddle(reader.getValue(10, 4));
						modelPerson.setAssigningAuthority(reader.getValue(10, 9));
						modelPerson.setNameTypeCode(reader.getValue(10, 10));
						modelPerson.setIdentifierTypeCode(reader.getValue(10, 13));
						modelPerson.setProfessionalSuffix(reader.getValue(10, 21));
//					  Person  p = PersonMapper.getFhirPerson(modelPerson);
						modelPerson = fhirRequester.savePractitioner(modelPerson);
					}
					vaccinationReported.setAdministeringProvider(modelPerson);
				}

			}
			vaccinationReported.setUpdatedDate(new Date());
			vaccinationReported.setAdministeredDate(administrationDate);
			vaccinationReported.setVaccineCvxCode(vaccineCvxCode);
			vaccinationReported.setVaccineNdcCode(vaccineNdcCode);
			vaccinationReported.setAdministeredAmount(reader.getValue(6));
			vaccinationReported.setInformationSource(reader.getValue(9));
			vaccinationReported.setLotnumber(reader.getValue(15));
			vaccinationReported.setExpirationDate(parseDateWarn(reader.getValue(16), "Invalid vaccination expiration date", "RXA", rxaCount, 16, strictDate, iisReportableList));
			vaccinationReported.setVaccineMvxCode(reader.getValue(17));
			vaccinationReported.setRefusalReasonCode(reader.getValue(18));
			vaccinationReported.setCompletionStatus(reader.getValue(20));

			vaccinationReported.setActionCode(reader.getValue(21));
			int segmentPosition = reader.getSegmentPosition();
			if (reader.advanceToSegment("RXR", "ORC")) {
				vaccinationReported.setBodyRoute(reader.getValue(1));
				vaccinationReported.setBodySite(reader.getValue(2));
			} else if (processingFlavorSet.contains(ProcessingFlavor.SPRUCE)) {
				if (vaccinationReported.getInformationSource().equals("00")) {
					throw new ProcessingException("RXR segment is required for administered vaccinations", "RXA", rxaCount, 0);
				}
			}
			if (vaccinationReported.getAdministeredDate().before(patientReported.getBirthDate()) && !processingFlavorSet.contains(ProcessingFlavor.CLEMENTINE)) {
				throw new ProcessingException("Vaccination is reported as having been administered before the patient was born", "RXA", rxaCount, 3);
			}
			if (!vaccinationReported.getVaccineCvxCode().equals("998") && !vaccinationReported.getVaccineCvxCode().equals("999") && (vaccinationReported.getCompletionStatus().equals("CP") || vaccinationReported.getCompletionStatus().equals("PA") || vaccinationReported.getCompletionStatus().isEmpty())) {
				vaccinationCount++;
			}

			if (vaccinationReported.getCompletionStatus().equals("RE")) {
				refusalCount++;
			}

			reader.gotoSegmentPosition(segmentPosition);
			int tempObxCount = obxCount;
			int fundingSourceObxCount = -1;
			int fundingEligibilityObxCount = -1;
			while (reader.advanceToSegment("OBX", "ORC")) { //TODO store entering and ordering practitioners
				tempObxCount++;
				String indicator = reader.getValue(3);
				if (indicator.equals("64994-7")) {
					String fundingEligibility = reader.getValue(5);
					if (StringUtils.isNotBlank(fundingEligibility)) {
						vaccinationReported.setFundingEligibility(fundingEligibility);
						fundingEligibilityObxCount = obxCount;
					}
				} else if (indicator.equals("30963-3")) {
					String fundingSource = reader.getValue(5);
					if (StringUtils.isNotBlank(fundingSource)) {
						vaccinationReported.setFundingSource(fundingSource);
						fundingSourceObxCount = tempObxCount;
					}
				}
			}

			verifyNoErrors(iisReportableList);
			immunizationProcessingInterceptor.processAndValidateVaccinationReported(vaccinationReported, iisReportableList, processingFlavorSet, fundingSourceObxCount, fundingEligibilityObxCount, rxaCount, vaccineCptCode);
			vaccinationReported = fhirRequester.saveVaccinationReported(vaccinationReported);
			vaccinationReportedList.add(vaccinationReported);
			reader.gotoSegmentPosition(segmentPosition);
			obxCount = readAndCreateObservations(reader, iisReportableList, patientReported, strictDate, obxCount, vaccinationReported, null);

		}
		if (processingFlavorSet.contains(ProcessingFlavor.CRANBERRY) && vaccinationCount == 0) {
			throw new ProcessingException("Patient vaccination history cannot be accepted without at least one administered or historical vaccination specified", "", 0, 0);
		}
		if (processingFlavorSet.contains(ProcessingFlavor.BILBERRY) && (vaccinationCount == 0 && refusalCount == 0)) {
			throw new ProcessingException("Patient vaccination history cannot be accepted without at least one administered, historical, or refused vaccination specified", "", 0, 0);
		}
		return vaccinationReportedList;
	}


	@SuppressWarnings("unchecked")
	public ObservationReported readObservations(HL7Reader reader, List<IisReportable> iisReportableList, PatientReported patientReported, boolean strictDate, int obxCount, VaccinationReported vaccinationReported, VaccinationMaster vaccination, String identifierCode, String valueCode) {
//    ObservationMaster observationMaster = null;
		ObservationReported observationReported = null;
		if (vaccination == null) {
			observationReported = fhirRequester.searchObservationReported(new SearchParameterMap("part-of", new ReferenceParam().setMissing(true))
				.add("subject", new ReferenceParam(patientReported.getPatientId())));
//				Observation.PART_OF.isMissing(true),
//				Observation.SUBJECT.hasId(patientReported.getPatientId()));
		} else {
			observationReported = fhirRequester.searchObservationReported(new SearchParameterMap("part-of", new ReferenceParam(vaccination.getVaccinationId())).add("subject", new ReferenceParam(patientReported.getPatientId())));
//				Observation.PART_OF.hasId(vaccination.getVaccinationId()),
//				Observation.SUBJECT.hasId(patientReported.getPatientId()));
		}
		if (observationReported == null) {
//      observationMaster = new ObservationMaster();
//      observationMaster.setPatientId(patientReported.getPatient().getPatientId());
//      observationMaster.setVaccination(vaccination);
//      observationMaster.setIdentifierCode(identifierCode);
			observationReported = new ObservationReported();
//      observationMaster.setObservationReported(observationReported);
			observationReported.setReportedDate(new Date());
		}
//    observationMaster.setValueCode(valueCode);

		observationReported.setPatientReportedId(patientReported.getPatientId());
		if (vaccinationReported != null) {
			observationReported.setVaccinationReportedId(vaccinationReported.getVaccinationId());
		}
//    observationReported.setObservation(observationMaster);
		observationReported.setUpdatedDate(new Date());
		observationReported.setIdentifierCode(identifierCode);
		observationReported.setValueType(reader.getValue(2));
		observationReported.setIdentifierLabel(reader.getValue(3, 2));
		observationReported.setIdentifierTable(reader.getValue(3, 3));
		observationReported.setValueCode(valueCode);
		observationReported.setValueLabel(reader.getValue(5, 2));
		observationReported.setValueTable(reader.getValue(5, 3));
		observationReported.setUnitsCode(reader.getValue(6, 1));
		observationReported.setUnitsLabel(reader.getValue(6, 2));
		observationReported.setUnitsTable(reader.getValue(6, 3));
		observationReported.setResultStatus(reader.getValue(11));
		observationReported.setObservationDate(parseDateWarn(reader.getValue(14), "Unparsable date/time of observation", "OBX", obxCount, 14, strictDate, iisReportableList));
		observationReported.setMethodCode(reader.getValue(17, 1));
		observationReported.setMethodLabel(reader.getValue(17, 2));
		observationReported.setMethodTable(reader.getValue(17, 3));
		return observationReported;
	}




}
