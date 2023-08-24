package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.parser.IParser;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.*;
import org.hl7.fhir.r5.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r5.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r5.model.Patient.ContactComponent;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.logic.CodeMapManager;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.mqe.hl7util.parser.HL7Reader;
import org.immregistries.smm.transform.ScenarioManager;
import org.immregistries.smm.transform.TestCaseMessage;
import org.immregistries.smm.transform.Transformer;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Deprecated, and only working with R5
 */
@SuppressWarnings("serial")
public class FhirTestServlet extends HttpServlet {

  private static final String PARAM_BASE_URL = "baseUrl";
  private static final String PARAM_MESSAGE = "message";
  private static final String PARAM_FORMAT = "format";
  private static final String PARAM_NEW = "new";

  private static final String FORMAT_JSON = "JSON";
  private static final String FORMAT_XML = "XML";

  private static final String BASE_URL = "https://sabbia.immregistries.org/iis";
	@Autowired
	IFhirSystemDao fhirSystemDao;

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    HttpSession session = req.getSession(true);
//    OrgAccess orgAccess = ServletHelper.getOrgAccess();
	  OrgMaster orgMaster = ServletHelper.getOrgMaster();


    resp.setContentType("text/html");
    PrintWriter out = new PrintWriter(resp.getOutputStream());
//    Session dataSession = PopServlet.getDataSession();
    HomeServlet.doHeader(out, "IIS Sandbox");
    try {

      try {
        CodeMap codeMap = CodeMapManager.getCodeMap();
        FhirContext ctx = fhirSystemDao.getContext();
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

        @SuppressWarnings("unchecked")
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
          if (reader.advanceToSegment("PID", "ORC")) {
            pr.setPatientReportedExternalLink(reader.getValue(3, 1));
            pm.setPatientExternalLink(reader.getValue(3, 1));
            pr.setNameLast(reader.getValue(5, 1));
            pr.setNameFirst(reader.getValue(5, 2));
            pr.setNameMiddle(reader.getValue(5, 3));
            pr.setBirthDate(getDateSafe(reader.getValue(7)));
            pr.setSex(reader.getValue(8));
            pr.setAddressLine1(reader.getValue(11, 1));
            pr.setAddressLine2(reader.getValue(11, 2));
            pr.setAddressCity(reader.getValue(11, 3));
            pr.setAddressState(reader.getValue(11, 4));
            pr.setAddressZip(reader.getValue(11, 5));
            pr.setAddressCountry(reader.getValue(11, 6));
            pr.setPhone(reader.getValue(13, 7) + reader.getValue(13, 8));
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
              VaccinationReported vr = new VaccinationReported();
              vaccinationReportedList.add(vr);
              vr.setExternalLink(vaccinationId);
              vr.setAdministeredDate(getDateSafe(reader.getValue(3)));
              {
                String vaccineCode = reader.getValue(5, 1);
                String vaccineCvxCode = "";
                String vaccineNdcCode = "";
                String vaccineCodeType = reader.getValue(5, 3);
                if (vaccineCodeType.equals("NDC")) {
                  vaccineNdcCode = vaccineCode;
                } else if (vaccineCodeType.equals("CPT") || vaccineCodeType.equals("C4")
                    || vaccineCodeType.equals("C5")) {
                  // ignore
                } else {
                  vaccineCvxCode = vaccineCode;
                }
                {
                  String altVaccineCode = reader.getValue(5, 4);
                  String altVaccineCodeType = reader.getValue(5, 6);
                  if (!altVaccineCode.equals("")) {
                    if (altVaccineCodeType.equals("NDC")) {
                      if (vaccineNdcCode.equals("")) {
                        vaccineNdcCode = altVaccineCode;
                      }
                    } else if (altVaccineCodeType.equals("CPT") || altVaccineCodeType.equals("C4")
                        || altVaccineCodeType.equals("C5")) {
                      // ignore
                    } else {
                      if (vaccineCvxCode.equals("")) {
                        vaccineCvxCode = altVaccineCode;
                      }
                    }
                  }
                }
                vr.setVaccineCvxCode(vaccineCvxCode);
                vr.setVaccineNdcCode(vaccineNdcCode);
              }
              {
                String admininisteredAmount = reader.getValue(6);
                if (!admininisteredAmount.equals("999")) {
                  vr.setAdministeredAmount(admininisteredAmount);
                }
              }
              vr.setInformationSource(reader.getValue(9));
              vr.setLotnumber(reader.getValue(15));
              vr.setVaccineMvxCode(reader.getValue(17));
              vr.setCompletionStatus(reader.getValue(20));
            }
          }
        }

