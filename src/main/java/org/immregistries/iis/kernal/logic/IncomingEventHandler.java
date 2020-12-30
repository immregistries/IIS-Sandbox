package org.immregistries.iis.kernal.logic;

import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgLocation;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.model.VaccinationReported;

public class IncomingEventHandler extends IncomingMessageHandler {

  private static final String ORG_LOCATION_FACILITY_CODE = "orgLocationFacilityCode";
  private static final String FUNDING_ELIGIBILITY = "fundingEligibility";
  private static final String FUNDING_SOURCE = "fundingSource";
  private static final String BODY_ROUTE = "bodyRoute";
  private static final String BODY_SITE = "bodySite";
  private static final String REFUSAL_REASON_CODE = "refusalReasonCode";
  private static final String ACTION_CODE = "actionCode";
  private static final String COMPLETION_STATUS = "completionStatus";
  private static final String EXPIRATION_DATE = "expirationDate";
  private static final String LOTNUMBER = "lotnumber";
  private static final String INFORMATION_SOURCE = "informationSource";
  private static final String ADMINISTERED_AMOUNT = "administeredAmount";
  private static final String VACCINE_MVX_CODE = "vaccineMvxCode";
  private static final String VACCINE_NDC_CODE = "vaccineNdcCode";
  private static final String VACCINE_CVX_CODE = "vaccineCvxCode";
  private static final String ADMINISTERED_DATE = "administeredDate";
  private static final String VACCINATION_REPORTED_EXTERNAL_LINK =
      "vaccinationReportedExternalLink";
  private static final String GUARDIAN_RELATIONSHIP = "guardianRelationship";
  private static final String GUARDIAN_MIDDLE = "guardianMiddle";
  private static final String GUARDIAN_FIRST = "guardianFirst";
  private static final String GUARDIAN_LAST = "guardianLast";
  private static final String REGISTRY_STATUS_INDICATOR_DATE = "registryStatusIndicatorDate";
  private static final String REGISTRY_STATUS_INDICATOR = "registryStatusIndicator";
  private static final String PROTECTION_INDICATOR_DATE = "protectionIndicatorDate";
  private static final String PROTECTION_INDICATOR = "protectionIndicator";
  private static final String PUBLICITY_INDICATOR_DATE = "publicityIndicatorDate";
  private static final String PUBLICITY_INDICATOR = "publicityIndicator";
  private static final String PATIENT_DEATH_DATE = "patientDeathDate";
  private static final String PATIENT_DEATH_FLAG = "patientDeathFlag";
  private static final String PATIENT_BIRTH_ORDER = "patientBirthOrder";
  private static final String PATIENT_BIRTH_FLAG = "patientBirthFlag";
  private static final String PATIENT_ETHNICITY = "patientEthnicity";
  private static final String PATIENT_EMAIL = "patientEmail";
  private static final String PATIENT_PHONE = "patientPhone";
  private static final String PATIENT_ADDRESS_COUNTY_PARISH = "patientAddressCountyParish";
  private static final String PATIENT_ADDRESS_COUNTRY = "patientAddressCountry";
  private static final String PATIENT_ADDRESS_ZIP = "patientAddressZip";
  private static final String PATIENT_ADDRESS_STATE = "patientAddressState";
  private static final String PATIENT_ADDRESS_CITY = "patientAddressCity";
  private static final String PATIENT_ADDRESS_LINE2 = "patientAddressLine2";
  private static final String PATIENT_ADDRESS_LINE1 = "patientAddressLine1";
  private static final String PATIENT_RACE6 = "patientRace6";
  private static final String PATIENT_RACE5 = "patientRace5";
  private static final String PATIENT_RACE4 = "patientRace4";
  private static final String PATIENT_RACE3 = "patientRace3";
  private static final String PATIENT_RACE2 = "patientRace2";
  private static final String PATIENT_RACE = "patientRace";
  private static final String PATIENT_SEX = "patientSex";
  private static final String PATIENT_BIRTH_DATE = "patientBirthDate";
  private static final String PATIENT_MOTHER_MAIDEN = "patientMotherMaiden";
  private static final String PATIENT_NAME_MIDDLE = "patientNameMiddle";
  private static final String PATIENT_NAME_FIRST = "patientNameFirst";
  private static final String PATIENT_NAME_LAST = "patientNameLast";
  private static final String PATIENT_REPORTED_TYPE = "patientReportedType";
  private static final String PATIENT_REPORTED_AUTHORITY = "patientReportedAuthority";
  private static final String PATIENT_REPORTED_EXTERNAL_LINK = "patientReportedExternalLink";

