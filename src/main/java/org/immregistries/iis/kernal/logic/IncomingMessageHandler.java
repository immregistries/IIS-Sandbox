package org.immregistries.iis.kernal.logic;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
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
import org.immregistries.iis.kernal.mapping.Interfaces.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.Interfaces.LocationMapper;
import org.immregistries.iis.kernal.mapping.Interfaces.ObservationMapper;
import org.immregistries.iis.kernal.mapping.Interfaces.PatientMapper;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.iis.kernal.servlet.PopServlet;
import org.immregistries.smm.tester.manager.HL7Reader;
import org.immregistries.vfa.connect.ConnectFactory;
import org.immregistries.vfa.connect.ConnectorInterface;
import org.immregistries.vfa.connect.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public abstract class IncomingMessageHandler implements IIncomingMessageHandler {
	public static final double MINIMAL_MATCHING_SCORE = 0.9;


	protected static final String PATIENT_MIDDLE_NAME_MULTI = "Multi";
	protected static final String QBP_Z34 = "Z34";
	protected static final String QBP_Z44 = "Z44";
	protected static final String RSP_Z42_MATCH_WITH_FORECAST = "Z42";
	protected static final String RSP_Z32_MATCH = "Z32";
	protected static final String RSP_Z31_MULTIPLE_MATCH = "Z31";
	protected static final String RSP_Z33_NO_MATCH = "Z33";
	protected static final String Z23_ACKNOWLEDGEMENT = "Z23";
	protected static final String QUERY_OK = "OK";
	// TODO:
	// Organize logic classes, need to have access classes for every object, maybe a new Access
	// package?
	// Look at names of database fields, make more consistent
	protected static final String QUERY_NOT_FOUND = "NF";
	protected static final String QUERY_TOO_MANY = "TM";
	protected static final String QUERY_APPLICATION_ERROR = "AE";
	protected static final Random random = new Random();
	private static Integer increment = 1;
	protected final Logger logger = LoggerFactory.getLogger(IncomingMessageHandler.class);
	@Autowired
	protected RepositoryClientFactory repositoryClientFactory;
	@Autowired
	protected FhirRequester fhirRequester;
	protected Session dataSession;
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

	public IncomingMessageHandler() {
		dataSession = PopServlet.getDataSession();
	}

	private static int nextIncrement() {
		synchronized (increment) {
			if (increment < Integer.MAX_VALUE) {
				increment = increment + 1;
			} else {
				increment = 1;
			}
			return increment;
		}
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

	public void printQueryNK1(PatientMaster patientMaster, StringBuilder sb, CodeMap codeMap) {
		if (patientMaster != null) {

			if (StringUtils.isNotBlank(patientMaster.getGuardianRelationship()) && StringUtils.isNotBlank(patientMaster.getGuardianLast()) && StringUtils.isNotBlank(patientMaster.getGuardianFirst())) {
				Code code = codeMap.getCodeForCodeset(CodesetType.PERSON_RELATIONSHIP, patientMaster.getGuardianRelationship());
				if (code != null) {
					sb.append("NK1");
					sb.append("|1");
					sb.append("|").append(patientMaster.getGuardianLast()).append("^").append(patientMaster.getGuardianFirst()).append("^^^^^L");
					sb.append("|").append(code.getValue()).append("^").append(code.getLabel()).append("^HL70063");
					sb.append("\r");
				}
			}
		}
	}

	public void printQueryPID(PatientMaster patientReported, Set<ProcessingFlavor> processingFlavorSet, StringBuilder sb, PatientMaster patient, SimpleDateFormat sdf, int pidCount) {
		// PID
		sb.append("PID");
		// PID-1
		sb.append("|").append(pidCount);
		// PID-2
		sb.append("|");
		// PID-3
		sb.append("|").append(patient.getExternalLink()).append("^^^IIS^SR");
		if (patientReported != null) {
			sb.append("~").append(patientReported.getExternalLink()).append("^^^").append(patientReported.getPatientReportedAuthority()).append("^").append(patientReported.getPatientReportedType());
		}
		// PID-4
		sb.append("|");
		// PID-5
		String firstName = patient.getNameFirst();
		String middleName = patient.getNameMiddle();
		String lastName = patient.getNameLast();
		String motherMaiden = null;
		if (patientReported != null) {
			motherMaiden = patientReported.getMotherMaidenName();
		}
		String dateOfBirth = sdf.format(patient.getBirthDate());

		// If "PHI" flavor, strip AIRA from names 10% of the time
		if (processingFlavorSet.contains(ProcessingFlavor.PHI)) {

			if (random.nextInt(10) == 0) {
				firstName = firstName.replace("AIRA", "");
				middleName = middleName.replace("AIRA", "");
				lastName = lastName.replace("AIRA", "");
			}
			if (motherMaiden != null) {
				motherMaiden = motherMaiden.replace("AIRA", "");
			}
		}

		if (processingFlavorSet.contains(ProcessingFlavor.CITRUS)) {
			int omission = random.nextInt(3);
			if (omission == 0) {
				firstName = "";
			} else if (omission == 1) {
				lastName = "";
			} else {
				dateOfBirth = "";
			}
		}

		sb.append("|").append(lastName).append("^").append(firstName).append("^").append(middleName).append("^^^^L");

		// PID-6
		sb.append("|");
		if (StringUtils.isNotBlank(motherMaiden)) {
			sb.append(motherMaiden).append("^^^^^^M");
		}
		// PID-7
		sb.append("|").append(dateOfBirth);
		if (patientReported != null) {
			// PID-8
			{
				String sex = patientReported.getSex();
				if (!sex.equals("F") && !sex.equals("M") && !sex.equals("X")) {
					sex = "U";
				}
				sb.append("|").append(sex);
			}
			// PID-9
			sb.append("|");
			// PID-10
			sb.append("|");
			{
				String race = patientReported.getRace();
				// if processing flavor is PUNKIN then the race should be reported, and if it is null then it must be reported as UNK
				if (processingFlavorSet.contains(ProcessingFlavor.PUNKIN)) {
					CodeMap codeMap = CodeMapManager.getCodeMap();
					Code raceCode = codeMap.getCodeForCodeset(CodesetType.PATIENT_RACE, race);
					if (race.equals("") || raceCode == null || CodeStatusValue.getBy(raceCode.getCodeStatus()) != CodeStatusValue.VALID) {
						sb.append("UNK^Unknown^CDCREC");
					} else {
						sb.append(raceCode.getValue());
						sb.append("^");
						sb.append(raceCode.getLabel());
						sb.append("^CDCREC");
					}
				} else if (StringUtils.isNotBlank(race)) {
					if (processingFlavorSet.contains(ProcessingFlavor.PITAYA) || processingFlavorSet.contains(ProcessingFlavor.PERSIMMON)) {
						CodeMap codeMap = CodeMapManager.getCodeMap();
						Code raceCode = codeMap.getCodeForCodeset(CodesetType.PATIENT_RACE, race);
						if (processingFlavorSet.contains(ProcessingFlavor.PITAYA) || (raceCode != null && CodeStatusValue.getBy(raceCode.getCodeStatus()) != CodeStatusValue.VALID)) {
							sb.append(raceCode == null ? race : raceCode.getValue());
							sb.append("^");
							if (raceCode != null) {
								sb.append(raceCode.getLabel());
							}
							sb.append("^CDCREC");
						}

					}
				}
			}
			// PID-11
			sb.append("|").append(patientReported.getAddressLine1()).append("^").append(patientReported.getAddressLine2()).append("^").append(patientReported.getAddressCity()).append("^").append(patientReported.getAddressState()).append("^").append(patientReported.getAddressZip()).append("^").append(patientReported.getAddressCountry()).append("^");
			if (!processingFlavorSet.contains(ProcessingFlavor.LIME)) {
				sb.append("P");
			}
			// PID-12
			sb.append("|");
			// PID-13
			sb.append("|");
			String phone = patientReported.getPhone();
			if (phone.length() == 10) {
				sb.append("^PRN^PH^^^").append(phone, 0, 3).append("^").append(phone, 3, 10);
			}
			// PID-14
			sb.append("|");
			// PID-15
			sb.append("|");
			// PID-16
			sb.append("|");
			// PID-17
			sb.append("|");
			// PID-18
			sb.append("|");
			// PID-19
			sb.append("|");
			// PID-20
			sb.append("|");
			// PID-21
			sb.append("|");
			// PID-22
			sb.append("|");
			{
				String ethnicity = patientReported.getEthnicity();
				// if processing flavor is PUNKIN then the race should be reported, and if it is null then it must be reported as UNK
				if (processingFlavorSet.contains(ProcessingFlavor.PUNKIN)) {
					CodeMap codeMap = CodeMapManager.getCodeMap();
					Code ethnicityCode = codeMap.getCodeForCodeset(CodesetType.PATIENT_ETHNICITY, ethnicity);
					if (ethnicity.equals("") || ethnicityCode == null || CodeStatusValue.getBy(ethnicityCode.getCodeStatus()) != CodeStatusValue.VALID) {
						sb.append("UNK^Unknown^CDCREC");
					} else {
						sb.append(ethnicityCode.getValue());
						sb.append("^");
						sb.append(ethnicityCode.getLabel());
						sb.append("^CDCREC");
					}
				}
				if (StringUtils.isNotBlank(ethnicity)) {
					if (processingFlavorSet.contains(ProcessingFlavor.PITAYA) || processingFlavorSet.contains(ProcessingFlavor.PERSIMMON)) {
						CodeMap codeMap = CodeMapManager.getCodeMap();
						Code ethnicityCode = codeMap.getCodeForCodeset(CodesetType.PATIENT_ETHNICITY, ethnicity);
						if (processingFlavorSet.contains(ProcessingFlavor.PITAYA) || (ethnicityCode != null && CodeStatusValue.getBy(ethnicityCode.getCodeStatus()) != CodeStatusValue.VALID)) {
							sb.append(ethnicityCode == null ? ethnicity : ethnicityCode.getValue());

							sb.append("^");
							if (ethnicityCode != null) {
								sb.append(ethnicityCode.getLabel());
							}
							sb.append("^CDCREC");
						}
					}
				}
			}
			// PID-23
			sb.append("|");
			// PID-24
			sb.append("|");
			sb.append(patientReported.getBirthFlag());
			// PID-25
			sb.append("|");
			sb.append(patientReported.getBirthOrder());

		}
		sb.append("\r");
	}

	public void printORC(Tenant tenant, StringBuilder sb, VaccinationMaster vaccination, boolean originalReporter) {
		Set<ProcessingFlavor> processingFlavorSet = tenant.getProcessingFlavorSet();
		sb.append("ORC");
		// ORC-1
		sb.append("|RE");
		// ORC-2
		sb.append("|");
		if (vaccination != null) {
			sb.append(vaccination.getVaccinationId()).append("^IIS");
		}
		// ORC-3
		sb.append("|");
		if (vaccination == null) {
			if (processingFlavorSet.contains(ProcessingFlavor.LIME)) {
				sb.append("999^IIS");
			} else {
				sb.append("9999^IIS");
			}
		} else {
			if (originalReporter) {
				sb.append(vaccination.getExternalLink()).append("^").append(tenant.getOrganizationName());
			}
		}
		sb.append("\r");
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

	public void printObx(StringBuilder sb, int obxSetId, int obsSubId, String loinc, String loincLabel, String value) {
		sb.append("OBX");
		// OBX-1
		sb.append("|");
		sb.append(obxSetId);
		// OBX-2
		sb.append("|");
		sb.append("CE");
		// OBX-3
		sb.append("|");
		sb.append(loinc).append("^").append(loincLabel).append("^LN");
		// OBX-4
		sb.append("|");
		sb.append(obsSubId);
		// OBX-5
		sb.append("|");
		sb.append(value);
		// OBX-6
		sb.append("|");
		// OBX-7
		sb.append("|");
		// OBX-8
		sb.append("|");
		// OBX-9
		sb.append("|");
		// OBX-10
		sb.append("|");
		// OBX-11
		sb.append("|");
		sb.append("F");
		sb.append("\r");
	}

	public void printObx(StringBuilder sb, int obxSetId, int obsSubId, ObservationReported ob) {
//    ObservationReported ob = observation.getObservationReported();
		sb.append("OBX");
		// OBX-1
		sb.append("|");
		sb.append(obxSetId);
		// OBX-2
		sb.append("|");
		sb.append(ob.getValueType());
		// OBX-3
		sb.append("|");
		sb.append(ob.getIdentifierCode()).append("^").append(ob.getIdentifierLabel()).append("^").append(ob.getIdentifierTable());
		// OBX-4
		sb.append("|");
		sb.append(obsSubId);
		// OBX-5
		sb.append("|");
		if (StringUtils.isBlank(ob.getValueTable())) {
			sb.append(ob.getValueCode());
		} else {
			sb.append(ob.getValueCode()).append("^").append(ob.getValueLabel()).append("^").append(ob.getValueTable());
		}
		// OBX-6
		sb.append("|");
		if (StringUtils.isBlank(ob.getUnitsTable())) {
			sb.append(ob.getUnitsCode());
		} else {
			sb.append(ob.getUnitsCode()).append("^").append(ob.getUnitsLabel()).append("^").append(ob.getUnitsTable());
		}
		// OBX-7
		sb.append("|");
		// OBX-8
		sb.append("|");
		// OBX-9
		sb.append("|");
		// OBX-10
		sb.append("|");
		// OBX-11
		sb.append("|");
		sb.append(ob.getResultStatus());
		// OBX-12
		sb.append("|");
		// OBX-13
		sb.append("|");
		// OBX-14
		sb.append("|");
		if (ob.getObservationDate() != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			sb.append(sdf.format(ob.getObservationDate()));
		}
		// OBX-15
		sb.append("|");
		// OBX-16
		sb.append("|");
		// OBX-17
		sb.append("|");
		if (StringUtils.isBlank(ob.getMethodTable())) {
			sb.append(ob.getMethodCode());
		} else {
			sb.append(ob.getMethodCode()).append("^").append(ob.getMethodLabel()).append("^").append(ob.getMethodTable());
		}
		sb.append("\r");
	}

	public void printObx(StringBuilder sb, int obxSetId, int obsSubId, String loinc, String loincLabel, String value, String valueLabel, String valueTable) {
		sb.append("OBX");
		// OBX-1
		sb.append("|");
		sb.append(obxSetId);
		// OBX-2
		sb.append("|");
		sb.append("CE");
		// OBX-3
		sb.append("|");
		sb.append(loinc).append("^").append(loincLabel).append("^LN");
		// OBX-4
		sb.append("|");
		sb.append(obsSubId);
		// OBX-5
		sb.append("|");
		sb.append(value).append("^").append(valueLabel).append("^").append(valueTable);
		// OBX-6
		sb.append("|");
		// OBX-7
		sb.append("|");
		// OBX-8
		sb.append("|");
		// OBX-9
		sb.append("|");
		// OBX-10
		sb.append("|");
		// OBX-11
		sb.append("|");
		sb.append("F");
		sb.append("\r");
	}

	public void printObx(StringBuilder sb, int obxSetId, int obsSubId, String loinc, String loincLabel, Date value) {
		sb.append("OBX");
		// OBX-1
		sb.append("|");
		sb.append(obxSetId);
		// OBX-2
		sb.append("|");
		sb.append("DT");
		// OBX-3
		sb.append("|");
		sb.append(loinc).append("^").append(loincLabel).append("^LN");
		// OBX-4
		sb.append("|");
		sb.append(obsSubId);
		// OBX-5
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		sb.append("|");
		if (value != null) {
			sb.append(sdf.format(value));
		}
		// OBX-6
		sb.append("|");
		// OBX-7
		sb.append("|");
		// OBX-8
		sb.append("|");
		// OBX-9
		sb.append("|");
		// OBX-10
		sb.append("|");
		// OBX-11
		sb.append("|");
		sb.append("F");
		sb.append("\r");
	}

	public String printCode(String value, CodesetType codesetType, String tableName, CodeMap codeMap) {
		if (value != null) {
			Code code = codeMap.getCodeForCodeset(codesetType, value);
			if (code != null) {
				if (tableName == null) {
					return code.getValue();
				}
				return code.getValue() + "^" + code.getLabel() + "^" + tableName;
			}
		}
		return "";
	}

	public String buildAck(HL7Reader reader, List<ProcessingException> processingExceptionList, Set<ProcessingFlavor> processingFlavorSet) {
		StringBuilder sb = new StringBuilder();
		{
			String messageType = "ACK^V04^ACK";
			String profileId = Z23_ACKNOWLEDGEMENT;
			createMSH(messageType, profileId, reader, sb, processingFlavorSet);
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
			printERRSegment(pe, sb);
		}
		return sb.toString();
	}

	public void printERRSegment(ProcessingException e, StringBuilder sb) {
		sb.append("ERR|");
		sb.append("|"); // 2
		if (StringUtils.isNotBlank(e.getSegmentId())) {
			sb.append(e.getSegmentId()).append("^").append(e.getSegmentRepeat());
			if (e.getFieldPosition() > 0) {
				sb.append("^").append(e.getFieldPosition());
			}
		}
		sb.append("|101^Required field missing^HL70357"); // 3
		sb.append("|"); // 4
		if (e.isError()) {
			sb.append("E");
		} else if (e.isWarning()) {
			sb.append("W");
		} else if (e.isInformation()) {
			sb.append("I");
		}
		sb.append("|"); // 5
		sb.append("|"); // 6
		sb.append("|"); // 7
		sb.append("|").append(e.getMessage()); // 8
		sb.append("|\r");
	}

	public void createMSH(String messageType, String profileId, HL7Reader reader, StringBuilder sb, Set<ProcessingFlavor> processingFlavorSet) {
		String sendingApp = "";
		String sendingFac = "";
		String receivingApp = "";
		StringBuilder receivingFac = new StringBuilder("IIS Sandbox");
		if (processingFlavorSet != null) {
			for (ProcessingFlavor processingFlavor : ProcessingFlavor.values()) {
				if (processingFlavorSet.contains(processingFlavor)) {
					receivingFac.append(" ").append(processingFlavor.getKey());
				}
			}
		}
		receivingFac.append(" v" + SoftwareVersion.VERSION);

		reader.resetPostion();
		if (reader.advanceToSegment("MSH")) {
			sendingApp = reader.getValue(3);
			sendingFac = reader.getValue(4);
			receivingApp = reader.getValue(5);
		}


		String sendingDateString;
		{
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddhhmmssZ");
			sendingDateString = simpleDateFormat.format(new Date());
		}
		String uniqueId;
		{
			uniqueId = String.valueOf(System.currentTimeMillis()) + nextIncrement();
		}
		String production = reader.getValue(11);
		// build MSH
		sb.append("MSH|^~\\&|");
		sb.append(receivingApp).append("|");
		sb.append(receivingFac).append("|");
		sb.append(sendingApp).append("|");
		sb.append(sendingFac).append("|");
		sb.append(sendingDateString).append("|");
		sb.append("|");
		if (processingFlavorSet != null && processingFlavorSet.contains(ProcessingFlavor.MELON)) {
			int pos = messageType.indexOf("^");
			if (pos > 0) {
				messageType = messageType.substring(0, pos);
				if (System.currentTimeMillis() % 2 == 0) {
					messageType += "^ZZZ";
				}
			}
		}
		sb.append(messageType).append("|");
		sb.append(uniqueId).append("|");
		sb.append(production).append("|");
		sb.append("2.5.1|");
		sb.append("|");
		sb.append("|");
		sb.append("NE|");
		sb.append("NE|");
		sb.append("|");
		sb.append("|");
		sb.append("|");
		sb.append("|");
		sb.append(profileId).append("^CDCPHINVS\r");
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
			if (problem != null) {
				processingExceptionList.add(new ProcessingException(problem, "QPD", 1, fieldPosition));
			} else {

				patientMasterForMatchQuery.setNameFirst(patientNameFirst);
				patientMasterForMatchQuery.setNameLast(patientNameLast);
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
	public String buildRSP(HL7Reader reader, String messageRecieved, PatientMaster patientMaster, Tenant tenant, List<PatientReported> patientReportedPossibleList, List<ProcessingException> processingExceptionList) {
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
			createMSH(messageType, profileId, reader, sb, processingFlavorSet);
		}
		{
			String sendersUniqueId = reader.getValue(10);
			if (hasErrors(processingExceptionList)) {
				sb.append("MSA|AE|").append(sendersUniqueId).append("\r");
			} else {
				sb.append("MSA|AA|").append(sendersUniqueId).append("\r");
			}
			if (processingExceptionList.size() > 0) {
				printERRSegment(processingExceptionList.get(processingExceptionList.size() - 1), sb);
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
				printQueryPID(pr, processingFlavorSet, sb, patient, sdf, count);
			}
		} else if (profileId.equals(RSP_Z32_MATCH) || profileId.equals(RSP_Z42_MATCH_WITH_FORECAST)) {
			/**
			 * CONFUSING naming p but no better solution right now but to deal with single match
			 */
			PatientMaster patient = patientMaster;
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			printQueryPID(patientMaster, processingFlavorSet, sb, patient, sdf, 1);
			if (profileId.equals(RSP_Z32_MATCH)) {
				printQueryNK1(patientMaster, sb, codeMap);
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
				printORC(tenant, sb, vaccination, originalReporter);
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
				sb.append(printCode(vaccination.getVaccineMvxCode(), CodesetType.VACCINATION_MANUFACTURER_CODE, "MVX", codeMap));
				// RXA-18
				sb.append("|");
				sb.append(printCode(vaccination.getRefusalReasonCode(), CodesetType.VACCINATION_REFUSAL, "NIP002", codeMap));
				// RXA-19
				sb.append("|");
				// RXA-20
				sb.append("|");
				if (!processingFlavorSet.contains(ProcessingFlavor.LIME)) {
					String completionStatus = vaccination.getCompletionStatus();
					if (completionStatus == null || completionStatus.equals("")) {
						completionStatus = "CP";
					}
					sb.append(printCode(completionStatus, CodesetType.VACCINATION_COMPLETION, null, codeMap));
				}

				// RXA-21
				sb.append("|A");
				sb.append("\r");
				if (vaccination.getBodyRoute() != null && !vaccination.getBodyRoute().equals("")) {
					sb.append("RXR");
					// RXR-1
					sb.append("|");
					sb.append(printCode(vaccination.getBodyRoute(), CodesetType.BODY_ROUTE, "NCIT", codeMap));
					// RXR-2
					sb.append("|");
					sb.append(printCode(vaccination.getBodySite(), CodesetType.BODY_SITE, "HL70163", codeMap));
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
							printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value, valueLabel, valueTable);
						}
						{
							obxSetId++;
							String loinc = "59781-5";
							String loincLabel = "Dose validity";
							String value = evaluationActual.getDoseValid();
							String valueLabel = value;
							String valueTable = "99107";
							printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value, valueLabel, valueTable);
						}
					}
				}
				try {
					Bundle bundle = fhirClient.search().forResource(Observation.class).where(Observation.PART_OF.hasId(patientMaster.getPatientId())).and(Observation.PART_OF.hasId(vaccination.getVaccinationId())).returnBundle(Bundle.class).execute();
					if (bundle.hasEntry()) {
						obsSubId++;
						for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
//						ObservationMaster observationMaster =
//							ObservationMapper.getMaster((Observation) entry.getResource());
							ObservationReported observationReported = observationMapper.getReported(entry.getResource());
							obxSetId++;
							printObx(sb, obxSetId, obsSubId, observationReported);
						}
					}
				} catch (ResourceNotFoundException e) {
				}
			}
			try {
				Bundle bundle = fhirClient.search().forResource(Observation.class).where(Observation.PART_OF.hasId(patientMaster.getPatientId())).returnBundle(Bundle.class).execute();
				if (bundle.hasEntry()) {
					printORC(tenant, sb, null, false);
					obsSubId++;
					for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
						obxSetId++;
						ObservationReported observationReported = observationMapper.getReported(entry.getResource());
						printObx(sb, obxSetId, obsSubId, observationReported);
					}
				}
			} catch (ResourceNotFoundException e) {
			}

			if (sendBackForecast && forecastActualList != null && forecastActualList.size() > 0) {
				printORC(tenant, sb, null, false);
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
						printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value, valueLabel, valueTable);
					}
					{
						obxSetId++;
						String loinc = "59783-1";
						String loincLabel = "Status in series";
						Admin admin = forecastActual.getAdmin();
						String value = admin.getAdminStatus();
						String valueLabel = admin.getLabel();
						String valueTable = "99106";
						printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value, valueLabel, valueTable);
					}
					if (forecastActual.getDueDate() != null) {
						obxSetId++;
						String loinc = "30981-5";
						String loincLabel = "Earliest date";
						Date value = forecastActual.getValidDate();
						printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value);
					}
					if (forecastActual.getDueDate() != null) {
						obxSetId++;
						String loinc = "30980-7";
						String loincLabel = "Recommended date";
						Date value = forecastActual.getDueDate();
						printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value);
					}
					if (forecastActual.getDueDate() != null) {
						obxSetId++;
						String loinc = "59778-1";
						String loincLabel = "Latest date";
						Date value = forecastActual.getOverdueDate();
						printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value);
					}
				}
			}
		}

		String messageResponse = sb.toString();
		recordMessageReceived(messageRecieved, patientMaster, messageResponse, "Query", categoryResponse, tenant);
		return messageResponse;
	}

}
