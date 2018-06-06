package org.immregistries.iis.kernal.logic;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.immregistries.dqa.codebase.client.CodeMap;
import org.immregistries.dqa.codebase.client.generated.Code;
import org.immregistries.dqa.codebase.client.reference.CodesetType;
import org.immregistries.dqa.hl7util.parser.HL7Reader;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.model.VaccinationReported;

public class IncomingMessageHandler {
  // TODO:
  // Organize logic classes, need to have access classes for every object, maybe a new Access
  // package?
  // Look at names of database fields, make more consistent

  private Session dataSession = null;

  public IncomingMessageHandler(Session dataSession) {
    this.dataSession = dataSession;
  }

  public String process(String message, OrgAccess orgAccess) {
    HL7Reader reader = new HL7Reader(message);
    String messageType = reader.getValue(9);
    if (messageType.equals("VXU")) {
      return processVXU(orgAccess, reader);
    } else if (messageType.equals("QBP")) {
      return processQBP(orgAccess, reader);
    } else {
      ProcessingException e = new ProcessingException("Unsupported message", "", 0, 0);
      return buildAck(reader, e);
    }
  }

  public String processQBP(OrgAccess orgAccess, HL7Reader reader) {
    PatientReported patientReported = null;
    PatientMaster patient = null;
    if (reader.advanceToSegment("QPD")) {
      String mrn = "";
      {
        mrn = reader.getValueBySearchingRepeats(3, 1, "MR", 5);
        if (mrn.equals("")) {
          mrn = reader.getValueBySearchingRepeats(3, 1, "PT", 5);
        }
      }
      if (!mrn.equals("")) {
        Query query = dataSession.createQuery(
            "from PatientReported where orgReported = ? and patientReportedExternalLink = ?");
        query.setParameter(0, orgAccess.getOrg());
        query.setParameter(1, mrn);
        List<PatientReported> patientReportedList = query.list();
        if (patientReportedList.size() > 0) {
          patientReported = patientReportedList.get(0);
          patient = patientReported.getPatient();
        }
      }
      String patientNameLast = reader.getValue(4, 1);
      String patientNameFirst = reader.getValue(4, 2);
      String patientNameMiddle = reader.getValue(4, 3);
      Date patientBirthDate = parseDate(reader.getValue(6));
      String patientSex = reader.getValue(7);
      if (patientReported != null) {
        int points = 0;
        if (!patientNameLast.equals("")
            && patientNameLast.equalsIgnoreCase(patientReported.getPatientNameLast())) {
          points = points + 2;
        }
        if (!patientNameFirst.equals("")
            && patientNameFirst.equalsIgnoreCase(patientReported.getPatientNameFirst())) {
          points = points + 2;
        }
        if (!patientNameMiddle.equals("")
            && patientNameMiddle.equalsIgnoreCase(patientReported.getPatientNameFirst())) {
          points = points + 2;
        }
        if (patientBirthDate != null
            && patientBirthDate.equals(patientReported.getPatientBirthDate())) {
          points = points + 2;
        }
        if (!patientSex.equals("")
            && patientSex.equalsIgnoreCase(patientReported.getPatientSex())) {
          points = points + 2;
        }
        if (points < 6) {
          // not enough matching so don't indicate this as a match
          patientReported = null;
        }
      }
    }

    return buildRSP(reader, patient, patientReported, orgAccess);
  }

