package org.immregistries.iis.kernal.logic;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodeStatusValue;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.SoftwareVersion;
import org.immregistries.iis.kernal.mapping.Interfaces.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.Interfaces.LocationMapper;
import org.immregistries.iis.kernal.mapping.Interfaces.ObservationMapper;
import org.immregistries.iis.kernal.mapping.Interfaces.PatientMapper;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.iis.kernal.repository.FhirRequester;
import org.immregistries.iis.kernal.repository.RepositoryClientFactory;
import org.immregistries.iis.kernal.servlet.PopServlet;
import org.immregistries.iis.kernal.servlet.ServletHelper;
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

public abstract class IncomingMessageHandler<Organization extends IBaseResource> implements IIncomingMessageHandler<Organization> {

  	private final Logger logger = LoggerFactory.getLogger(IncomingMessageHandler.class);
  	@Autowired
  	protected RepositoryClientFactory repositoryClientFactory;
  	@Autowired
  	protected FhirRequester fhirRequester;
	@Autowired
	PatientMapper patientMapper;
	@Autowired
	ImmunizationMapper immunizationMapper;
	@Autowired
	ObservationMapper observationMapper;
	@Autowired
	LocationMapper locationMapper;

  protected IGenericClient getFhirClient() {
	  return repositoryClientFactory.getFhirClientFromSession();
  }

  protected static final String PATIENT_MIDDLE_NAME_MULTI = "Multi";
  // TODO:
  // Organize logic classes, need to have access classes for every object, maybe a new Access
  // package?
  // Look at names of database fields, make more consistent

  protected static final String QBP_Z34 = "Z34";
  protected static final String QBP_Z44 = "Z44";
  protected static final String RSP_Z42_MATCH_WITH_FORECAST = "Z42";
  protected static final String RSP_Z32_MATCH = "Z32";
  protected static final String RSP_Z31_MULTIPLE_MATCH = "Z31";
  protected static final String RSP_Z33_NO_MATCH = "Z33";
  protected static final String Z23_ACKNOWLEDGEMENT = "Z23";
  protected static final String QUERY_OK = "OK";
  protected static final String QUERY_NOT_FOUND = "NF";
  protected static final String QUERY_TOO_MANY = "TM";
  protected static final String QUERY_APPLICATION_ERROR = "AE";

	protected Session dataSession;

	public IncomingMessageHandler() {
		dataSession = PopServlet.getDataSession();
	}

