package org.immregistries.iis.kernal.servlet.deprecated;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hl7.fhir.r5.model.*;
import org.hl7.fhir.r5.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r5.model.Enumerations.AdministrativeGender;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.logic.CodeMapManager;
import org.immregistries.iis.kernal.logic.IncomingMessageHandler;
import org.immregistries.iis.kernal.mapping.interfaces.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.internalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.Tenant;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.servlet.HomeServlet;
import org.immregistries.iis.kernal.servlet.PopServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

/**
 * Deprecated
 */
@SuppressWarnings("serial")
public class V2ToFhirServlet extends HttpServlet {
	public static final String PARAM_PATIENT_REPORTED_ID = "patientReportedId";
	@Autowired
	RepositoryClientFactory repositoryClientFactory;
	@Autowired
	ImmunizationMapper immunizationMapper;
	@Autowired
	IncomingMessageHandler incomingMessageHandler;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		doGet(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		Tenant tenant = ServletHelper.getTenant();
		if (tenant == null) {
			throw new AuthenticationCredentialsNotFoundException("");
		}

		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		Session dataSession = PopServlet.getDataSession();
		HomeServlet.doHeader(out, "IIS Sandbox");
		try {
			PatientReported pr = (PatientReported) dataSession.get(PatientReported.class,
				Integer.parseInt(req.getParameter(PARAM_PATIENT_REPORTED_ID)));


			try {
				CodeMap codeMap = CodeMapManager.getCodeMap();
				FhirContext ctx = repositoryClientFactory.getFhirContext();
				IParser parser = ctx.newJsonParser();
				parser.setPrettyPrint(true);

				Bundle bundle = new Bundle();

				Patient p = new Patient();
				createPatientResource(pr, p);
				bundle.addEntry().setResource(p);
				List<VaccinationMaster> vaccinationMasterList =
					incomingMessageHandler.getVaccinationMasterList(pr.getPatient());

				for (VaccinationMaster vaccination : vaccinationMasterList) {
					Immunization immunization = new Immunization();
					Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE,
						vaccination.getVaccineCvxCode());
					if (cvxCode == null) {
						continue;
					}
					if ("D".equals(vaccination.getActionCode())) {
						continue;
					}
					createImmunizationResource(vaccination, immunization, cvxCode, codeMap);
					bundle.addEntry().setResource(immunization);
				}


				String serialized = parser.encodeResourceToString(bundle);
				out.println("<h3>JSON</h3>");
				out.println("<textarea cols=\"100\" rows=\"30\">" + serialized + "</textarea>");

			} catch (Exception e) {
				out.println("<h3>Exception Thrown</h3>");
				out.println("<pre>");
				e.printStackTrace();
				out.println("</pre>");
			}


		} catch (Exception e) {
			System.err.println("Unable to render page: " + e.getMessage());
			e.printStackTrace(System.err);
		} finally {
			dataSession.close();
		}
		HomeServlet.doFooter(out);
		out.flush();
		out.close();
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

	private void createImmunizationResource(VaccinationMaster vaccination, Immunization immunization,
														 Code cvxCode, CodeMap codeMap) {
		immunization = (Immunization) immunizationMapper.fhirResource(vaccination); // TODO Maybe remove this or remove the rest

		{
			DateTimeType occurance = new DateTimeType(vaccination.getAdministeredDate());
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
		if (StringUtils.isNotEmpty(vaccination.getVaccineNdcCode())) {
			CodeableConcept ndcCoding = createCodeableConcept(vaccination.getVaccineNdcCode(),
				CodesetType.VACCINATION_NDC_CODE, "NDC", codeMap); //TODO use CodeSet type in mapping ?
			immunization.setVaccineCode(ndcCoding);
		}
		{
			String administeredAmount = vaccination.getAdministeredAmount();
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
			String infoSource = vaccination.getInformationSource();
			if (StringUtils.isNotEmpty(infoSource)) {
				immunization.setPrimarySource(infoSource.equals("00"));
			}
		}

		{
			String lotNumber = vaccination.getLotnumber();
			if (StringUtils.isNotEmpty(lotNumber)) {
				immunization.setLotNumber(lotNumber);
			}
		}

		{
			Date expirationDate = vaccination.getExpirationDate();
			if (expirationDate != null) {
				immunization.setExpirationDate(expirationDate);
			}
		}


		{
			CodeableConcept mvxCoding = createCodeableConcept(vaccination.getVaccineMvxCode(),
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
			id.setValue(pm.getMainPatientIdentifier().getValue());
			CodeableConcept type = new CodeableConcept();
			type.addCoding().setCode("MR");
			id.setType(type);
		}
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
		// TODO Race - not supported by base specification, probably have to use extensions
		if (StringUtils.isNotEmpty(pr.getFirstAddress().getAddressLine1())
				|| StringUtils.isNotEmpty(pr.getFirstAddress().getAddressZip())) {
			Address address = p.addAddress();
			if (StringUtils.isNotEmpty(pr.getFirstAddress().getAddressLine1())) {
				address.addLine(pr.getFirstAddress().getAddressLine1());
			}
			if (StringUtils.isNotEmpty(pr.getFirstAddress().getAddressLine2())) {
				address.addLine(pr.getFirstAddress().getAddressLine2());
			}
			address.setCity(pr.getFirstAddress().getAddressCity());
			address.setState(pr.getFirstAddress().getAddressState());
			address.setPostalCode(pr.getFirstAddress().getAddressZip());
			address.setCountry(pr.getFirstAddress().getAddressCountry());
			address.setDistrict(pr.getFirstAddress().getAddressCountyParish());
		}
		{
			ContactPoint contactPoint = p.addTelecom();
			contactPoint.setSystem(ContactPointSystem.PHONE);
			contactPoint.setValue(pr.getFirstPhone().getNumber());
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

//		if (!pr.getGuardianRelationship().equals("")
//			&& (!pr.getGuardianLast().equals("") || !pr.getGuardianFirst().equals(""))) {
//			ContactComponent contactComponent = p.addContact();
//			contactComponent.addRelationship().addCoding().setCode(pr.getGuardianRelationship());
//			HumanName humanName = new HumanName();
//			humanName.setFamily(pr.getGuardianLast());
//			humanName.addGiven(pr.getGuardianFirst());
//			contactComponent.setName(humanName);
//		}

	}

}
