package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.Immunization;
import org.hl7.fhir.r5.model.Patient;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.mapping.forR5.LocationMapperR5;
import org.immregistries.iis.kernal.mapping.internalClient.FhirRequester;
import org.immregistries.iis.kernal.mapping.internalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public class CovidServlet extends HttpServlet {

	public static final String COVID_CVX_CODES = "208,207,210,212,211,213";
	public static final String ACTION_GENERATE = "Generate";
	public static final String PARAM_ACTION = "action";
	public static final String PARAM_DATE_START = "dateStart";
	public static final String PARAM_DATE_END = "dateEnd";
	public static final String PARAM_CVX_CODES = "cvxCodes";
	public static final String PARAM_INCLUDE_PHI = "includePhi";
	private static final String EXPORT_YYYY_MM_DD = "yyyy-MM-dd";
	private static Set<String> suffixAllowedValueSet = new HashSet<>();

	static {
		suffixAllowedValueSet.add("RN");
		suffixAllowedValueSet.add("MD");
		suffixAllowedValueSet.add("NP");
		suffixAllowedValueSet.add("PA");
		suffixAllowedValueSet.add("LPN");
	}

	@Autowired
	RepositoryClientFactory repositoryClientFactory;
	@Autowired
	FhirRequester fhirRequester;
	@Autowired
	LocationMapperR5 locationMapper;

	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		doGet(req, resp);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		Tenant tenant = ServletHelper.getTenant();
		if (tenant == null) {
			throw new AuthenticationCredentialsNotFoundException("");
		}
		IGenericClient fhirClient = repositoryClientFactory.newGenericClient(req);


		try {
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
			String action = req.getParameter(PARAM_ACTION);
			String messageError = null;
			String dateStartString = req.getParameter(PARAM_DATE_START);
			String dateEndString = req.getParameter(PARAM_DATE_END);
			HomeServlet.doHeader(out, "IIS Sandbox", tenant);


			Date dateStart = null;
			Date dateEnd = null;
			if (dateStartString == null) {
				Calendar calendar = Calendar.getInstance();
				calendar.set(Calendar.HOUR, 0);
				calendar.set(Calendar.MINUTE, 0);
				calendar.set(Calendar.SECOND, 0);
				calendar.add(Calendar.DAY_OF_MONTH, -1);
				dateStartString = sdf.format(calendar.getTime());
			} else {
				try {
					dateStart = sdf.parse(dateStartString);
				} catch (ParseException pe) {
					messageError = "Start date is unparsable";
				}
			}
			if (dateEndString == null) {
				dateEndString = sdf.format(new Date());
			} else {
				try {
					dateEnd = sdf.parse(dateEndString);
				} catch (ParseException pe) {
					messageError = "End date is unparsable";
				}
			}
			String cvxCodes = req.getParameter(PARAM_CVX_CODES);
			if (StringUtils.isEmpty(cvxCodes)) {
				cvxCodes = COVID_CVX_CODES;
			}
			boolean includePhi =
				req.getParameter(PARAM_CVX_CODES) == null || req.getParameter(PARAM_INCLUDE_PHI) != null;

			if (messageError != null) {
				out.println("  <div class=\"w3-panel w3-red\">");
				out.println("    <p>" + messageError + "</p>");
				out.println("  </div>");
			}
			out.println("    <div class=\"w3-container w3-card-4\">");
			out.println("    <h2>Export COVID-19 Flat-File for CDC Reporting</h2>");
			out.println("    <form method=\"POST\" action=\"covid\" class=\"w3-container w3-card-4\">");
			out.println("          <label>Start Date</label>");
			out.println("          <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_DATE_START
				+ "\" value=\"" + dateStartString + "\"/>");
			out.println("          <label>End Date</label>");
			out.println("          <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_DATE_END
				+ "\" value=\"" + dateEndString + "\"/>");
			out.println("          <label>End Date</label>");
			out.println("          <label>CVX Codes to Include</label>");
			out.println("          <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_CVX_CODES
				+ "\" value=\"" + cvxCodes + "\"/>");
			out.println("          <label>Include PHI</label>");
			out.println("          <input class=\"w3-input\" type=\"checkbox\" name=\""
				+ PARAM_INCLUDE_PHI + "\" value=\"Y\"" + (includePhi ? " checked" : "") + "/>");
			out.println(
				"          <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\""
					+ PARAM_ACTION + "\" value=\"" + ACTION_GENERATE + "\"/>");
			out.println("    </form>");
			if (action != null) {
				if (action.equals(ACTION_GENERATE) && dateStart != null && dateEnd != null) {

					Set<String> cvxCodeSet = new HashSet<>();
					{
						String[] codes = cvxCodes.split("\\,");
						for (String c : codes) {
							if (StringUtils.isNotEmpty(c)) {
								cvxCodeSet.add(c);
							}
						}
					}
					List<VaccinationReported> vaccinationReportedList;
					out.print(
						"<textarea cols=\"80\" rows=\"30\" style=\"white-space: nowrap;  overflow: auto;\">");
					{
						vaccinationReportedList = fhirRequester.searchVaccinationReportedList(
							new SearchParameterMap(Immunization.SP_PATIENT, new ReferenceParam().setChain(Patient.SP_ORGANIZATION).setValue(String.valueOf(tenant.getOrgId()))));// TODO TEST
//						Immunization.DATE.after().day(dateStart),
//					 Immunization.DATE.before().day(dateEnd),
//							Immunization.PATIENT.hasChainedProperty(Patient.ORGANIZATION.hasId(String.valueOf(tenant.getOrgId()))));
						Date finalDateStart = dateStart;
						Date finalDateEnd = dateEnd;
						vaccinationReportedList = vaccinationReportedList.stream().filter(vaccinationReported -> vaccinationReported.getReportedDate().after(finalDateStart) && vaccinationReported.getReportedDate().before(finalDateEnd)).collect(Collectors.toList());
//            TODO verify claim for reportedDate search criteria
//            Query query = dataSession.createQuery(
//                "from VaccinationReported where reportedDate >= :dateStart and reportedDate <= :dateEnd "
//                    + "and patientReported.orgReported = :orgReported");
					}

					printHeaderLine(out);
					for (VaccinationReported vaccinationReported : vaccinationReportedList) {
						if (cvxCodeSet.contains(vaccinationReported.getVaccineCvxCode())) {
							if (vaccinationReported.getCompletionStatus().equals("NA")) {
								// not reporting missed appointments anymore
								continue;
							}
							int doseNumber = getDoseNumber(fhirClient, vaccinationReported);
							printLine(out, vaccinationReported, includePhi, doseNumber);
						}
					}
					out.println("</textarea>");
				}
			}
			out.println("    </div>");

		} catch (Exception e) {
			System.err.println("Unable to render page: " + e.getMessage());
			e.printStackTrace(System.err);
		}
		HomeServlet.doFooter(out);
		out.flush();
		out.close();
	}

	private void printHeaderLine(PrintWriter out) {
		printField("vax_event_id", out);
		printField("ext_type", out);
		printField("pprl_id", out);
		printField("recip_id", out);
		printField("recip_first_name", out);
		printField("recip_middle_name", out);
		printField("recip_last_name", out);
		printField("recip_dob", out);
		printField("recip_sex", out);
		printField("recip_address_street", out);
		printField("recip_address_street_2", out);
		printField("recip_address_city", out);
		printField("recip_address_county", out);
		printField("recip_address_state", out);
		printField("recip_address_zip", out);
		printField("recip_race_1", out);
		printField("recip_race_2", out);
		printField("recip_race_3", out);
		printField("recip_race_4", out);
		printField("recip_race_5", out);
		printField("recip_race_6", out);
		printField("recip_ethnicity", out);
		printField("admin_date", out);
		printField("cvx", out);
		printField("ndc", out);
		printField("mvx", out);
		printField("lot_number", out);
		printField("vax_expiration", out);
		printField("vax_admin_site", out);
		printField("vax_route", out);
		printField("dose_num", out);
		printField("vax_series_complete", out);
		printField("responsible_org", out);
		printField("admin_name", out);
		printField("vtrcks_prov_pin", out);
		printField("admin_type", out);
		printField("admin_address_street", out);
		printField("admin_address_street_2", out);
		printField("admin_address_city", out);
		printField("admin_address_county", out);
		printField("admin_address_state", out);
		printField("admin_address_zip", out);
		printField("vax_refusal", out);
		printField("cmorbid_status", out);
		printField("serology", out);
		out.println();
	}

	private int getDoseNumber(IGenericClient fhirClient, VaccinationReported vaccinationReported) {
		int doseNumber = 0;
		if (vaccinationReported.getCompletionStatus().equals("CP")) {
//		 Query query = dataSession
//          .createQuery("from VaccinationReported where administeredDate < :administeredDate "
//              + "and patientReported = :patientReported and completionStatus = 'CP'");
//      query.setParameter("administeredDate", vaccinationReported.getAdministeredDate());
//      query.setParameter("patientReported", vaccinationReported.getPatientReported());
			List<VaccinationReported> list = fhirRequester.searchVaccinationReportedList(
				new SearchParameterMap(Immunization.SP_DATE, new DateParam().setPrefix(ParamPrefixEnum.ENDS_BEFORE).setValue(vaccinationReported.getAdministeredDate()))
					.add(Immunization.SP_PATIENT, new ReferenceParam(vaccinationReported.getPatientReportedId()))
					.add(Immunization.SP_STATUS, new TokenParam().setValue(Immunization.ImmunizationStatusCodes.COMPLETED.toCode()))
//				Immunization.DATE.before().day(vaccinationReported.getAdministeredDate()),
//				Immunization.PATIENT.hasId(vaccinationReported.getPatientReportedId()),
//				Immunization.STATUS.exactly().identifier(Immunization.ImmunizationStatusCodes.COMPLETED.toCode())
			);
			doseNumber = list.size() + 1;
		}
		return doseNumber;
	}

	private void printLine(PrintWriter out, VaccinationReported vaccinationReported,
								  boolean includePhi, int doseNumber) {

		PatientReported patientReported = vaccinationReported.getPatientReported();
		PatientMaster patient = patientReported.getPatient();


		// 1: Vaccination event ID
		printField(vaccinationReported.getVaccination().getVaccinationId(), out);

		// 2: Extract type
		if (includePhi) {
			printField("I", out);
		} else {
			printField("D", out);
		}
		// 3: PPRL generated ID
		printField("", out);
		// 4: Recipient ID
		printField(patient.getPatientId(), out);
		// 5:  Recipient name: first
		printField(patientReported.getNameFirst(), includePhi, out);
		// 6:  Recipient name: middle
		printField(patientReported.getNameMiddle(), includePhi, out);
		// 7:  Recipient name: last
		printField(patientReported.getNameLast(), includePhi, out);
		// 8:  Recipient date of birth
		printField(patientReported.getBirthDate(), out);
		// 9:  Recipient sex
		printField(patientReported.getSex(), out);
		// 10:  Recipient address: street
        printField(patientReported.getFirstAddress().getAddressLine1(), includePhi, out);
		// 11: Recipient address: street 2
        printField(patientReported.getFirstAddress().getAddressLine2(), includePhi, out);
		// 12:  Recipient address: city
        printField(patientReported.getFirstAddress().getAddressCity(), includePhi, out);
		// 13:  Recipient address:  county
        printField(patientReported.getFirstAddress().getAddressCountyParish(), out);
		// 14:  Recipient address: state
        printField(patientReported.getFirstAddress().getAddressState(), out);
		// 15:  Recipient address: zip code
        printField(patientReported.getFirstAddress().getAddressZip(), out);
		// 16:  Recipient race 1
		if (patient.getRaces().isEmpty() || StringUtils.isEmpty(patientReported.getRaces().get(0))) {
			printField("UNK", out);
		} else {
			for (String race : patientReported.getRaces()) {
				printField(race, out);
			}
		}

		boolean administeredVaccination = StringUtils.isEmpty(vaccinationReported.getCompletionStatus())
			|| vaccinationReported.getCompletionStatus().equals("CP");
		if (administeredVaccination) {
			// 24:  CVX
			printField(vaccinationReported.getVaccineCvxCode(), out);
			// 25: NDC
			printField(vaccinationReported.getVaccineNdcCode(), out);
			// 26:  MVX
			printField(vaccinationReported.getVaccineMvxCode(), out);
			// 27:  Lot number
			printField(vaccinationReported.getLotnumber(), out);
			// 28:  Vaccine expiration date
			printField(vaccinationReported.getExpirationDate(), out);
			// 29:  Vaccine administering site
			printField(vaccinationReported.getBodySite(), out);
			// 30:  Vaccine route of administration
			printField(vaccinationReported.getBodyRoute(), out);
			if (administeredVaccination) {
				if (doseNumber > 0) {
					// 31:  Dose number
					printField(doseNumber, out);
					// 32:  Vaccination series complete
					if (doseNumber >= 2) {
						printField("Yes", out);
					} else {
						printField("No", out);
					}
				} else {
					// 31:  Dose number
					printField("UNK", out);
					// 32:  Vaccination series complete
					printField("UNK", out);
				}
			} else {
				//    31   Dose number
				printField("", out);
				//    32   Vaccination series complete
				printField("", out);
			}
		} else {
			// 24:  CVX
			if (vaccinationReported.getCompletionStatus().equals("RE")) {
				printField(vaccinationReported.getVaccineCvxCode(), out);
			} else {
				printField("", out);
			}
			// 25: NDC
			printField("", out);
			// 26:  MVX
			printField("", out);
			// 27:  Lot number
			printField("", out);
			// 28:  Vaccine expiration date
			printField("", out);
			// 29:  Vaccine administering site
			printField("", out);
			// 30:  Vaccine route of administration
			printField("", out);
			// 31:  Dose number
			printField("", out);
			// 32:  Vaccination series complete
			printField("", out);
		}

		OrgLocation orgLocation = vaccinationReported.getOrgLocation();
		if (orgLocation == null) {
			orgLocation = fhirRequester.readAsOrgLocation(vaccinationReported.getOrgLocationId());
		}
		if (orgLocation == null) {
			// 33: Responsible organization
			printField("", out);
			// 34:  Administered at location
			printField("", out);
			// 35: VTrckS provider PIN
			printField("", out);
			// 36:  Administered at location: type
			printField("", out);
			// 37:  Administration address: street
			printField("", out);
			// 38:  Administration address: street 2
			printField("", out);
			// 39:  Administration address: city
			printField("", out);
			// 40:  Administration address: county
			printField("", out);
			// 41:  Administration address: state
			printField("", out);
			// 42:  Administration address: zip code
			printField("", out);
		} else {
			// 33: Responsible organization
			printField(orgLocation.getOrgFacilityCode(), out);
			// 34:  Administered at location
			printField(orgLocation.getOrgFacilityName(), out);
			// 35: VTrckS provider PIN
			printField(orgLocation.getVfcProviderPin(), out);
			// 36:  Administered at location: type
			printField(orgLocation.getLocationType(), out);
			// 37:  Administration address: street
			printField(orgLocation.getAddressLine1(), out);
			// 38:  Administration address: street 2
			printField(orgLocation.getAddressLine2(), out);
			// 39:  Administration address: city
			printField(orgLocation.getAddressCity(), out);
			// 40:  Administration address: county
			printField(orgLocation.getAddressCountyParish(), out);
			// 41:  Administration address: state
			printField(orgLocation.getAddressState(), out);
			// 42:  Administration address: zip code
			printField(orgLocation.getAddressZip(), out);
		}
		// 43:  Vaccination refusal
		if (administeredVaccination) {
			printField("No", out);
		} else {
			if (vaccinationReported.getCompletionStatus().equals("RE")) {
				printField("Yes", out);
			} else {
				printField("No", out);
			}
		}
		// 44:  Comorbidity status
		printField("UNK", out);

		// 45:  Serology results
		printField("UNK", out);
		out.println();
	}

	private String getSuffix(VaccinationReported vaccinationReported) {
		String suffix = vaccinationReported.getAdministeringProvider().getProfessionalSuffix();
		if (StringUtils.isEmpty(suffix)) {
			suffix = "UNK";
		} else {
			if (!suffixAllowedValueSet.contains(suffix)) {
				suffix = "OTH";
			}
		}
		return suffix;
	}

	private void printField(String s, PrintWriter out) {
		if (s != null) {
			out.print(s);
		}
		out.print("\t");
	}

	private void printField(String s, boolean includePhi, PrintWriter out) {
		if (includePhi) {
			if (s != null) {
				out.print(s);
			}
		} else {
			out.print("Redacted");
		}
		out.print("\t");
	}

	private void printField(Date d, PrintWriter out) {
		if (d != null) {
			SimpleDateFormat sdf = new SimpleDateFormat(EXPORT_YYYY_MM_DD);
			out.print(sdf.format(d));
		}
		out.print("\t");
	}

	private void printField(Integer i, PrintWriter out) {
		if (i != null) {
			out.print(i);
		}
		out.print("\t");
	}

}