  public void verifyNoErrors(List<ProcessingException> processingExceptionList)
      throws ProcessingException {
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

  public void recordMessageReceived(String message, PatientReported patientReported,
      String messageResponse, String categoryRequest, String categoryResponse,
      OrgMaster orgMaster) {
    MessageReceived messageReceived = new MessageReceived();
    messageReceived.setOrgMaster(orgMaster);
    messageReceived.setMessageRequest(message);
	 if (patientReported != null) {
		 messageReceived.setPatientReportedId(patientReported.getId());
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

  public void printQueryNK1(PatientReported patientReported, StringBuilder sb, CodeMap codeMap) {
    if (patientReported != null) {
      if (!patientReported.getGuardianRelationship().equals("")
          && !patientReported.getGuardianLast().equals("")
          && !patientReported.getGuardianFirst().equals("")) {
        Code code = codeMap.getCodeForCodeset(CodesetType.PERSON_RELATIONSHIP,
            patientReported.getGuardianRelationship());
        if (code != null) {
          sb.append("NK1");
          sb.append("|1");
          sb.append("|").append(patientReported.getGuardianLast()).append("^").append(patientReported.getGuardianFirst()).append("^^^^^L");
          sb.append("|").append(code.getValue()).append("^").append(code.getLabel()).append("^HL70063");
          sb.append("\r");
        }
      }
    }
  }

  public void printQueryPID(PatientReported patientReported,
      Set<ProcessingFlavor> processingFlavorSet, StringBuilder sb, PatientMaster patient,
      SimpleDateFormat sdf, int pidCount) {
    // PID
    sb.append("PID");
    // PID-1
    sb.append("|").append(pidCount);
    // PID-2
    sb.append("|");
    // PID-3
    sb.append("|").append(patient.getPatientExternalLink()).append("^^^IIS^SR");
    if (patientReported != null) {
      sb.append("~").append(patientReported.getPatientReportedExternalLink()).append("^^^").append(patientReported.getPatientReportedAuthority()).append("^").append(patientReported.getPatientReportedType());
    }
    // PID-4
    sb.append("|");
    // PID-5
    String firstName = patient.getPatientNameFirst();
    String middleName = patient.getPatientNameMiddle();
    String lastName = patient.getPatientNameLast();
    String dateOfBirth = sdf.format(patient.getPatientBirthDate());

    // If "PHI" flavor, strip AIRA from names 10% of the time
    if (processingFlavorSet.contains(ProcessingFlavor.PHI)) {
      if (random.nextInt(10) == 0) {
        firstName = firstName.replace("AIRA", "");
        middleName = middleName.replace("AIRA", "");
        lastName = lastName.replace("AIRA", "");
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
    if (patientReported != null) {
      sb.append(patientReported.getMotherMaidenName()).append("^^^^^^M");
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
        if (!race.equals("")) {
          if (processingFlavorSet.contains(ProcessingFlavor.PITAYA)
              || processingFlavorSet.contains(ProcessingFlavor.PERSIMMON)) {
            CodeMap codeMap = CodeMapManager.getCodeMap();
            Code raceCode = codeMap.getCodeForCodeset(CodesetType.PATIENT_RACE, race);
            if (processingFlavorSet.contains(ProcessingFlavor.PITAYA) || (raceCode != null
                && CodeStatusValue.getBy(raceCode.getCodeStatus()) != CodeStatusValue.VALID)) {
              sb.append(raceCode);
              sb.append("^");
              if (raceCode != null) {
                sb.append(raceCode.getDescription());
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
        if (!ethnicity.equals("")) {
          if (processingFlavorSet.contains(ProcessingFlavor.PITAYA)
              || processingFlavorSet.contains(ProcessingFlavor.PERSIMMON)) {
            CodeMap codeMap = CodeMapManager.getCodeMap();
            Code ethnicityCode =
                codeMap.getCodeForCodeset(CodesetType.PATIENT_ETHNICITY, ethnicity);
            if (processingFlavorSet.contains(ProcessingFlavor.PITAYA) || (ethnicityCode != null
                && CodeStatusValue.getBy(ethnicityCode.getCodeStatus()) != CodeStatusValue.VALID)) {
              sb.append(ethnicityCode);
              sb.append("^");
              if (ethnicityCode != null) {
                sb.append(ethnicityCode.getDescription());
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

  public void printORC(OrgAccess orgAccess, StringBuilder sb, VaccinationMaster vaccination,
      VaccinationReported vaccinationReported, boolean originalReporter) {
    Set<ProcessingFlavor> processingFlavorSet = orgAccess.getOrg().getProcessingFlavorSet();
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
        sb.append(vaccinationReported.getVaccinationReportedExternalLink()).append("^").append(orgAccess.getOrg().getOrganizationName());
      }
    }
    sb.append("\r");
  }

  public List<ForecastActual> doForecast(PatientMaster patient, PatientReported patientReported,
      CodeMap codeMap, List<VaccinationMaster> vaccinationMasterList, OrgAccess orgAccess) {
    List<ForecastActual> forecastActualList = null;
    Set<ProcessingFlavor> processingFlavorSet = orgAccess.getOrg().getProcessingFlavorSet();
    try {
      TestCase testCase = new TestCase();
      testCase.setEvalDate(new Date());
      testCase.setPatientSex(patientReported == null ? "F" : patientReported.getSex());
      testCase.setPatientDob(patient.getPatientBirthDate());
      List<TestEvent> testEventList = new ArrayList<>();
      for (VaccinationMaster vaccination : vaccinationMasterList) {
        Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE,
            vaccination.getVaccineCvxCode());
        if (cvxCode == null) {
          continue;
        }
        VaccinationReported vaccinationReported = vaccination.getVaccinationReported();
        if ("D".equals(vaccinationReported.getActionCode())) {
          continue;
        }
        int cvx = 0;
        try {
          cvx = Integer.parseInt(vaccinationReported.getVaccineCvxCode());
          TestEvent testEvent = new TestEvent(cvx, vaccinationReported.getAdministeredDate());
          testEventList.add(testEvent);
          vaccinationReported.setTestEvent(testEvent);
        } catch (NumberFormatException nfe) {
          continue;
        }
      }
      testCase.setTestEventList(testEventList);
      Software software = new Software();
      software.setServiceUrl("https://florence.immregistries.org/lonestar/forecast");
      software.setService(Service.LSVF);
      if (processingFlavorSet.contains(ProcessingFlavor.ICE)) {
        software.setServiceUrl(
            "https://florence.immregistries.org/opencds-decision-support-service/evaluate");
        software.setService(Service.ICE);
      }

      ConnectorInterface connector =
          ConnectFactory.createConnecter(software, VaccineGroup.getForecastItemList());
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

  public void printObx(StringBuilder sb, int obxSetId, int obsSubId, String loinc,
      String loincLabel, String value) {
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

  public void printObx(StringBuilder sb, int obxSetId, int obsSubId,
      ObservationReported ob) {
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
    if (ob.getValueTable().equals("")) {
      sb.append(ob.getValueCode());
    } else {
      sb.append(ob.getValueCode()).append("^").append(ob.getValueLabel()).append("^").append(ob.getValueTable());
    }
    // OBX-6
    sb.append("|");
    if (ob.getUnitsTable().equals("")) {
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
    if (ob.getMethodTable().equals("")) {
      sb.append(ob.getMethodCode());
    } else {
      sb.append(ob.getMethodCode()).append("^").append(ob.getMethodLabel()).append("^").append(ob.getMethodTable());
    }
    sb.append("\r");
  }

  public void printObx(StringBuilder sb, int obxSetId, int obsSubId, String loinc,
      String loincLabel, String value, String valueLabel, String valueTable) {
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


  public void printObx(StringBuilder sb, int obxSetId, int obsSubId, String loinc,
      String loincLabel, Date value) {
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

  public String printCode(String value, CodesetType codesetType, String tableName,
      CodeMap codeMap) {
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

  public String buildAck(HL7Reader reader, List<ProcessingException> processingExceptionList) {
    StringBuilder sb = new StringBuilder();
    {
      String messageType = "ACK^V04^ACK";
      String profileId = Z23_ACKNOWLEDGEMENT;
      createMSH(messageType, profileId, reader, sb, null);
    }

    String sendersUniqueId = "";
    reader.resetPostion();
    if (reader.advanceToSegment("MSH")) {
      sendersUniqueId = reader.getValue(10);
    } else {
      sendersUniqueId = "MSH NOT FOUND";
    }
    if (sendersUniqueId.equals("")) {
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
    if (e.getSegmentId() != null && !e.getSegmentId().equals("")) {
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

  public void createMSH(String messageType, String profileId, HL7Reader reader, StringBuilder sb,
      Set<ProcessingFlavor> processingFlavorSet) {
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
      uniqueId = "" + System.currentTimeMillis() + nextIncrement();
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

  private static Integer increment = 1;

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

  public Date parseDateWarn(String dateString, String errorMessage, String segmentId,
      int segmentRepeat, int fieldPosition, boolean strict,
      List<ProcessingException> processingExceptionList) {
    try {
      return parseDateInternal(dateString, strict);
    } catch (ParseException e) {
      if (errorMessage != null) {
        ProcessingException pe = new ProcessingException(errorMessage + ": " + e.getMessage(),
            segmentId, segmentRepeat, fieldPosition).setWarning();
        processingExceptionList.add(pe);
      }
    }
    return null;
  }

  public Date parseDateError(String dateString, String errorMessage, String segmentId,
      int segmentRepeat, int fieldPosition, boolean strict) throws ProcessingException {
    try {
      Date date = parseDateInternal(dateString, strict);
      if (date == null) {
        if (errorMessage != null) {
          throw new ProcessingException(errorMessage + ": No date was specified", segmentId,
              segmentRepeat, fieldPosition);
        }
      }
      return date;
    } catch (ParseException e) {
      if (errorMessage != null) {
        throw new ProcessingException(errorMessage + ": " + e.getMessage(), segmentId,
            segmentRepeat, fieldPosition);
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

  protected static final Random random = new Random();
  private static final char[] ID_CHARS =
      {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T',
          'U', 'V', 'W', 'X', 'Y', 'Z', '1', '2', '3', '4', '5', '6', '7', '8', '9'};


  public String generateId() {
    StringBuilder patientRegistryId = new StringBuilder();
    for (int i = 0; i < 12; i++) {
      patientRegistryId.append(ID_CHARS[random.nextInt(ID_CHARS.length)]);
    }
    return patientRegistryId.toString();
  }
}
