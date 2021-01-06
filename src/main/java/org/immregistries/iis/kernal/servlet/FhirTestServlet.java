package org.immregistries.iis.kernal.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Patient.ContactComponent;
import org.hl7.fhir.r4.model.SimpleQuantity;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.logic.CodeMapManager;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgMaster;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.VaccinationReported;
import org.immregistries.mqe.hl7util.parser.HL7Reader;
import org.immregistries.smm.transform.ScenarioManager;
import org.immregistries.smm.transform.TestCaseMessage;
import org.immregistries.smm.transform.Transformer;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@SuppressWarnings("serial")
public class FhirTestServlet extends HttpServlet {

  private static final String PARAM_BASE_URL = "baseUrl";
  private static final String PARAM_MESSAGE = "message";
  private static final String PARAM_FORMAT = "format";
  private static final String PARAM_NEW = "new";

  private static final String FORMAT_JSON = "json";
  private static final String FORMAT_XML = "xml";

  private static final String BASE_URL = "https://florence.immregistries.org/iis-sandbox/fhir";

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    HttpSession session = req.getSession(true);
    OrgAccess orgAccess = (OrgAccess) session.getAttribute("orgAccess");

    resp.setContentType("text/html");
    PrintWriter out = new PrintWriter(resp.getOutputStream());
    Session dataSession = PopServlet.getDataSession();
    HomeServlet.doHeader(out, session);
    try {



      try {
        CodeMap codeMap = CodeMapManager.getCodeMap();
        FhirContext ctx = FhirContext.forR4();
        String format = req.getParameter(PARAM_FORMAT);
        if (format == null) {
          format = FORMAT_XML;
        }

        IParser parser;
        if (format.equals(FORMAT_JSON)) {
          parser = ctx.newJsonParser();
        } else {
          parser = ctx.newXmlParser();
        }
        parser.setPrettyPrint(true);

        PatientReported pr = (PatientReported) session.getAttribute("patientReported");
        List<VaccinationReported> vaccinationReportedList =
            (List<VaccinationReported>) session.getAttribute("vaccinationReportedList");
        String message = req.getParameter(PARAM_MESSAGE);
        if (pr == null || req.getParameter(PARAM_NEW) != null) {
          pr = new PatientReported();
          PatientMaster pm = new PatientMaster();
          pr.setPatient(pm);
          vaccinationReportedList = new ArrayList<>();

          if (message == null) {
            TestCaseMessage testCaseMessage =
                ScenarioManager.createTestCaseMessage(ScenarioManager.SCENARIO_1_R_ADMIN_CHILD);
            Transformer transformer = new Transformer();
            transformer.transform(testCaseMessage);
            message = testCaseMessage.getMessageText();
          }
          HL7Reader reader = new HL7Reader(message);
          if (reader.advanceToSegment("PID")) {
            pr.setPatientReportedExternalLink(reader.getValue(3, 1));
            pm.setPatientExternalLink(reader.getValue(3, 1));
            pr.setPatientNameLast(reader.getValue(5, 1));
            pr.setPatientNameFirst(reader.getValue(5, 2));
            pr.setPatientNameMiddle(reader.getValue(5, 3));
            pr.setPatientBirthDate(getDateSafe(reader.getValue(7)));
            pr.setPatientSex(reader.getValue(8));
            pr.setPatientAddressLine1(reader.getValue(11, 1));
            pr.setPatientAddressLine2(reader.getValue(11, 2));
            pr.setPatientAddressCity(reader.getValue(11, 3));
            pr.setPatientAddressState(reader.getValue(11, 4));
            pr.setPatientAddressZip(reader.getValue(11, 5));
            pr.setPatientAddressCountry(reader.getValue(11, 6));
            pr.setPatientPhone(reader.getValue(13, 7) + reader.getValue(13, 8));
            if (reader.advanceToSegment("NK1", "ORC")) {
              pr.setGuardianLast(reader.getValue(2, 1));
              pr.setGuardianFirst(reader.getValue(2, 2));
              pr.setGuardianMiddle(reader.getValue(2, 3));
              pr.setGuardianRelationship(reader.getValue(3));
            }
          }
          while (reader.advanceToSegment("ORC")) {
            String vaccinationId = reader.getValue(3);
            if (reader.advanceToSegment("RXA")) {
              VaccinationReported vaccinationReported = new VaccinationReported();
              vaccinationReportedList.add(vaccinationReported);
              vaccinationReported.setVaccinationReportedExternalLink(vaccinationId);

            }
          }
        }

        Patient patient = new Patient();
        createPatientResource(pr, patient);



        String baseUrl = req.getParameter(BASE_URL);
        if (baseUrl == null) {
          String tenantId = "tenantId";
          if (orgAccess != null) {
            tenantId = orgAccess.getOrg().getOrganizationName();
          }
          baseUrl = BASE_URL + "/" + tenantId + "/";
        }

        out.println("<h3>Multitenancy</h3>");
        out.println(
            "<p>The IIS Sandbox supports multitenancy for FHIR. Which means that resources submitted to the sandbox will be placed in dedicated buckets of data that are separated from data by different tenants (facilities.) This is different than IIS which will merge data from all submitters. The IIS Sandbox keeps submitted data separate to support testing from multiple agencies. </p>");
        out.println("<a href=\"" + baseUrl + "\">" + baseUrl + "</a>");
        String patientUrl = baseUrl + "Patient/";
        String patientUrlWithId = patientUrl + pr.getPatientReportedExternalLink();
        out.println("<h3>Patient Resource</h3>");
        out.println("<table>");
        out.println("  <tr>");
        out.println("    <th>Action</th>");
        out.println("    <th>Method</th>");
        out.println("    <th>URL</th>");
        out.println("  </tr>");
        out.println("  <tr>");
        out.println("    <td>Create</td>");
        out.println("    <td>POST</td>");
        out.println("    <td><a href=\"" + patientUrl + "\">" + patientUrl + "</a></td>");
        out.println("  </tr>");
        out.println("  <tr>");
        out.println("    <td>Read</td>");
        out.println("    <td>GET</td>");
        out.println(
            "    <td><a href=\"" + patientUrlWithId + "\">" + patientUrlWithId + "</a></td>");
        out.println("  </tr>");
        out.println("  <tr>");
        out.println("    <td>Update</td>");
        out.println("    <td>PUT</td>");
        out.println(
            "    <td><a href=\"" + patientUrlWithId + "\">" + patientUrlWithId + "</a></td>");
        out.println("  </tr>");
        out.println("  <tr>");
        out.println("    <td>Delete</td>");
        out.println("    <td>DELETE</td>");
        out.println(
            "    <td><a href=\"" + patientUrlWithId + "\">" + patientUrlWithId + "</a></td>");
        out.println("  </tr>");
        out.println("</table>");
        out.println("<textarea cols=\"80\" rows=\"30\">" + parser.encodeResourceToString(patient)
            + "</textarea>");

        int count = 0;
        for (VaccinationReported vaccinationReported : vaccinationReportedList) {
          count++;
          Immunization immunization = new Immunization();
          Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE,
              vaccinationReported.getVaccineCvxCode());
          if (cvxCode == null) {
            continue;
          }

          if ("D".equals(vaccinationReported.getActionCode())) {
            continue;
          }
          createImmunizationResource(vaccinationReported, immunization, cvxCode, codeMap);
          out.println("<h3>Immunization #" + count + "</h3>");
          out.println("<pre>" + parser.encodeResourceToString(immunization) + "</pre>");
        }


        out.println("<h1>Generate Examples</h1>");
        out.println("<form action=\"fhirTest\" method=\"POST\" target=\"_blank\">");
        out.println("    <div class=\"w3-container w3-half w3-margin-top\">");
        out.println("      <label>Message To Send</label>");
        out.println("      <textarea class=\"w3-input\" name=\"" + PARAM_MESSAGE
            + "\" rows=\"15\" cols=\"160\">" + message + "</textarea></td>");
        out.println("    <div class=\"w3-container w3-card-4\">");
        out.println("      <label>Base URL</label>");
        out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_BASE_URL
            + "\" value=\"" + baseUrl + "\"/>");
        out.println("      <label>Message Format</label>");
        out.println(
            "      <input class=\"w3-input\" type=\"radio\" name=\"" + PARAM_FORMAT + "\" value=\""
                + FORMAT_JSON + "\"" + (format.equals(FORMAT_JSON) ? " selected" : "") + "/> JSON");
        out.println("      XML <input class=\"w3-input\" type=\"radio\" name=\"" + PARAM_FORMAT
            + "\" value=\"" + FORMAT_XML + "\"" + (format.equals(FORMAT_XML) ? " selected" : "")
            + "/> XML");
        out.println("      <label>Regenerate Example</label>");
        out.println("      <input class=\"w3-input\" type=\"checkbox\" name=\"" + PARAM_NEW
            + "\" value=\"True\"/>");
        out.println("      <br/>");
        out.println(
            "      <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"sumbit\" value=\"Refresh\"/>");
        out.println("    </div>");
        out.println("    </div>");
        out.println("</form>");
      } catch (Exception e) {
        out.println("<h3>Exception Thrown</h3>");
        out.println("<pre>");
        e.printStackTrace(out);
        out.println("</pre>");
      }



    } catch (Exception e) {
      System.err.println("Unable to render page: " + e.getMessage());
      e.printStackTrace(System.err);
    } finally {
      dataSession.close();
    }
    HomeServlet.doFooter(out, session);
    out.flush();
    out.close();
  }

  private Date getDateSafe(String value) throws ParseException {
    SimpleDateFormat sdf = new SimpleDateFormat("YYYYMMDD");
    Date date = null;
    try {
      date = sdf.parse(value);
    } catch (ParseException parseException) {
      // ignore
    }
    return date;
  }

  public CodeableConcept createCodeableConcept(String value, CodesetType codesetType,
      String tableName, CodeMap codeMap) {
    CodeableConcept codeableConcept = null;
    if (value != null) {
      Code code = codeMap.getCodeForCodeset(codesetType, value);
      if (code != null) {
        if (tableName != null) {
          codeableConcept = new CodeableConcept();
          Coding coding = codeableConcept.addCoding();
          coding.setCode(code.getValue());
          coding.setDisplay(code.getLabel());
          coding.setSystem(tableName);
        }
      }
    }
    return codeableConcept;
  }

  private void createImmunizationResource(VaccinationReported vaccinationReported,
      Immunization immunization, Code cvxCode, CodeMap codeMap) {


    {
      DateTimeType occurance = new DateTimeType(vaccinationReported.getAdministeredDate());
      immunization.setOccurrence(occurance);
    }
    {
      CodeableConcept vaccineCode = new CodeableConcept();
      Coding cvxCoding = vaccineCode.addCoding();
      cvxCoding.setCode(cvxCode.getValue());
      cvxCoding.setDisplay(cvxCode.getLabel());
      cvxCoding.setSystem("CVX");
      immunization.setVaccineCode(vaccineCode);
    }
    if (StringUtils.isNotEmpty(vaccinationReported.getVaccineNdcCode())) {
      CodeableConcept ndcCoding = createCodeableConcept(vaccinationReported.getVaccineNdcCode(),
          CodesetType.VACCINATION_NDC_CODE, "NDC", codeMap);
      immunization.setVaccineCode(ndcCoding);
    }
    {
      String administeredAmount = vaccinationReported.getAdministeredAmount();
      if (StringUtils.isNotEmpty(administeredAmount)) {
        SimpleQuantity doseQuantity = new SimpleQuantity();
        try {
          double d = Double.parseDouble(administeredAmount);
          doseQuantity.setValue(d);
          immunization.setDoseQuantity(doseQuantity);
        } catch (NumberFormatException nfe) {
          //ignore
        }
      }
    }

    {
      String infoSource = vaccinationReported.getInformationSource();
      if (StringUtils.isNotEmpty(infoSource)) {
        immunization.setPrimarySource(infoSource.equals("00"));
      }
    }

    {
      String lotNumber = vaccinationReported.getLotnumber();
      if (StringUtils.isNotEmpty(lotNumber)) {
        immunization.setLotNumber(lotNumber);
      }
    }

    {
      Date expirationDate = vaccinationReported.getExpirationDate();
      if (expirationDate != null) {
        immunization.setExpirationDate(expirationDate);
      }
    }


    {
      CodeableConcept mvxCoding = createCodeableConcept(vaccinationReported.getVaccineMvxCode(),
          CodesetType.VACCINATION_MANUFACTURER_CODE, "MVX", codeMap);
      // todo, need to make a reference
    }

    // TODO Refusal reasons
    // TODO Vaccination completion
    // TODO Route
    // TODO Site

    // TODO Observations

  }

  private void createPatientResource(PatientReported pr, Patient p) {
    PatientMaster pm = pr.getPatient();
    {
      Identifier id = p.addIdentifier();
      id.setValue(pm.getPatientExternalLink());
      CodeableConcept type = new CodeableConcept();
      type.addCoding().setCode("MR");
      id.setType(type);
    }
    p.setId(pm.getPatientExternalLink());
    {
      HumanName name = p.addName();
      name.setFamily(pr.getPatientNameLast());
      name.addGiven(pr.getPatientNameFirst());
      name.addGiven(pr.getPatientNameMiddle());
    }
    // TODO Mother's maiden name
    p.setBirthDate(pr.getPatientBirthDate());
    {
      AdministrativeGender administrativeGender = null;
      if (pr.getPatientSex().equals("F")) {
        administrativeGender = Enumerations.AdministrativeGender.FEMALE;
      } else if (pr.getPatientSex().equals("M")) {
        administrativeGender = Enumerations.AdministrativeGender.MALE;
      } else if (pr.getPatientSex().equals("O")) {
        administrativeGender = Enumerations.AdministrativeGender.OTHER;
      } else if (pr.getPatientSex().equals("U")) {
        administrativeGender = Enumerations.AdministrativeGender.UNKNOWN;
      } else if (pr.getPatientSex().equals("X")) {
        administrativeGender = Enumerations.AdministrativeGender.OTHER;
      }
      if (administrativeGender != null) {
        p.setGender(administrativeGender);
      }
    }
    // TODO Race - not supported by base specification, probably have to use extensions
    if (StringUtils.isNotEmpty(pr.getPatientAddressLine1())
        || StringUtils.isNotEmpty(pr.getPatientAddressZip())) {
      Address address = p.addAddress();
      if (StringUtils.isNotEmpty(pr.getPatientAddressLine1())) {
        address.addLine(pr.getPatientAddressLine1());
      }
      if (StringUtils.isNotEmpty(pr.getPatientAddressLine2())) {
        address.addLine(pr.getPatientAddressLine2());
      }
      address.setCity(pr.getPatientAddressCity());
      address.setState(pr.getPatientAddressState());
      address.setPostalCode(pr.getPatientAddressZip());
      address.setCountry(pr.getPatientAddressCountry());
      address.setDistrict(pr.getPatientAddressCountyParish());
    }
    {
      ContactPoint contactPoint = p.addTelecom();
      contactPoint.setSystem(ContactPointSystem.PHONE);
      contactPoint.setValue(pr.getPatientPhone());
    }
    // TODO Ethnicity not supported by base standard

    if (pr.getPatientBirthFlag().equals("Y")) {
      BooleanType booleanType = new BooleanType(true);
      p.setMultipleBirth(booleanType);
      if (StringUtils.isNotEmpty(pr.getPatientBirthOrder())) {
        try {
          int birthOrder = Integer.parseInt(pr.getPatientBirthOrder());
          IntegerType integerType = new IntegerType();
          integerType.setValue(birthOrder);
          p.setMultipleBirth(integerType);
        } catch (NumberFormatException nfe) {
          // ignore
        }
      }
    } else if (pr.getPatientBirthFlag().equals("N")) {
      BooleanType booleanType = new BooleanType(false);
      p.setMultipleBirth(booleanType);
    }

    if (!pr.getGuardianRelationship().equals("")
        && (!pr.getGuardianLast().equals("") || !pr.getGuardianFirst().equals(""))) {
      ContactComponent contactComponent = p.addContact();
      contactComponent.addRelationship().addCoding().setCode(pr.getGuardianRelationship());
      HumanName humanName = new HumanName();
      humanName.setFamily(pr.getGuardianLast());
      humanName.addGiven(pr.getGuardianFirst());
      contactComponent.setName(humanName);
    }

  }

  @SuppressWarnings("unchecked")
  public OrgAccess authenticateOrgAccess(String userId, String password, String facilityId,
      Session dataSession) {
    OrgMaster orgMaster = null;
    OrgAccess orgAccess = null;
    {
      Query query = dataSession.createQuery("from OrgMaster where organizationName = ?");
      query.setParameter(0, facilityId);
      List<OrgMaster> orgMasterList = query.list();
      if (orgMasterList.size() > 0) {
        orgMaster = orgMasterList.get(0);
      } else {
        orgMaster = new OrgMaster();
        orgMaster.setOrganizationName(facilityId);
        orgAccess = new OrgAccess();
        orgAccess.setOrg(orgMaster);
        orgAccess.setAccessName(userId);
        orgAccess.setAccessKey(password);
        Transaction transaction = dataSession.beginTransaction();
        dataSession.save(orgMaster);
        dataSession.save(orgAccess);
        transaction.commit();
      }

    }
    if (orgAccess == null) {
      orgAccess = authenticateOrgAccessForFacility(userId, password, dataSession, orgMaster);
    }
    return orgAccess;
  }

  public OrgAccess authenticateOrgAccessForFacility(String userId, String password,
      Session dataSession, OrgMaster orgMaster) {
    OrgAccess orgAccess = null;
    Query query = dataSession
        .createQuery("from OrgAccess where accessName = ? and accessKey = ? and org = ?");
    query.setParameter(0, userId);
    query.setParameter(1, password);
    query.setParameter(2, orgMaster);
    List<OrgAccess> orgAccessList = query.list();
    if (orgAccessList.size() != 0) {
      orgAccess = orgAccessList.get(0);
    }
    return orgAccess;
  }
}