  public static final String[] PARAMS_PATIENT = new String[] {PATIENT_REPORTED_EXTERNAL_LINK,
      PATIENT_REPORTED_AUTHORITY, PATIENT_REPORTED_TYPE, PATIENT_NAME_LAST, PATIENT_NAME_FIRST,
      PATIENT_NAME_MIDDLE, PATIENT_MOTHER_MAIDEN, PATIENT_BIRTH_DATE, PATIENT_SEX, PATIENT_RACE,
      PATIENT_RACE2, PATIENT_RACE3, PATIENT_RACE4, PATIENT_RACE5, PATIENT_RACE6,
      PATIENT_ADDRESS_LINE1, PATIENT_ADDRESS_LINE2, PATIENT_ADDRESS_CITY, PATIENT_ADDRESS_STATE,
      PATIENT_ADDRESS_ZIP, PATIENT_ADDRESS_COUNTRY, PATIENT_ADDRESS_COUNTY_PARISH, PATIENT_PHONE,
      PATIENT_EMAIL, PATIENT_ETHNICITY, PATIENT_BIRTH_FLAG, PATIENT_BIRTH_ORDER, PATIENT_DEATH_FLAG,
      PATIENT_DEATH_DATE, PUBLICITY_INDICATOR, PUBLICITY_INDICATOR_DATE, PROTECTION_INDICATOR,
      PROTECTION_INDICATOR_DATE, REGISTRY_STATUS_INDICATOR, REGISTRY_STATUS_INDICATOR_DATE,
      GUARDIAN_LAST, GUARDIAN_FIRST, GUARDIAN_MIDDLE, GUARDIAN_RELATIONSHIP};

  public static final String[] PARAMS_VACCINATION =
      new String[] {VACCINATION_REPORTED_EXTERNAL_LINK, ADMINISTERED_DATE, VACCINE_CVX_CODE,
          VACCINE_NDC_CODE, VACCINE_MVX_CODE, ADMINISTERED_AMOUNT, INFORMATION_SOURCE, LOTNUMBER,
          EXPIRATION_DATE, COMPLETION_STATUS, ACTION_CODE, REFUSAL_REASON_CODE, BODY_SITE,
          BODY_ROUTE, FUNDING_SOURCE, FUNDING_ELIGIBILITY, ORG_LOCATION_FACILITY_CODE};

  public IncomingEventHandler(Session dataSession) {
    super(dataSession);
  }

  public String process(HttpServletRequest req, OrgAccess orgAccess) {
    try {
      processEvent(orgAccess, req);
    } catch (Exception e) {
      e.printStackTrace(System.err);
      return "Exception processing request" + e.getMessage();
    }

    return "OK";
  }


