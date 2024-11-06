package org.immregistries.iis.kernal.logic;

import ca.uhn.fhir.rest.client.api.IGenericClient;
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
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodeStatusValue;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.InternalClient.FhirRequester;
import org.immregistries.iis.kernal.InternalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.SoftwareVersion;
import org.immregistries.iis.kernal.fhir.interceptors.PartitionCreationInterceptor;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.mapping.Interfaces.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.Interfaces.LocationMapper;
import org.immregistries.iis.kernal.mapping.Interfaces.ObservationMapper;
import org.immregistries.iis.kernal.mapping.Interfaces.PatientMapper;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.iis.kernal.servlet.PopServlet;
import org.immregistries.mqe.hl7util.Reportable;
import org.immregistries.mqe.hl7util.SeverityLevel;
import org.immregistries.mqe.hl7util.builder.AckData;
import org.immregistries.mqe.hl7util.builder.HL7Util;
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
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public abstract class IncomingMessageHandler implements IIncomingMessageHandler {
	public static final int NAME_SIZE_LIMIT = 15;
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
	PatientMapper patientMapper;
	@Autowired
	ImmunizationMapper immunizationMapper;
	@Autowired
	ObservationMapper observationMapper;
	@Autowired
	LocationMapper locationMapper;
	@Autowired
	PartitionCreationInterceptor partitionCreationInterceptor;

	SyncHL7Validator syncHL7Validator;

	public IncomingMessageHandler() {
		mqeMessageService = MqeMessageService.INSTANCE;
		dataSession = PopServlet.getDataSession();

		InputStream profileXML = IncomingMessageHandler.class.getResourceAsStream("/export/VXU-Z22_Profile.xml");
		InputStream constraintsXML = IncomingMessageHandler.class.getResourceAsStream("/export/VXU-Z22_Constraints.xml");
		InputStream vsLibraryXML = IncomingMessageHandler.class.getResourceAsStream("/export/VXU-Z22_ValueSetLibrary.xml");

		Profile profile = XMLDeserializer.deserialize(profileXML).get();
		ValueSetLibrary valueSetLibrary = ValueSetLibraryImpl.apply(vsLibraryXML).get();
		ConformanceContext conformanceContext = DefaultConformanceContext.apply(Collections.singletonList(constraintsXML)).get();

		syncHL7Validator = new SyncHL7Validator(profile, valueSetLibrary, conformanceContext);
	}

	public void verifyNoErrors(List<ProcessingException> processingExceptionList) throws ProcessingException {
		for (ProcessingException pe : processingExceptionList) {
			if (pe.isError()) {
				throw pe;
			}
		}
	}

	public boolean hasErrors(List<ProcessingException> processingExceptionList) {
		for (ProcessingException pe : processingExceptionList) {
			if (pe.isError()) {
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
			testCase.setPatientSex(patient == null ? "F" : patient.getSex());
			testCase.setPatientDob(patient.getBirthDate());
			List<TestEvent> testEventList = new ArrayList<>();
			for (VaccinationMaster vaccination : vaccinationMasterList) {
				Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, vaccination.getVaccineCvxCode());
				if (cvxCode == null) {
					continue;
				}
				if ("D".equals(vaccination.getActionCode())) {
					continue;
				}
				int cvx = 0;
				try {
					cvx = Integer.parseInt(vaccination.getVaccineCvxCode());
					TestEvent testEvent = new TestEvent(cvx, vaccination.getAdministeredDate());
					testEventList.add(testEvent);
					vaccination.setTestEvent(testEvent);
				} catch (NumberFormatException nfe) {
					continue;
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

	public String buildAck(HL7Reader reader, List<ProcessingException> processingExceptionList, Set<ProcessingFlavor> processingFlavorSet) {
		StringBuilder sb = new StringBuilder();
		{
			String messageType = "ACK^V04^ACK";
			String profileId = Z23_ACKNOWLEDGEMENT;
			hl7MessageWriter.createMSH(messageType, profileId, reader, sb, processingFlavorSet);
		}

		// if processing flavor contains MEDLAR then all the non E errors have to removed from the processing list
		if (processingFlavorSet != null && processingFlavorSet.contains(ProcessingFlavor.MEDLAR)) {
			List<ProcessingException> tempProcessingExceptionList = new ArrayList<ProcessingException>();
			for (ProcessingException pe : processingExceptionList) {
				if (pe.isError()) {
					tempProcessingExceptionList.add(pe);
				}
			}
			processingExceptionList = tempProcessingExceptionList;
		}

		String sendersUniqueId = "";
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
		for (ProcessingException pe : processingExceptionList) {
			if (pe.isError() || pe.isWarning()) {
				overallStatus = "AE";
				break;
			}
		}

		sb.append("MSA|").append(overallStatus).append("|").append(sendersUniqueId).append("\r");
		for (ProcessingException pe : processingExceptionList) {
			sb.append(HL7Util.makeERRSegment(new ProcessingExceptionReportable(pe), false));
		}
		return sb.toString();
	}

	public String buildAckMqe(MqeMessageServiceResponse mqeMessageServiceResponse, List<ProcessingException> processingExceptionList, Set<ProcessingFlavor> processingFlavorSet, List<Reportable> validatorReportables) {
		IisAckBuilder ackBuilder = IisAckBuilder.INSTANCE;
		AckData data = new AckData();
		MqeMessageHeader header = mqeMessageServiceResponse.getMessageObjects().getMessageHeader();
		data.setProfileId(Z23_ACKNOWLEDGEMENT);

		List<ValidationRuleResult> resultList = mqeMessageServiceResponse.getValidationResults();
		List<Reportable> reportables = new ArrayList<>(validatorReportables);
		reportables.addAll(processingExceptionList.stream().map(ProcessingExceptionReportable::new).collect(Collectors.toList()));
		/* This code needs to get put somewhere better. */
		for (ValidationRuleResult result : resultList) {
			reportables.addAll(result.getValidationDetections());
		}
		// if processing flavor contains MEDLAR then all the non E errors have to removed from the processing list
		if (processingFlavorSet != null && processingFlavorSet.contains(ProcessingFlavor.MEDLAR)) {
			reportables = reportables.stream().filter(reportable -> !SeverityLevel.ERROR.equals(reportable.getSeverity())).collect(Collectors.toList());
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

		return ackBuilder.buildAckFrom(data);
	}

	public Date parseDateWarn(String dateString, String errorMessage, String segmentId, int segmentRepeat, int fieldPosition, boolean strict, List<ProcessingException> processingExceptionList) {
		try {
			return parseDateInternal(dateString, strict);
		} catch (ParseException e) {
			if (errorMessage != null) {
				ProcessingException pe = new ProcessingException(errorMessage + ": " + e.getMessage(), segmentId, segmentRepeat, fieldPosition).setWarning();
				processingExceptionList.add(pe);
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
		if (dateString.length() == 0) {
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

	public String processQBP(Tenant tenant, HL7Reader reader, String messageReceived) {
		PatientMaster patientMasterForMatchQuery = new PatientMaster();
		List<ProcessingException> processingExceptionList = new ArrayList<>();
		if (reader.advanceToSegment("QPD")) {
			String mrn = "";
			{
				mrn = reader.getValueBySearchingRepeats(3, 1, "MR", 5);
				if (mrn.equals("")) {
					mrn = reader.getValueBySearchingRepeats(3, 1, "PT", 5);
				}
			}
			String problem = null;
			int fieldPosition = 0;
			if (!mrn.equals("")) {
				patientMasterForMatchQuery.setExternalLink(mrn); // TODO system
//				patientReported = fhirRequester.searchPatientReported(
//					Patient.IDENTIFIER.exactly().systemAndCode(MRN_SYSTEM, mrn)
//				);
			}
			String patientNameLast = reader.getValue(4, 1);
			String patientNameFirst = reader.getValue(4, 2);
			String patientNameMiddle = reader.getValue(4, 3);
			boolean strictDate = false;

			Date patientBirthDate = parseDateWarn(reader.getValue(6), "Invalid patient birth date", "QPD", 1, 6, strictDate, processingExceptionList);
			String patientSex = reader.getValue(7);

			if (patientNameLast.equals("")) {
				problem = "Last name is missing";
				fieldPosition = 4;
			} else if (patientNameFirst.equals("")) {
				problem = "First name is missing";
				fieldPosition = 4;
			} else if (patientBirthDate == null) {
				problem = "Date of Birth is missing";
				fieldPosition = 6;
			}
			if (StringUtils.isNotBlank(problem)) {
				processingExceptionList.add(new ProcessingException(problem, "QPD", 1, fieldPosition));
			} else {

				PatientName patientName = new PatientName(patientNameLast, patientNameFirst, patientNameMiddle, "");
				patientMasterForMatchQuery.addPatientName(patientName);
				patientMasterForMatchQuery.setBirthDate(patientBirthDate);
			}
		} else {
			processingExceptionList.add(new ProcessingException("QPD segment not found", null, 0, 0));
		}

		Set<ProcessingFlavor> processingFlavorSet = tenant.getProcessingFlavorSet();
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
		PatientMaster singleMatch = null;

		singleMatch = fhirRequester.matchPatient(multipleMatches, patientMasterForMatchQuery, cutoff);

		return buildRSP(reader, messageReceived, singleMatch, tenant, multipleMatches, processingExceptionList);
	}

	@SuppressWarnings("unchecked")
	public String buildRSP(HL7Reader reader, String messageReceived, PatientMaster patientMaster, Tenant tenant, List<PatientReported> patientReportedPossibleList, List<ProcessingException> processingExceptionList) {
		IGenericClient fhirClient = repositoryClientFactory.getFhirClient();
		reader.resetPostion();
		reader.advanceToSegment("MSH");

		Set<ProcessingFlavor> processingFlavorSet = tenant.getProcessingFlavorSet();
		StringBuilder sb = new StringBuilder();
		String profileIdSubmitted = reader.getValue(21);
		CodeMap codeMap = CodeMapManager.getCodeMap();
		String categoryResponse = "No Match";
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
			String messageType = "RSP^K11^RSP_K11";
			if (patientMaster == null) {
				queryResponse = QUERY_NOT_FOUND;
				profileId = RSP_Z33_NO_MATCH;
				categoryResponse = "No Match";
				if (patientReportedPossibleList.size() > 0) {
					if (profileIdSubmitted.equals(QBP_Z34)) {
						if (patientReportedPossibleList.size() > maxCount) {
							queryResponse = QUERY_TOO_MANY;
							profileId = RSP_Z33_NO_MATCH;
							categoryResponse = "Too Many Matches";
						} else {
							queryResponse = QUERY_OK;
							profileId = RSP_Z31_MULTIPLE_MATCH;
							categoryResponse = "Possible Match";
						}
					} else if (profileIdSubmitted.equals("Z44")) {
						queryResponse = QUERY_NOT_FOUND;
						profileId = RSP_Z33_NO_MATCH;
						categoryResponse = "No Match";
					}
				}
				if (hasErrors(processingExceptionList)) {
					queryResponse = QUERY_APPLICATION_ERROR;
				}
			} else if (profileIdSubmitted.equals(QBP_Z34)) {
				profileId = RSP_Z32_MATCH;
				categoryResponse = "Match";
			} else if (profileIdSubmitted.equals(QBP_Z44)) {
				if (processingFlavorSet.contains(ProcessingFlavor.ORANGE)) {
					profileId = RSP_Z32_MATCH;
					categoryResponse = "Match";
				} else {
					sendBackForecast = true;
					profileId = RSP_Z42_MATCH_WITH_FORECAST;
					categoryResponse = "Match";
				}
			} else {
				processingExceptionList.add(new ProcessingException("Unrecognized profile id '" + profileIdSubmitted + "'", "MSH", 1, 21));
			}
			hl7MessageWriter.createMSH(messageType, profileId, reader, sb, processingFlavorSet);
		}
		{
			String sendersUniqueId = reader.getValue(10);
			if (hasErrors(processingExceptionList)) {
				sb.append("MSA|AE|").append(sendersUniqueId).append("\r");
			} else {
				sb.append("MSA|AA|").append(sendersUniqueId).append("\r");
			}
			if (processingExceptionList.size() > 0) {
				sb.append(HL7Util.makeERRSegment(new ProcessingExceptionReportable(processingExceptionList.get(processingExceptionList.size() - 1)), false));
			}
		}
		String profileName = "Request a Complete Immunization History";
		if (profileIdSubmitted.equals("")) {
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
				if (!vaccination.getVaccineNdcCode().equals("")) {
					Code ndcCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_NDC_CODE, vaccination.getVaccineNdcCode());
					if (ndcCode != null) {
						sb.append("~").append(ndcCode.getValue()).append("^").append(ndcCode.getLabel()).append("^NDC");
					}
				}
				{
					// RXA-6
					sb.append("|");
					double adminAmount = 0.0;
					if (!vaccination.getAdministeredAmount().equals("")) {
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
					if (completionStatus == null || completionStatus.equals("")) {
						completionStatus = "CP";
					}
					sb.append(hl7MessageWriter.printCode(completionStatus, CodesetType.VACCINATION_COMPLETION, null, codeMap));
				}

				// RXA-21
				sb.append("|A");
				sb.append("\r");
				if (vaccination.getBodyRoute() != null && !vaccination.getBodyRoute().equals("")) {
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
							ObservationReported observationReported = observationMapper.getReported(entry.getResource());
							obxSetId++;
							hl7MessageWriter.printObx(sb, obxSetId, obsSubId, observationReported);
						}
					}
				} catch (ResourceNotFoundException e) {
				}
			}
			try {
				Bundle bundle = fhirClient.search().forResource(Observation.class).where(Observation.PART_OF.hasId(patientMaster.getPatientId())).returnBundle(Bundle.class).execute();
				if (bundle.hasEntry()) {
					hl7MessageWriter.printORC(tenant, sb, null, false);
					obsSubId++;
					for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
						obxSetId++;
						ObservationReported observationReported = observationMapper.getReported(entry.getResource());
						hl7MessageWriter.printObx(sb, obxSetId, obsSubId, observationReported);
					}
				}
			} catch (ResourceNotFoundException e) {
			}

			if (sendBackForecast && forecastActualList != null && forecastActualList.size() > 0) {
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

		String messageResponse = sb.toString();
		recordMessageReceived(messageReceived, patientMaster, messageResponse, "Query", categoryResponse, tenant);
		return messageResponse;
	}

	public int readAndCreateObservations(HL7Reader reader, List<ProcessingException> processingExceptionList, PatientReported patientReported, boolean strictDate, int obxCount, VaccinationReported vaccinationReported, VaccinationMaster vaccination) {
		while (reader.advanceToSegment("OBX", "ORC")) {
			obxCount++;
			String identifierCode = reader.getValue(3);
			String valueCode = reader.getValue(5);
			ObservationReported observationReported = readObservations(reader, processingExceptionList, patientReported, strictDate, obxCount, vaccinationReported, vaccination, identifierCode, valueCode);
			if (observationReported.getIdentifierCode().equals("30945-0")) // contraindication!
			{
				CodeMap codeMap = CodeMapManager.getCodeMap();
				Code contraCode = codeMap.getCodeForCodeset(CodesetType.CONTRAINDICATION_OR_PRECAUTION, observationReported.getValueCode());
				if (contraCode == null) {
					ProcessingException pe = new ProcessingException("Unrecognized contraindication or precaution", "OBX", obxCount, 5);
					pe.setWarning();
					processingExceptionList.add(pe);
				}
				if (observationReported.getObservationDate() != null) {
					Date today = new Date();
					if (observationReported.getObservationDate().after(today)) {
						ProcessingException pe = new ProcessingException("Contraindication or precaution observed in the future", "OBX", obxCount, 5);
						pe.setWarning();
						processingExceptionList.add(pe);
					}
					if (patientReported.getBirthDate() != null && observationReported.getObservationDate().before(patientReported.getBirthDate())) {
						ProcessingException pe = new ProcessingException("Contraindication or precaution observed before patient was born", "OBX", obxCount, 14);
						pe.setWarning();
						processingExceptionList.add(pe);
					}
				}
			}
			{
				observationReported.setPatientReportedId(patientReported.getPatientId());

//		  Observation observation = ObservationMapper.getFhirResource(observationMaster,observationReported);
				observationReported = fhirRequester.saveObservationReported(observationReported);

			}
		}
		return obxCount;
	}

	public abstract ObservationReported readObservations(HL7Reader reader, List<ProcessingException> processingExceptionList, PatientReported patientReported, boolean strictDate, int obxCount, VaccinationReported vaccinationReported, VaccinationMaster vaccination, String identifierCode, String valueCode);


	public List<Reportable> nistValidation(String message) throws Exception {
		String id = "aa72383a-7b48-46e5-a74a-82e019591fe7";
		List<Reportable> reportableList = new ArrayList();
		Report report = syncHL7Validator.check(message, id);
		logger.info(report.toText());
		logger.info(report.toJson());
		for (Map.Entry<String, List<Entry>> mapEntry : report.getEntries().entrySet()
		) {
			for (Entry assertion : mapEntry.getValue()) {
				logger.info("entry {}", assertion.toText());
				String severity = assertion.getClassification();
				SeverityLevel severityLevel = SeverityLevel.ACCEPT;
				if (severity.equalsIgnoreCase("error")) {
					severityLevel = SeverityLevel.WARN;
				}

				if (severityLevel != SeverityLevel.ACCEPT) {
					NISTReportable reportable = new NISTReportable();
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
					this.readErrorLocation(reportable, path);
				}
			}
		}


//		ValidationReport validationReport = new ValidationReport(report.toText());

		return reportableList;
	}

	public void readErrorLocation(NISTReportable reportable, String path) {
		if (path != null && path.length() >= 3) {
			String segmentid = path.substring(0, 3);
			if (path.length() > 3) {
				path = path.substring(4);
			} else {
				path = "";
			}

			Hl7Location errorLocation = NISTReportable.readErrorLocation(path, segmentid);
			if (errorLocation != null) {
				reportable.getHl7LocationList().add(errorLocation);
			}
		}

	}


	public PatientReported processPatientFhirAgnostic(HL7Reader reader, List<ProcessingException> processingExceptionList, Set<ProcessingFlavor> processingFlavorSet, CodeMap codeMap, boolean strictDate, PatientReported patientReported, String patientReportedExternalLink, String patientReportedAuthority, String patientReportedType) throws ProcessingException {

		if (processingFlavorSet.contains(ProcessingFlavor.APPLESAUCE)) {

		}
		List<PatientName> names = new ArrayList<>(reader.getRepeatCount(5));
		PatientName legalName = null;
		for (int i = 0; i <= reader.getRepeatCount(5); i++) {
			String patientNameLast = reader.getValueRepeat(5, 1, i);
			String patientNameFirst = reader.getValueRepeat(5, 2, i);
			String patientNameMiddle = reader.getValueRepeat(5, 3, i);


			String nameType = reader.getValueRepeat(5, 7, i);
			if (processingFlavorSet.contains(ProcessingFlavor.APPLESAUCE)) {
				if (patientNameFirst.toUpperCase().contains("BABY BOY") || patientNameFirst.toUpperCase().contains("BABY GIRL") ||
					patientNameFirst.toUpperCase().contains("BABY")) {
					nameType = "NB";
				} else if (patientNameFirst.toUpperCase().contains("TEST")) {
					nameType = "TEST";
				}
			}

			if (processingFlavorSet.contains(ProcessingFlavor.MANDATORYLEGALNAME)) {
				patientNameLast = patientNameLast.toUpperCase();
				patientNameFirst = patientNameFirst.toUpperCase();
				patientNameMiddle = patientNameMiddle.toUpperCase();
			}

			if (processingFlavorSet.contains(ProcessingFlavor.ASCIICONVERT)) {
				patientNameLast = Normalizer.normalize(patientNameLast, Normalizer.Form.NFD);
				patientNameFirst = Normalizer.normalize(patientNameFirst, Normalizer.Form.NFD);
				patientNameMiddle = Normalizer.normalize(patientNameMiddle, Normalizer.Form.NFD);
			}

			if (processingFlavorSet.contains(ProcessingFlavor.NONASCIIREJECT)) {
				if (!Normalizer.isNormalized(patientNameLast, Normalizer.Form.NFD) ||
					!Normalizer.isNormalized(patientNameFirst, Normalizer.Form.NFD) ||
					!Normalizer.isNormalized(patientNameMiddle, Normalizer.Form.NFD)) {
					throw new ProcessingException("Illegal characters found in name", "PID", 1, 5);
				}
			}

			if (processingFlavorSet.contains(ProcessingFlavor.REMOVEHYPHENSPACES)) {
				patientNameLast = patientNameLast.replace(" ", "").replace("-", "");
				patientNameFirst = patientNameFirst.replace(" ", "").replace("-", "");
				patientNameMiddle = patientNameMiddle.replace(" ", "").replace("-", "");
			}

			if (processingFlavorSet.contains(ProcessingFlavor.LIMITSIZENAME)) {
				patientNameLast = patientNameLast.substring(0, NAME_SIZE_LIMIT);
				patientNameFirst = patientNameFirst.substring(0, NAME_SIZE_LIMIT);
				patientNameMiddle = patientNameMiddle.substring(0, NAME_SIZE_LIMIT);
			}

			if (processingFlavorSet.contains(ProcessingFlavor.NOSINGLECHARNAME)) {
				if (patientNameLast.replace(".", "").length() == 1 ||
					patientNameFirst.replace(".", "").length() == 1 ||
					patientNameMiddle.replace(".", "").length() == 1) {
					throw new ProcessingException("Single character names not accepted", "PID", 1, 5);
				}
				patientNameLast = patientNameLast.substring(0, NAME_SIZE_LIMIT);
				patientNameFirst = patientNameFirst.substring(0, NAME_SIZE_LIMIT);
				patientNameMiddle = patientNameMiddle.substring(0, NAME_SIZE_LIMIT);
			}

			if (processingFlavorSet.contains(ProcessingFlavor.MIDDLENAMECONCAT)) {
				patientNameFirst += " " + patientNameMiddle;
				patientNameMiddle = "";
			}
			PatientName patientName = new PatientName(patientNameLast, patientNameFirst, patientNameMiddle, nameType);


			names.add(patientName);
			if ("L".equals(nameType)) {
				legalName = patientName;
			}
			if (processingFlavorSet.contains(ProcessingFlavor.IGNORENAMETYPE)) {
				nameType = "";
				patientName.setNameType("");
				i = reader.getRepeatCount(5) + 1;
			}
		}

		if (legalName == null && processingFlavorSet.contains(ProcessingFlavor.MANDATORYLEGALNAME)) {
			throw new ProcessingException("Patient legal name not found", "PID", 1, 5);
		}
		if (legalName != null && processingFlavorSet.contains(ProcessingFlavor.REJECTLONGNAME) && legalName.getNameLast().length() > NAME_SIZE_LIMIT) {
			if (legalName.getNameLast().length() > NAME_SIZE_LIMIT ||
				legalName.getNameFirst().length() > NAME_SIZE_LIMIT ||
				legalName.getNameMiddle().length() > NAME_SIZE_LIMIT) {
				throw new ProcessingException("Patient name is too long", "PID", 1, 5);

			}
		}
		if (legalName != null && legalName.getNameLast().equals("")) {
			throw new ProcessingException("Patient last name was not found, required for accepting patient and vaccination history", "PID", 1, 5);
		}
		if (legalName != null && legalName.getNameFirst().equals("")) {
			throw new ProcessingException("Patient first name was not found, required for accepting patient and vaccination history", "PID", 1, 5);
		}

		String patientPhone = reader.getValue(13, 6) + reader.getValue(13, 7);
		String telUseCode = reader.getValue(13, 2);
		if (patientPhone.length() > 0) {
			if (!telUseCode.equals("PRN")) {
				ProcessingException pe = new ProcessingException("Patient phone telecommunication type must be PRN ", "PID", 1, 13);
				if (!processingFlavorSet.contains(ProcessingFlavor.QUINZE)) {
					pe.setWarning();
				}
				processingExceptionList.add(pe);
			}

			{
				int countNums = 0;
				boolean invalidCharFound = false;
				char invalidChar = ' ';
				for (char c : patientPhone.toCharArray()) {

					if (c >= '0' && c <= '9') {
						countNums++;
					} else if (c != '-' && c != '.' && c != ' ' && c != '(' && c != ')') {
						if (!invalidCharFound) {
							invalidCharFound = true;
							invalidChar = c;
						}
					}
				}
				if (invalidCharFound) {
					ProcessingException pe = new ProcessingException("Patient phone number has unexpected character: " + invalidChar, "PID", 1, 13);
					pe.setWarning();
					processingExceptionList.add(pe);
				}
				if (countNums != 10 || patientPhone.startsWith("555") || patientPhone.startsWith("0") || patientPhone.startsWith("1")) {
					ProcessingException pe = new ProcessingException("Patient phone number does not appear to be valid", "PID", 1, 13);
					pe.setWarning();
					processingExceptionList.add(pe);
				}
			}
		}
		if (!telUseCode.equals("PRN")) {
			patientPhone = "";
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
		Date patientBirthDate;
		patientBirthDate = parseDateError(reader.getValue(7), "Bad format for date of birth", "PID", 1, 7, strictDate);
		if (patientBirthDate.after(new Date())) {
			throw new ProcessingException("Patient is indicated as being born in the future, unable to record patients who are not yet born", "PID", 1, 7);
		}
		patientReported.setExternalLink(patientReportedExternalLink);
		patientReported.setPatientReportedType(patientReportedType);
		patientReported.setPatientNames(names);
		patientReported.setMotherMaidenName(reader.getValue(6));
		patientReported.setBirthDate(patientBirthDate);
		patientReported.setSex(reader.getValue(8));
		patientReported.setRace(reader.getValue(10));
		patientReported.setRace2(reader.getValueRepeat(10, 1, 2));
		patientReported.setRace3(reader.getValueRepeat(10, 1, 3));
		patientReported.setRace4(reader.getValueRepeat(10, 1, 4));
		patientReported.setRace5(reader.getValueRepeat(10, 1, 5));
		patientReported.setRace6(reader.getValueRepeat(10, 1, 6));
		patientReported.setAddressLine1(reader.getValue(11, 1));
		patientReported.setAddressLine2(reader.getValue(11, 2));
		patientReported.setAddressCity(reader.getValue(11, 3));
		patientReported.setAddressState(reader.getValue(11, 4));
		patientReported.setAddressZip(reader.getValue(11, 5));
		patientReported.setAddressCountry(reader.getValue(11, 6));
		patientReported.setAddressCountyParish(reader.getValue(11, 9));
		patientReported.setEthnicity(reader.getValue(22));
		patientReported.setBirthFlag(reader.getValue(24));
		patientReported.setBirthOrder(reader.getValue(25));
		patientReported.setDeathDate(parseDateWarn(reader.getValue(29), "Invalid patient death date", "PID", 1, 29, strictDate, processingExceptionList));
		patientReported.setDeathFlag(reader.getValue(30));
		patientReported.setEmail(reader.getValueBySearchingRepeats(13, 4, "NET", 2));
		patientReported.setPhone(patientPhone);
		patientReported.setPatientReportedAuthority(patientReportedAuthority);

		{
			String patientSex = patientReported.getSex();
			if (!ValidValues.verifyValidValue(patientSex, ValidValues.SEX)) {
				ProcessingException pe = new ProcessingException("Patient sex '" + patientSex + "' is not recognized", "PID", 1, 8).setWarning();
				if (processingFlavorSet.contains(ProcessingFlavor.ELDERBERRIES)) {
					pe.setWarning();
				}
				processingExceptionList.add(pe);
			}
		}

		String patientAddressCountry = patientReported.getAddressCountry();
		if (!patientAddressCountry.equals("")) {
			if (!ValidValues.verifyValidValue(patientAddressCountry, ValidValues.COUNTRY_2DIGIT) && !ValidValues.verifyValidValue(patientAddressCountry, ValidValues.COUNTRY_3DIGIT)) {
				ProcessingException pe = new ProcessingException("Patient address country '" + patientAddressCountry + "' is not recognized and cannot be accepted", "PID", 1, 11);
				if (processingFlavorSet.contains(ProcessingFlavor.GUAVA)) {
					pe.setWarning();
				}
				processingExceptionList.add(pe);
			}
		}
		if (patientAddressCountry.equals("") || patientAddressCountry.equals("US") || patientAddressCountry.equals("USA")) {
			String patientAddressState = patientReported.getAddressState();
			if (!patientAddressState.equals("")) {
				if (!ValidValues.verifyValidValue(patientAddressState, ValidValues.STATE)) {
					ProcessingException pe = new ProcessingException("Patient address state '" + patientAddressState + "' is not recognized and cannot be accepted", "PID", 1, 11);
					if (processingFlavorSet.contains(ProcessingFlavor.GUAVA)) {
						pe.setWarning();
					}
					processingExceptionList.add(pe);
				}
			}
		}


		{
			String race = patientReported.getRace();
			if (!race.equals("")) {
				Code raceCode = codeMap.getCodeForCodeset(CodesetType.PATIENT_RACE, race);
				if (raceCode == null || CodeStatusValue.getBy(raceCode.getCodeStatus()) != CodeStatusValue.VALID) {
					ProcessingException pe = new ProcessingException("Invalid race '" + race + "', message cannot be accepted", "PID", 1, 10);
					if (!processingFlavorSet.contains(ProcessingFlavor.FIG)) {
						pe.setWarning();
					}
					processingExceptionList.add(pe);
				}
			}
		}

		{
			String ethnicity = patientReported.getEthnicity();
			if (!ethnicity.equals("")) {
				Code ethnicityCode = codeMap.getCodeForCodeset(CodesetType.PATIENT_ETHNICITY, ethnicity);
				if (ethnicityCode == null || CodeStatusValue.getBy(ethnicityCode.getCodeStatus()) != CodeStatusValue.VALID) {
					ProcessingException pe = new ProcessingException("Invalid ethnicity '" + ethnicity + "', message cannot be accepted", "PID", 1, 10);
					if (!processingFlavorSet.contains(ProcessingFlavor.FIG)) {
						pe.setWarning();
					}
					processingExceptionList.add(pe);
				}
			}
		}

		if (processingFlavorSet.contains(ProcessingFlavor.BLACKBERRY)) {
			if (patientReported.getAddressLine1().equals("") || patientReported.getAddressCity().equals("") || patientReported.getAddressState().equals("") || patientReported.getAddressZip().equals("")) {
				throw new ProcessingException("Patient address is required but it was not sent", "PID", 1, 11);
			}
		}

		{
			String birthFlag = patientReported.getBirthFlag();
			String birthOrder = patientReported.getBirthOrder();
			if (!birthFlag.equals("") || !birthOrder.equals("")) {
				if (birthFlag.equals("") || birthFlag.equals("N")) {
					// The only acceptable value here is now blank or 1
					if (!birthOrder.equals("1") && !birthOrder.equals("")) {
						ProcessingException pe = new ProcessingException("Birth order was specified as " + birthOrder + " but not indicated as multiple birth", "PID", 1, 25);
						if (processingFlavorSet.contains(ProcessingFlavor.PLANTAIN)) {
							pe.setWarning();
						}
						processingExceptionList.add(pe);
					}
				} else if (birthFlag.equals("Y")) {
					if (birthOrder.equals("")) {
						ProcessingException pe = new ProcessingException("Multiple birth but birth order was not specified", "PID", 1, 24);
						pe.setWarning();
						processingExceptionList.add(pe);
					} else if (!ValidValues.verifyValidValue(birthOrder, ValidValues.BIRTH_ORDER)) {
						ProcessingException pe = new ProcessingException("Birth order was specified as " + birthOrder + " but not an expected value, must be between 1 and 9", "PID", 1, 25);
						if (processingFlavorSet.contains(ProcessingFlavor.PLANTAIN)) {
							pe.setWarning();
						}
						processingExceptionList.add(pe);
					}
				} else {
					ProcessingException pe = new ProcessingException("Multiple birth indicator " + birthFlag + " is not recognized", "PID", 1, 24);
					if (processingFlavorSet.contains(ProcessingFlavor.PLANTAIN)) {
						pe.setWarning();
					}
					processingExceptionList.add(pe);
				}
			}
		}

		if (reader.advanceToSegment("PD1")) {
			patientReported.setPublicityIndicator(reader.getValue(11));
			patientReported.setProtectionIndicator(reader.getValue(12));
			patientReported.setProtectionIndicatorDate(parseDateWarn(reader.getValue(13), "Invalid protection indicator date", "PD1", 1, 13, strictDate, processingExceptionList));
			patientReported.setRegistryStatusIndicator(reader.getValue(16));
			patientReported.setRegistryStatusIndicatorDate(parseDateWarn(reader.getValue(17), "Invalid registry status indicator date", "PD1", 1, 17, strictDate, processingExceptionList));
			patientReported.setPublicityIndicatorDate(parseDateWarn(reader.getValue(18), "Invalid publicity indicator date", "PD1", 1, 18, strictDate, processingExceptionList));
		}
		reader.resetPostion();
		{
			int repeatCount = 0;
			while (reader.advanceToSegment("NK1")) {
				patientReported.setGuardianLast(reader.getValue(2, 1));
				patientReported.setGuardianFirst(reader.getValue(2, 2));
				patientReported.setGuardianMiddle(reader.getValue(2, 1));
				String guardianRelationship = reader.getValue(3);
				patientReported.setGuardianRelationship(guardianRelationship);
				repeatCount++;
				if (patientReported.getGuardianLast().equals("")) {
					ProcessingException pe = new ProcessingException("Next-of-kin last name is empty", "NK1", repeatCount, 2).setWarning();
					processingExceptionList.add(pe);
				}
				if (patientReported.getGuardianFirst().equals("")) {
					ProcessingException pe = new ProcessingException("Next-of-kin first name is empty", "NK1", repeatCount, 2).setWarning();
					processingExceptionList.add(pe);
				}
				if (guardianRelationship.equals("")) {
					ProcessingException pe = new ProcessingException("Next-of-kin relationship is empty", "NK1", repeatCount, 3).setWarning();
					processingExceptionList.add(pe);
				}
				if (guardianRelationship.equals("MTH") || guardianRelationship.equals("FTH") || guardianRelationship.equals("GRD")) {
					break;
				} else {
					ProcessingException pe = new ProcessingException((guardianRelationship.equals("") ? "Next-of-kin relationship not specified so is not recognized as guardian and will be ignored" : ("Next-of-kin relationship '" + guardianRelationship + "' is not a recognized guardian and will be ignored")), "NK1", repeatCount, 3).setWarning();
					processingExceptionList.add(pe);
				}
			}
		}
		reader.resetPostion();

		verifyNoErrors(processingExceptionList);

		patientReported.setUpdatedDate(new Date());
		patientReported = fhirRequester.savePatientReported(patientReported);
		patientReported = fhirRequester.saveRelatedPerson(patientReported);

		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		ArrayList<String> groupPatientIds = (ArrayList<String>) request.getAttribute("groupPatientIds");
		if (groupPatientIds != null) { // If there are numerous patients added and option was activated
			groupPatientIds.add(patientReported.getPatientId());
		}
		request.setAttribute("groupPatientIds", groupPatientIds);


		return patientReported;
	}


}