        Patient patient = new Patient();
        createPatientResource(pr, patient);

        String baseUrl = req.getParameter(BASE_URL);
        if (baseUrl == null) {
          String tenantId = "tenantId";
          if (orgMaster != null) {
            tenantId = orgMaster.getOrganizationName();
          }
          tenantId = URLEncoder.encode(tenantId, StandardCharsets.UTF_8);
          baseUrl = BASE_URL + "/" + tenantId + "/";
        }

        out.println("<h2>FHIR Test Endpoint</h2>");
        out.println("<p>" + baseUrl + "</p>");
        if (orgMaster == null) {
          out.println(
              "    <div class=\"w3-panel w3-yellow\"><p class=\"w3-left-align\">You will need to place the IIS Sandbox Facility Id where the tenantId is indicated. </p></div>");
        }
        out.println(
            "<p>The IIS Sandbox supports multitenancy for FHIR. Which means that resources submitted to the sandbox will be placed in dedicated buckets of data that are separated from data by different tenants (facilities.) This is different than IIS which will merge data from all submitters. The IIS Sandbox keeps submitted data separate to support testing from multiple agencies. </p>");
        {
          String patientUrl = baseUrl + "Patient/";
          String patientUrlWithId = patientUrl + pr.getPatientReportedExternalLink();
          out.println("<h3>Patient Resource</h3>");
          out.println(
              "<table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
          out.println("  <tr class=\"w3-green\">");
          out.println("    <th>Action</th>");
          out.println("    <th>Method</th>");
          out.println("    <th>URL</th>");
          out.println("  </tr>");
          out.println("  <tbody>");
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
          out.println("  </tbody>");
          out.println("</table>");
          out.println("<h4>" + format + "</h4>");
          out.println("<textarea cols=\"80\" rows=\"15\">" + parser.encodeResourceToString(patient)
              + "</textarea>");
        }

        int count = 0;
        for (VaccinationReported vr : vaccinationReportedList) {
          Immunization immunization = new Immunization();
          Code cvxCode =
              codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, vr.getVaccineCvxCode());
          if (cvxCode == null) {
            continue;
          }

          if ("D".equals(vr.getActionCode())) {
            continue;
          }
          count++;
          createImmunizationResource(vr, immunization, cvxCode, codeMap);
          String immUrl = baseUrl + "Immunization/";
          String immUrlWithId = immUrl + vr.getExternalLink();
          out.println("<h3>Immunization #" + count + "</h3>");
          out.println(
              "<table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
          out.println("  <tr class=\"w3-green\">");
          out.println("    <th>Action</th>");
          out.println("    <th>Method</th>");
          out.println("    <th>URL</th>");
          out.println("  </tr>");
          out.println("  <tbody>");
          out.println("  <tr>");
          out.println("    <td>Create</td>");
          out.println("    <td>POST</td>");
          out.println("    <td><a href=\"" + immUrl + "\">" + immUrl + "</a></td>");
          out.println("  </tr>");
          out.println("  <tr>");
          out.println("    <td>Read</td>");
          out.println("    <td>GET</td>");
          out.println("    <td><a href=\"" + immUrlWithId + "\">" + immUrlWithId + "</a></td>");
          out.println("  </tr>");
          out.println("  <tr>");
          out.println("    <td>Update</td>");
          out.println("    <td>PUT</td>");
          out.println("    <td><a href=\"" + immUrlWithId + "\">" + immUrlWithId + "</a></td>");
          out.println("  </tr>");
          out.println("  <tr>");
          out.println("    <td>Delete</td>");
          out.println("    <td>DELETE</td>");
          out.println("    <td><a href=\"" + immUrlWithId + "\">" + immUrlWithId + "</a></td>");
          out.println("  </tr>");
          out.println("  </tbody>");
          out.println("</table>");
          out.println("<h4>" + format + "</h4>");
          out.println("<textarea cols=\"80\" rows=\"15\">"
              + parser.encodeResourceToString(immunization) + "</textarea>");
        }

        out.println("<h2>Setup</h2>");
        out.println("<form action=\"fhirTest\" method=\"POST\">");
        out.println("    <div class=\"w3-container w3-half w3-margin-top\">");
        out.println("    <div class=\"w3-container w3-card-4\">");
        out.println("      <label>VXU Message</label>");
        out.println("      <textarea class=\"w3-input\" name=\"" + PARAM_MESSAGE
            + "\" rows=\"15\" cols=\"160\">" + message + "</textarea></td>");
        out.println("      <label>Regenerate</label>");
        out.println("      <input class=\"w3-input\" type=\"checkbox\" name=\"" + PARAM_NEW
            + "\" value=\"True\"/>");
        out.println("      <label>Base URL</label>");
        out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_BASE_URL
            + "\" value=\"" + baseUrl + "\"/>");
        out.println("      <label>JSON</label>");
        out.println(
            "      <input class=\"w3-input\" type=\"radio\" name=\"" + PARAM_FORMAT + "\" value=\""
                + FORMAT_JSON + "\"" + (format.equals(FORMAT_JSON) ? " checked" : "") + "/>");
        out.println("      <label>XML</label>");
        out.println(
            "      <input class=\"w3-input\" type=\"radio\" name=\"" + PARAM_FORMAT + "\" value=\""
                + FORMAT_XML + "\"" + (format.equals(FORMAT_XML) ? " checked" : "") + "/>");
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
    }
    HomeServlet.doFooter(out);
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
          // ignore
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
      name.setFamily(pr.getNameLast());
      name.addGiven(pr.getNameFirst());
      name.addGiven(pr.getNameMiddle());
    }
    // TODO Mother's maiden name
    p.setBirthDate(pr.getBirthDate());
    {
      AdministrativeGender administrativeGender = null;
      if (pr.getSex().equals("F")) {
        administrativeGender = AdministrativeGender.FEMALE;
      } else if (pr.getSex().equals("M")) {
        administrativeGender = AdministrativeGender.MALE;
      } else if (pr.getSex().equals("O")) {
        administrativeGender = AdministrativeGender.OTHER;
      } else if (pr.getSex().equals("U")) {
        administrativeGender = AdministrativeGender.UNKNOWN;
      } else if (pr.getSex().equals("X")) {
        administrativeGender = AdministrativeGender.OTHER;
      }
      if (administrativeGender != null) {
        p.setGender(administrativeGender);
      }
    }
    // TODO Race - not supported by base specification, probably have to use
    // extensions
    if (StringUtils.isNotEmpty(pr.getAddressLine1())
       || StringUtils.isNotEmpty(pr.getAddressZip())) {
      Address address = p.addAddress();
      if (StringUtils.isNotEmpty(pr.getAddressLine1())) {
        address.addLine(pr.getAddressLine1());
      }
      if (StringUtils.isNotEmpty(pr.getAddressLine2())) {
        address.addLine(pr.getAddressLine2());
      }
      address.setCity(pr.getAddressCity());
      address.setState(pr.getAddressState());
      address.setPostalCode(pr.getAddressZip());
      address.setCountry(pr.getAddressCountry());
      address.setDistrict(pr.getAddressCountyParish());
    }
    {
      ContactPoint contactPoint = p.addTelecom();
      contactPoint.setSystem(ContactPointSystem.PHONE);
      contactPoint.setValue(pr.getPhone());
    }
    // TODO Ethnicity not supported by base standard

    if (pr.getBirthFlag().equals("Y")) {
      BooleanType booleanType = new BooleanType(true);
      p.setMultipleBirth(booleanType);
      if (StringUtils.isNotEmpty(pr.getBirthOrder())) {
        try {
          int birthOrder = Integer.parseInt(pr.getBirthOrder());
          IntegerType integerType = new IntegerType();
          integerType.setValue(birthOrder);
          p.setMultipleBirth(integerType);
        } catch (NumberFormatException nfe) {
          // ignore
        }
      }
    } else if (pr.getBirthFlag().equals("N")) {
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

}