  public void processEvent(OrgAccess orgAccess, HttpServletRequest req) throws Exception {
    CodeMap codeMap = CodeMapManager.getCodeMap();
    PatientReported patientReported = processPatient(orgAccess, req, codeMap);
    VaccinationReported vaccinationReported = null;
    VaccinationMaster vaccination = null;
    Date administrationDate = null;
    String vaccinationReportedExternalLink = req.getParameter(VACCINATION_REPORTED_EXTERNAL_LINK);
    if (vaccinationReportedExternalLink.equals("")) {
      throw new Exception("Vaccination order id was not found, unable to process");
    }
    administrationDate = parseDateInternal(req.getParameter(ADMINISTERED_DATE), true);
    if (administrationDate.after(new Date())) {
      throw new Exception(
          "Vaccination is indicated as occuring in the future, unable to accept future vaccination events");
    }
    {
      Query query = dataSession.createQuery(
          "from VaccinationReported where patientReported = ? and vaccinationReportedExternalLink = ?");
      query.setParameter(0, patientReported);
      query.setParameter(1, vaccinationReportedExternalLink);
      List<VaccinationReported> vaccinationReportedList = query.list();
      if (vaccinationReportedList.size() > 0) {
        vaccinationReported = vaccinationReportedList.get(0);
        vaccination = vaccinationReported.getVaccination();
      }
    }
    if (vaccinationReported == null) {
      vaccination = new VaccinationMaster();
      vaccinationReported = new VaccinationReported();
      vaccinationReported.setVaccination(vaccination);
      vaccination.setVaccinationReported(null);
      vaccinationReported.setReportedDate(new Date());
      vaccinationReported.setVaccinationReportedExternalLink(vaccinationReportedExternalLink);
    }
    vaccinationReported.setPatientReported(patientReported);
    vaccination.setPatient(patientReported.getPatient());

    String vaccineCvxCode = req.getParameter(VACCINE_CVX_CODE);
    String vaccineNdcCode = req.getParameter(VACCINE_NDC_CODE);

    {
      Code ndcCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_NDC_CODE, vaccineNdcCode);
      if (ndcCode != null) {
        if (ndcCode.getCodeStatus() != null && ndcCode.getCodeStatus().getDeprecated() != null
            && ndcCode.getCodeStatus().getDeprecated().getNewCodeValue() != null
            && !ndcCode.getCodeStatus().getDeprecated().getNewCodeValue().equals("")) {
          vaccineNdcCode = ndcCode.getCodeStatus().getDeprecated().getNewCodeValue();
        }
        Code cvxCode = codeMap.getRelatedCode(ndcCode, CodesetType.VACCINATION_CVX_CODE);
        if (cvxCode != null && vaccineCvxCode.equals("")) {
          vaccineCvxCode = cvxCode.getValue();
        }
      }
    }
    if (vaccineCvxCode.equals("")) {
      throw new Exception("Unable to find a recognized vaccine administration code (CVX or NDC)");
    } else {
      Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, vaccineCvxCode);
      if (cvxCode != null) {
        vaccineCvxCode = cvxCode.getValue();
      } else {
        throw new Exception("Unrecognized CVX vaccine '" + vaccineCvxCode + "'");
      }
    }

    {
      String administeredAtLocation = req.getParameter(ORG_LOCATION_FACILITY_CODE);
      if (StringUtils.isNotEmpty(administeredAtLocation)) {
        Query query = dataSession.createQuery(
            "from OrgLocation where orgMaster = :orgMaster and orgFacilityCode = :orgFacilityCode");
        query.setParameter("orgMaster", orgAccess.getOrg());
        query.setParameter("orgFacilityCode", administeredAtLocation);
        List<OrgLocation> orgMasterList = query.list();
        OrgLocation orgLocation = null;
        if (orgMasterList.size() > 0) {
          orgLocation = orgMasterList.get(0);
        }

        if (orgLocation == null) {
          orgLocation = new OrgLocation();
          orgLocation.setOrgFacilityCode(administeredAtLocation);
          orgLocation.setOrgMaster(orgAccess.getOrg());
          orgLocation.setOrgFacilityName(administeredAtLocation);
          orgLocation.setLocationType("");
          orgLocation.setAddressLine1("");
          orgLocation.setAddressLine2("");
          orgLocation.setAddressCity("");
          orgLocation.setAddressState("");
          orgLocation.setAddressZip("");
          orgLocation.setAddressCountry("");
          Transaction transaction = dataSession.beginTransaction();
          dataSession.save(orgLocation);
          transaction.commit();
        }
        vaccinationReported.setOrgLocation(orgLocation);
      }
    }
    vaccination.setVaccineCvxCode(vaccineCvxCode);
    vaccination.setAdministeredDate(administrationDate);
    vaccinationReported.setUpdatedDate(new Date());
    vaccinationReported.setAdministeredDate(administrationDate);
    vaccinationReported.setVaccineCvxCode(vaccineCvxCode);
    vaccinationReported.setVaccineNdcCode(vaccineNdcCode);
    vaccinationReported.setAdministeredAmount(req.getParameter(ADMINISTERED_AMOUNT));
    vaccinationReported.setInformationSource(req.getParameter(INFORMATION_SOURCE));
    vaccinationReported.setLotnumber(req.getParameter(LOTNUMBER));
    vaccinationReported
        .setExpirationDate(parseDateInternal(req.getParameter(EXPIRATION_DATE), true));
    vaccinationReported.setVaccineMvxCode(req.getParameter(VACCINE_MVX_CODE));
    vaccinationReported.setRefusalReasonCode(req.getParameter(REFUSAL_REASON_CODE));
    vaccinationReported.setCompletionStatus(req.getParameter(COMPLETION_STATUS));
    vaccinationReported.setActionCode(req.getParameter(ACTION_CODE));
    vaccinationReported.setBodyRoute(req.getParameter(BODY_ROUTE));
    vaccinationReported.setBodySite(req.getParameter(BODY_SITE));
    if (vaccinationReported.getAdministeredDate().before(patientReported.getPatientBirthDate())) {
      throw new Exception(
          "Vaccination is reported as having been administered before the patient was born");
    }

    vaccinationReported.setFundingEligibility(req.getParameter(FUNDING_ELIGIBILITY));
    vaccinationReported.setFundingSource(req.getParameter(FUNDING_SOURCE));


    {
      Transaction transaction = dataSession.beginTransaction();
      dataSession.saveOrUpdate(vaccination);
      dataSession.saveOrUpdate(vaccinationReported);
      vaccination.setVaccinationReported(vaccinationReported);
      dataSession.saveOrUpdate(vaccination);
      transaction.commit();
    }

  }

  public PatientReported processPatient(OrgAccess orgAccess, HttpServletRequest req,
      CodeMap codeMap) throws Exception {
    PatientReported patientReported = null;
    PatientMaster patient = null;

    String patientReportedExternalLink = req.getParameter(PATIENT_REPORTED_EXTERNAL_LINK);
    String patientReportedAuthority = req.getParameter(PATIENT_REPORTED_AUTHORITY);
    String patientReportedType = req.getParameter(PATIENT_REPORTED_TYPE);
    if (StringUtils.isEmpty(patientReportedExternalLink)) {
      throw new Exception("Patient external link must be indicated");
    }

    {
      Query query = dataSession.createQuery(
          "from PatientReported where orgReported = ? and patientReportedExternalLink = ?");
      query.setParameter(0, orgAccess.getOrg());
      query.setParameter(1, patientReportedExternalLink);
      List<PatientReported> patientReportedList = query.list();
      if (patientReportedList.size() > 0) {
        patientReported = patientReportedList.get(0);
        patient = patientReported.getPatient();
      }
    }

    if (patientReported == null) {
      patient = new PatientMaster();
      patient.setPatientExternalLink(generatePatientExternalLink());
      patient.setOrgMaster(orgAccess.getOrg());
      patientReported = new PatientReported();
      patientReported.setOrgReported(orgAccess.getOrg());
      patientReported.setPatientReportedExternalLink(patientReportedExternalLink);
      patientReported.setPatient(patient);
      patientReported.setReportedDate(new Date());
    }


    String patientNameLast = req.getParameter(PATIENT_NAME_LAST);
    String patientNameFirst = req.getParameter(PATIENT_NAME_FIRST);
    String patientNameMiddle = req.getParameter(PATIENT_NAME_MIDDLE);
    String patientPhone = req.getParameter(PATIENT_PHONE);

    if (patientNameLast.equals("")) {
      throw new Exception(
          "Patient last name was not found, required for accepting patient and vaccination history");
    }
    if (patientNameFirst.equals("")) {
      throw new Exception(
          "Patient first name was not found, required for accepting patient and vaccination history");
    }


    String zip = req.getParameter(PATIENT_ADDRESS_ZIP);
    if (zip.length() > 5) {
      zip = zip.substring(0, 5);
    }
    String addressFragPrep = req.getParameter(PATIENT_ADDRESS_LINE1);
    String addressFrag = "";
    {
      int spaceIndex = addressFragPrep.indexOf(" ");
      if (spaceIndex > 0) {
        addressFragPrep = addressFragPrep.substring(0, spaceIndex);
      }
      addressFrag = zip + ":" + addressFragPrep;
    }
    Date patientBirthDate = parseDateInternal(req.getParameter(PATIENT_BIRTH_DATE), true);

    if (patientBirthDate.after(new Date())) {
      throw new Exception(
          "Patient is indicated as being born in the future, unable to record patients who are not yet born");
    }
    patient.setPatientAddressFrag(addressFrag);
    patient.setPatientNameLast(patientNameLast);
    patient.setPatientNameFirst(patientNameFirst);
    patient.setPatientNameMiddle(patientNameMiddle);
    patient.setPatientPhoneFrag(patientPhone);
    patient.setPatientBirthDate(patientBirthDate);
    patient.setPatientSoundexFirst(""); // TODO, later
    patient.setPatientSoundexLast(""); // TODO, later
    patientReported.setPatientReportedExternalLink(patientReportedExternalLink);
    patientReported.setPatientReportedType(patientReportedType);
    patientReported.setPatientNameFirst(patientNameFirst);
    patientReported.setPatientNameLast(patientNameLast);
    patientReported.setPatientNameMiddle(patientNameMiddle);
    patientReported.setPatientMotherMaiden(req.getParameter(PATIENT_MOTHER_MAIDEN));
    patientReported.setPatientBirthDate(patientBirthDate);
    patientReported.setPatientSex(req.getParameter(PATIENT_SEX));
    patientReported.setPatientRace(req.getParameter(PATIENT_RACE));
    patientReported.setPatientRace2(req.getParameter(PATIENT_RACE2));
    patientReported.setPatientRace3(req.getParameter(PATIENT_RACE3));
    patientReported.setPatientRace4(req.getParameter(PATIENT_RACE4));
    patientReported.setPatientRace5(req.getParameter(PATIENT_RACE5));
    patientReported.setPatientRace6(req.getParameter(PATIENT_RACE6));
    patientReported.setPatientAddressLine1(req.getParameter(PATIENT_ADDRESS_LINE1));
    patientReported.setPatientAddressLine2(req.getParameter(PATIENT_ADDRESS_LINE2));
    patientReported.setPatientAddressCity(req.getParameter(PATIENT_ADDRESS_CITY));
    patientReported.setPatientAddressState(req.getParameter(PATIENT_ADDRESS_STATE));
    patientReported.setPatientAddressZip(req.getParameter(PATIENT_ADDRESS_ZIP));
    patientReported.setPatientAddressCountry(req.getParameter(PATIENT_ADDRESS_COUNTRY));
    patientReported.setPatientAddressCountyParish(req.getParameter(PATIENT_ADDRESS_COUNTY_PARISH));
    patientReported.setPatientEthnicity(req.getParameter(PATIENT_ETHNICITY));
    patientReported.setPatientBirthFlag(req.getParameter(PATIENT_BIRTH_FLAG));
    patientReported.setPatientBirthOrder(req.getParameter(PATIENT_BIRTH_ORDER));
    patientReported
        .setPatientDeathDate(parseDateInternal(req.getParameter(PATIENT_DEATH_DATE), true));
    patientReported.setPatientDeathFlag(req.getParameter(PATIENT_DEATH_FLAG));
    patientReported.setPatientEmail(req.getParameter(PATIENT_EMAIL));
    patientReported.setPatientPhone(patientPhone);
    patientReported.setPatientReportedAuthority(patientReportedAuthority);
    patientReported.setPublicityIndicator(req.getParameter(PUBLICITY_INDICATOR));
    patientReported.setProtectionIndicator(req.getParameter(PROTECTION_INDICATOR));
    patientReported.setProtectionIndicatorDate(
        parseDateInternal(req.getParameter(PROTECTION_INDICATOR_DATE), true));
    patientReported.setRegistryStatusIndicator(req.getParameter(REGISTRY_STATUS_INDICATOR));
    patientReported.setRegistryStatusIndicatorDate(
        parseDateInternal(req.getParameter(PROTECTION_INDICATOR_DATE), true));
    patientReported.setPublicityIndicatorDate(
        parseDateInternal(req.getParameter(PUBLICITY_INDICATOR_DATE), true));
    patientReported.setGuardianLast(req.getParameter(GUARDIAN_LAST));
    patientReported.setGuardianFirst(req.getParameter(GUARDIAN_FIRST));
    patientReported.setGuardianMiddle(req.getParameter(GUARDIAN_MIDDLE));
    patientReported.setGuardianRelationship(req.getParameter(GUARDIAN_RELATIONSHIP));
    patientReported.setUpdatedDate(new Date());
    {
      Transaction transaction = dataSession.beginTransaction();
      dataSession.saveOrUpdate(patient);
      dataSession.saveOrUpdate(patientReported);
      transaction.commit();
    }
    return patientReported;
  }



}