  public String processVXU(OrgAccess orgAccess, HL7Reader reader) {
    try {
      String patientReportedExternalLink = "";
      String patientReportedAuthority = "";
      String patientReportedType = "MR";
      if (reader.advanceToSegment("PID")) {
        patientReportedExternalLink =
            reader.getValueBySearchingRepeats(3, 1, patientReportedType, 5);
        patientReportedAuthority = reader.getValueBySearchingRepeats(3, 4, patientReportedType, 5);
        if (patientReportedExternalLink.equals("")) {
          patientReportedAuthority = "";
          patientReportedType = "PT";
          patientReportedExternalLink =
              reader.getValueBySearchingRepeats(3, 1, patientReportedType, 5);
          patientReportedAuthority =
              reader.getValueBySearchingRepeats(3, 4, patientReportedType, 5);
          if (patientReportedExternalLink.equals("")) {
            throw new ProcessingException(
                "MRN was not found, required for accepting vaccination report", "PID", 1, 3);
          }
        }
      } else {
        throw new ProcessingException(
            "No PID segment found, required for accepting vaccination report", "", 0, 0);
      }


      PatientReported patientReported = null;
      PatientMaster patient = null;
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
        patientReported = new PatientReported();
        patientReported.setOrgReported(orgAccess.getOrg());
        patientReported.setPatientReportedExternalLink(patientReportedExternalLink);
        patientReported.setPatient(patient);
        patientReported.setReportedDate(new Date());
      }

      {
        String patientNameLast = reader.getValue(5, 1);
        String patientNameFirst = reader.getValue(5, 2);
        String patientNameMiddle = reader.getValue(5, 3);
        String patientPhone = reader.getValue(13, 6) + reader.getValue(13, 7);


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
        patientBirthDate =
            parseDate(reader.getValue(7), "Bad format for date of birth", "PID", 1, 7);
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
        patientReported.setPatientMotherMaiden(reader.getValue(6));
        patientReported.setPatientBirthDate(patientBirthDate);
        patientReported.setPatientSex(reader.getValue(8));
        patientReported.setPatientRace(reader.getValue(10));
        patientReported.setPatientAddressLine1(reader.getValue(11, 1));
        patientReported.setPatientAddressLine2(reader.getValue(11, 2));
        patientReported.setPatientAddressCity(reader.getValue(11, 3));
        patientReported.setPatientAddressState(reader.getValue(11, 4));
        patientReported.setPatientAddressZip(reader.getValue(11, 5));
        patientReported.setPatientAddressCountry(reader.getValue(11, 6));
        patientReported.setPatientAddressCountyParish(reader.getValue(11, 9));
        patientReported.setPatientEthnicity(reader.getValue(22));
        patientReported.setPatientBirthFlag(reader.getValue(24));
        patientReported.setPatientBirthOrder(reader.getValue(25));
        patientReported.setPatientDeathDate(parseDate(reader.getValue(29)));
        patientReported.setPatientDeathFlag(reader.getValue(30));
        patientReported.setPatientEmail(reader.getValueBySearchingRepeats(13, 4, "NET", 2));
        patientReported.setPatientPhone(patientPhone);
        patientReported.setPatientReportedAuthority(patientReportedAuthority);
      }
      if (reader.advanceToSegment("PD1")) {
        patientReported.setPublicityIndicator(reader.getValue(11));
        patientReported.setProtectionIndicator(reader.getValue(12));
        patientReported.setProtectionIndicatorDate(parseDate(reader.getValue(13)));
        patientReported.setRegistryStatusIndicator(reader.getValue(16));
        patientReported.setRegistryStatusIndicatorDate(parseDate(reader.getValue(17)));
        patientReported.setPublicityIndicatorDate(parseDate(reader.getValue(18)));
      }
      reader.resetPostion();
      while (reader.advanceToSegment("NK1")) {
        patientReported.setGuardianLast(reader.getValue(2, 1));
        patientReported.setGuardianFirst(reader.getValue(2, 2));
        patientReported.setGuardianMiddle(reader.getValue(2, 1));
        String guardianRelationship = reader.getValue(3);
        patientReported.setGuardianRelationship(guardianRelationship);
        if (guardianRelationship.equals("MTH") || guardianRelationship.equals("FTH")
            || guardianRelationship.equals("GRD")) {
          break;
        }
      }
      reader.resetPostion();

      patientReported.setUpdatedDate(new Date());
      {
        Transaction transaction = dataSession.beginTransaction();
        dataSession.saveOrUpdate(patient);
        dataSession.saveOrUpdate(patientReported);
        transaction.commit();
      }
      int orcCount = 0;
      int rxaCount = 0;
      while (reader.advanceToSegment("ORC")) {
        orcCount++;
        VaccinationReported vaccinationReported = null;
        VaccinationMaster vaccination = null;
        String vaccineCode = "";
        Date administrationDate = null;
        String vaccinationReportedExternalLink = reader.getValue(3);
        if (reader.advanceToSegment("RXA", "ORC")) {
          rxaCount++;
          vaccineCode = reader.getValue(5, 1);
          if (vaccineCode.equals("")) {
            throw new ProcessingException("Vaccine code is not indicated in RXA-5.1", "RXA",
                rxaCount, 5);
          }
          if (vaccineCode.equals("998")) {
            continue;
          }
          if (vaccinationReportedExternalLink.equals("")) {
            throw new ProcessingException("Vaccination order id was not found, unable to process",
                "ORC", orcCount, 3);
          }
          administrationDate = parseDate(reader.getValue(3, 1),
              "Could not read administered date in RXA-5", "RXA", rxaCount, 5);
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
          vaccination.setPatient(patient);
          CodeMap codeMap = CodeMapManager.getCodeMap();
          String vaccineCvxCode = "";
          String vaccineNdcCode = "";
          String vaccineCodeType = reader.getValue(5, 3);
          if (vaccineCodeType.equals("NDC")) {
            vaccineNdcCode = vaccineCode;
            Code ndcCode =
                codeMap.getCodeForCodeset(CodesetType.VACCINATION_NDC_CODE, vaccineNdcCode);
            if (ndcCode != null) {
              if (ndcCode.getCodeStatus() != null && ndcCode.getCodeStatus().getDeprecated() != null
                  && ndcCode.getCodeStatus().getDeprecated().getNewCodeValue() != null
                  && !ndcCode.getCodeStatus().getDeprecated().getNewCodeValue().equals("")) {
                vaccineNdcCode = ndcCode.getCodeStatus().getDeprecated().getNewCodeValue();
              }
              Code cvxCode = codeMap.getRelatedCode(ndcCode, CodesetType.VACCINATION_CVX_CODE);
              if (cvxCode != null) {
                vaccineCvxCode = cvxCode.getValue();
              }
            }
          } else if (vaccineCodeType.equals("CPT")) {
            Code cptCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CPT_CODE, vaccineCode);
            if (cptCode != null) {
              Code cvxCode = codeMap.getRelatedCode(cptCode, CodesetType.VACCINATION_CVX_CODE);
              if (cvxCode != null) {
                vaccineCvxCode = cvxCode.getValue();
              }
            }
          } else {
            // assume this is CVX
            Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, vaccineCode);
            if (cvxCode != null) {
              vaccineCvxCode = cvxCode.getValue();
            }

          }
          if (vaccineCvxCode.equals("")) {
            throw new ProcessingException(
                "Unrecognized vaccine " + vaccineCodeType + " code '" + vaccineCode + "'", "RXA",
                rxaCount, 5);
          }
          vaccination.setVaccineCvxCode(vaccineCvxCode);
          vaccination.setAdministeredDate(administrationDate);
          vaccinationReported.setUpdatedDate(new Date());
          vaccinationReported.setAdministeredDate(administrationDate);
          vaccinationReported.setVaccineCvxCode(vaccineCvxCode);
          vaccinationReported.setVaccineNdcCode(vaccineNdcCode);
          vaccinationReported.setAdministeredAmount(reader.getValue(6));
          vaccinationReported.setInformationSource(reader.getValue(9));
          vaccinationReported.setLotnumber(reader.getValue(15));
          vaccinationReported.setExpirationDate(parseDate(reader.getValue(16)));
          vaccinationReported.setVaccineMvxCode(reader.getValue(17));
          vaccinationReported.setRefusalReasonCode(reader.getValue(18));
          vaccinationReported.setCompletionStatus(reader.getValue(20));
          vaccinationReported.setActionCode(reader.getValue(21));
          int segmentPosition = reader.getSegmentPosition();
          if (reader.advanceToSegment("RXR", "ORC")) {
            vaccinationReported.setBodyRoute(reader.getValue(1));
            vaccinationReported.setBodySite(reader.getValue(2));
          }
          reader.gotoSegmentPosition(segmentPosition);
          while (reader.advanceToSegment("OBX", "ORC")) {
            String indicator = reader.getValue(3);
            if (indicator.equals("64994-7")) {
              vaccinationReported.setFundingEligibility(reader.getValue(5));
            } else if (indicator.equals("30963-3")) {
              vaccinationReported.setFundingSource(reader.getValue(5));
            }
          }
          reader.gotoSegmentPosition(segmentPosition);
          {
            Transaction transaction = dataSession.beginTransaction();
            dataSession.saveOrUpdate(vaccination);
            dataSession.saveOrUpdate(vaccinationReported);
            vaccination.setVaccinationReported(vaccinationReported);
            dataSession.saveOrUpdate(vaccination);
            transaction.commit();
          }
        }
      }
      return buildAck(reader, null);
    } catch (ProcessingException e) {
      return buildAck(reader, e);
    }
  }

  public String buildRSP(HL7Reader reader, PatientMaster patient, PatientReported patientReported,
      OrgAccess orgAccess) {
    reader.resetPostion();

    StringBuilder sb = new StringBuilder();
    String profileIdSubmitted = reader.getValue(21);
    CodeMap codeMap = CodeMapManager.getCodeMap();
    {
      String messageType = "RSP^K11^RSP_K11";
      String profileId = "Z32";
      if (patient == null) {
        profileId = "Z33";
      } else if (profileIdSubmitted.equals("Z34")) {
        profileId = "Z32";
      } else if (profileIdSubmitted.equals("Z44")) {
        profileId = "Z42";
      }
      createMSH(messageType, profileId, reader, sb);
    }
    {
      String sendersUniqueId = reader.getValue(10);
      sb.append("MSA|AA|" + sendersUniqueId + "\r");
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
      String queryId = "";
      if (reader.advanceToSegment("QPD")) {
        queryId = reader.getValue(2);
      }
      sb.append("QAK|" + queryId + "|");
      if (patient == null) {
        sb.append("NF|");
      } else {
        sb.append("OK|");
      }
      sb.append("" + profileIdSubmitted + "^" + profileName + "^CDCPHINVS\r");
    }
    if (reader.advanceToSegment("QPD")) {
      sb.append(reader.getOriginalSegment() + "\r");
    }
    if (patient != null) {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
      // PID
      sb.append("PID");
      // PID-1
      sb.append("|1");
      // PID-2
      sb.append("|");
      // PID-3
      sb.append("|" + patient.getPatientExternalLink() + "^^^IIS^SR");
      if (patientReported != null) {
        sb.append("~" + patientReported.getPatientReportedExternalLink() + "^^^"
            + patientReported.getPatientReportedAuthority() + "^"
            + patientReported.getPatientReportedType());
      }
      // PID-4
      sb.append("|");
      // PID-5
      sb.append("|" + patient.getPatientNameLast() + "^" + patient.getPatientNameFirst() + "^"
          + patient.getPatientNameMiddle() + "^^^^L");
      // PID-6
      if (patientReported != null) {
        sb.append("|" + patientReported.getPatientMotherMaiden() + "^^^^^^M");
      }
      // PID-7
      sb.append("|" + sdf.format(patient.getPatientBirthDate()));
      if (patientReported != null) {
        // PID-8
        sb.append("|" + patientReported.getPatientSex());
        // PID-9
        sb.append("|");
        // PID-10
        sb.append("|");
        // PID-11
        sb.append("|" + patientReported.getPatientAddressLine1() + "^"
            + patientReported.getPatientAddressLine2() + "^"
            + patientReported.getPatientAddressCity() + "^"
            + patientReported.getPatientAddressState() + "^"
            + patientReported.getPatientAddressZip() + "^"
            + patientReported.getPatientAddressCountry());
        // PID-12
        sb.append("|");
        // PID-13
        sb.append("|");
        String phone = patientReported.getPatientPhone();
        if (phone.length() == 10) {
          sb.append("^PRN^PH^^^" + phone.substring(0, 3) + "^" + phone.substring(3, 10));
        }
      }
      sb.append("\r");
      if (patientReported != null) {
        if (!patientReported.getGuardianRelationship().equals("")
            && !patientReported.getGuardianLast().equals("")
            && !patientReported.getGuardianFirst().equals("")) {
          Code code = codeMap.getCodeForCodeset(CodesetType.PERSON_RELATIONSHIP,
              patientReported.getGuardianRelationship());
          if (code != null) {
            sb.append("NK1");
            sb.append("|1");
            sb.append("|" + patientReported.getGuardianLast() + "^"
                + patientReported.getGuardianFirst() + "^^^^^L");
            sb.append("|" + code.getValue() + "^" + code.getLabel() + "^HL70063");
            sb.append("\r");
          }
        }
      }
      List<VaccinationMaster> vaccinationMasterList;
      {
        Query query = dataSession.createQuery("from VaccinationMaster where patient = ?");
        query.setParameter(0, patient);
        vaccinationMasterList = query.list();
      }
      for (VaccinationMaster vaccination : vaccinationMasterList) {
        Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE,
            vaccination.getVaccineCvxCode());
        if (cvxCode == null) {
          continue;
        }
        VaccinationReported vaccinationReported = vaccination.getVaccinationReported();
        boolean originalReporter =
            vaccinationReported.getPatientReported().getOrgReported().equals(orgAccess.getOrg());
        if ("D".equals(vaccinationReported.getActionCode())) {
          continue;
        }
        sb.append("ORC");
        // ORC-1
        sb.append("|RE");
        // ORC-2
        sb.append("|");
        if (originalReporter) {
          sb.append(vaccinationReported.getVaccinationReportedExternalLink() + "^"
              + orgAccess.getOrg().getOrganizationName());
        }
        // ORC-3
        sb.append("|");
        sb.append(vaccination.getVaccinationId() + "^IIS");
        sb.append("\r");
        sb.append("RXA");
        // RXA-1
        sb.append("|0");
        // RXA-2
        sb.append("|1");
        // RXA-3
        sb.append("|" + sdf.format(vaccination.getAdministeredDate()));
        // RXA-4
        sb.append("|");
        // RXA-5
        sb.append("|" + cvxCode.getValue() + "^" + cvxCode.getLabel() + "^CVX");
        if (!vaccinationReported.getVaccineNdcCode().equals("")) {
          Code ndcCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_NDC_CODE,
              vaccinationReported.getVaccineNdcCode());
          if (ndcCode != null) {
            sb.append("~" + ndcCode.getValue() + "^" + ndcCode.getLabel() + "^NDC");
          }
        }
        {
          // RXA-6
          sb.append("|");
          double adminAmount = 0.0;
          if (!vaccinationReported.getAdministeredAmount().equals("")) {
            try {
              adminAmount = Double.parseDouble(vaccinationReported.getAdministeredAmount());
            } catch (NumberFormatException nfe) {
              adminAmount = 0.0;
            }
          }
          if (adminAmount > 0) {
            sb.append(adminAmount);
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
          if (vaccinationReported.getInformationSource() != null) {
            informationCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_INFORMATION_SOURCE,
                vaccinationReported.getInformationSource());
          }
          if (informationCode != null) {
            sb.append(informationCode.getValue() + "^" + informationCode.getLabel() + "^NIP0001");
          }
        }
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
        if (vaccinationReported.getLotnumber() != null) {
          sb.append(vaccinationReported.getLotnumber());
        }
        // RXA-16
        sb.append("|");
        if (vaccinationReported.getExpirationDate() != null) {
          sb.append(sdf.format(vaccinationReported.getExpirationDate()));
        }
        // RXA-17
        sb.append("|");
        sb.append(printCode(vaccinationReported.getVaccineMvxCode(),
            CodesetType.VACCINATION_MANUFACTURER_CODE, "MVX", codeMap));
        // RXA-18
        sb.append("|");
        sb.append(printCode(vaccinationReported.getRefusalReasonCode(),
            CodesetType.VACCINATION_REFUSAL, "NIP002", codeMap));
        // RXA-19
        sb.append("|");
        // RXA-20
        sb.append("|");
        sb.append(printCode(vaccinationReported.getCompletionStatus(),
            CodesetType.VACCINATION_COMPLETION, null, codeMap));
        // RXA-21
        sb.append("|A");
        sb.append("\r");
        if (vaccinationReported.getBodyRoute() != null
            && !vaccinationReported.getBodyRoute().equals("")) {
          sb.append("RXR");
          // RXR-1
          sb.append("|");
          sb.append(printCode(vaccinationReported.getBodyRoute(), CodesetType.BODY_ROUTE, "NCIT",
              codeMap));
          // RXR-2
          sb.append("|");
          sb.append(printCode(vaccinationReported.getBodySite(), CodesetType.BODY_SITE, "HL70163",
              codeMap));
        }
      }
    }

    return sb.toString();
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

  public String buildAck(HL7Reader reader, ProcessingException e) {
    StringBuilder sb = new StringBuilder();
    {
      String messageType = "ACK^V04^ACK";
      String profileId = "Z23";
      createMSH(messageType, profileId, reader, sb);
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
    if (e == null) {
      sb.append("MSA|AA|" + sendersUniqueId + "\r");
    } else {
      sb.append("MSA|AE|" + sendersUniqueId + "\r");
      sb.append("ERR|");
      sb.append("|"); // 2
      if (e.getSegmentId() != null && !e.getSegmentId().equals("")) {
        sb.append(e.getSegmentId() + "^" + e.getSegmentRepeat());
        if (e.getFieldPosition() > 0) {
          sb.append("^" + e.getFieldPosition());
        }
      }
      sb.append("|101^Required field missing^HL70357"); // 3
      sb.append("|E"); // 4
      sb.append("|"); // 5
      sb.append("|"); // 6
      sb.append("|"); // 7
      sb.append("|" + e.getMessage()); // 8
      sb.append("|\r");
    }
    return sb.toString();
  }

  public void createMSH(String messageType, String profileId, HL7Reader reader, StringBuilder sb) {
    String sendingApp = "";
    String sendingFac = "";
    String receivingApp = "";
    String receivingFac = "";

    reader.resetPostion();
    if (reader.advanceToSegment("MSH")) {
      sendingApp = reader.getValue(3);
      sendingFac = reader.getValue(4);
      receivingApp = reader.getValue(5);
      receivingFac = reader.getValue(6);
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
    sb.append(receivingApp + "|");
    sb.append(receivingFac + "|");
    sb.append(sendingApp + "|");
    sb.append(sendingFac + "|");
    sb.append(sendingDateString + "|");
    sb.append("|");
    sb.append(messageType + "|");
    sb.append(uniqueId + "|");
    sb.append(production + "|");
    sb.append("2.5.1|");
    sb.append("|");
    sb.append("|");
    sb.append("NE|");
    sb.append("NE|");
    sb.append("|");
    sb.append("|");
    sb.append("|");
    sb.append("|");
    sb.append(profileId + "^CDCPHINVS\r");
  }

  private static Integer increment = new Integer(1);

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

  public Date parseDate(String dateString) {
    Date date = null;
    try {
      date = parseDateInternal(dateString);
    } catch (ParseException e) {
      // ignore
    }
    return date;
  }

  public Date parseDate(String dateString, String errorMessage, String segmentId, int segmentRepeat,
      int fieldPosition) throws ProcessingException {
    Date date = null;
    try {
      date = parseDateInternal(dateString);
    } catch (ParseException e) {
      if (errorMessage != null) {
        throw new ProcessingException(errorMessage + ": " + e.getMessage(), segmentId,
            segmentRepeat, fieldPosition);
      }
    }
    return date;
  }

  public Date parseDateInternal(String dateString) throws ParseException {
    Date date;
    if (dateString.length() > 8) {
      dateString = dateString.substring(0, 8);
    }
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
    date = simpleDateFormat.parse(dateString);
    return date;
  }

  private static final Random random = new Random();
  private static final char[] ID_CHARS =
      {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T',
          'U', 'V', 'W', 'X', 'Y', 'Z', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

  public String generatePatientExternalLink() {
    boolean keepLooking = true;
    int count = 0;
    while (keepLooking) {
      count++;
      if (count > 1000) {
        throw new RuntimeException("Unable to get a new id, tried 1000 times!");
      }
      String patientExternalLink = generateId();
      Query query = dataSession.createQuery("from PatientMaster where patientExternalLink = ?");
      query.setParameter(0, patientExternalLink);
      if (query.list().size() == 0) {
        return patientExternalLink;
        // we found a unique id!
      }
    }
    return null;
  }

  public String generateId() {
    String patientRegistryId = "";
    for (int i = 0; i < 12; i++) {
      patientRegistryId += ID_CHARS[random.nextInt(ID_CHARS.length)];
    }
    return patientRegistryId;
  }
}
