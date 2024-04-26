package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.*;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.InternalClient.FhirRequester;
import org.immregistries.iis.kernal.InternalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.fhir.annotations.OnR5Condition;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.logic.CodeMapManager;
import org.immregistries.iis.kernal.mapping.Interfaces.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.Interfaces.PatientMapper;
import org.immregistries.iis.kernal.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping({"/vaccination","/patient/{patientId}/vaccination", "/tenant/{tenantId}/patient/{patientId}/vaccination"})
@Conditional(OnR5Condition.class)
public class VaccinationServlet extends PatientServlet {
	public static final String PARAM_ACTION = "action";
	public static final String PARAM_RESOURCE = "resource";
	public static final String PARAM_VACCINATION_REPORTED_ID = "vaccinationReportedId";
	@Autowired
	RepositoryClientFactory repositoryClientFactory;
	@Autowired
	FhirRequester fhirRequester;
	@Autowired
	PatientMapper patientMapper;
	@Autowired
	ImmunizationMapper<Immunization> immunizationMapper;

	public static String linkUrl(String facilityId, String patientId) {
		return "/tenant/" + facilityId + "/patient/" + patientId + "/vaccination";
	}

	@PostMapping
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		doGet(req, resp);
	}

	@GetMapping
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {

		Tenant tenant = ServletHelper.getTenant();


		if (tenant == null) {
			throw new AuthenticationCredentialsNotFoundException("");
		}
		IGenericClient fhirClient = repositoryClientFactory.newGenericClient(req);


		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		try {
			Immunization immunization = getImmunizationFromParameter(req,fhirClient);
			VaccinationMaster vaccination = immunizationMapper.getMaster(immunization);
//			 fhirRequests.searchVaccinationReported(fhirClient,
//			 Immunization.IDENTIFIER.exactly().code(req.getParameter(PARAM_VACCINATION_REPORTED_ID)));

			String action = req.getParameter(PARAM_ACTION);
			if (action != null) {
			}

			HomeServlet.doHeader(out, "IIS Sandbox - Vaccinations");

			out.println("<h2>Tenant : " + tenant.getOrganizationName() + "</h2>");
			PatientReported patientReportedSelected = fhirRequester.readPatientReported(vaccination.getPatientReportedId());
			{
				printPatient(out, patientReportedSelected);
				SimpleDateFormat sdfDate = new SimpleDateFormat("MM/dd/yyyy");
				out.println("  <div class=\"w3-container\">");
				out.println("<h4>Vaccination</h4>");
				{
					CodeMap codeMap = CodeMapManager.getCodeMap();
					out.println("<table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
					out.println("  <tr class=\"w3-green\">");
					out.println("    <th>Vaccine</th>");
					out.println("    <th>Admin Date</th>");
					out.println("    <th>Manufacturer</th>");
					out.println("    <th>Lot Number</th>");
					out.println("    <th>Information</th>");
					out.println("    <th>Action</th>");
					out.println("  </tr>");
					out.println("  <tbody>");
					{
						out.println("  <tr>");
						out.println("    <td>");
						if (!StringUtils.isEmpty(vaccination.getVaccineCvxCode())) {
							Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE,
								vaccination.getVaccineCvxCode());
							if (cvxCode == null) {
								out.println("Unknown CVX (" + vaccination.getVaccineCvxCode() + ")");
							} else {
								out.println(
									cvxCode.getLabel() + " (" + vaccination.getVaccineCvxCode() + ")");
							}
						}
						out.println("    </td>");
						out.println("    <td>");
						if (vaccination.getAdministeredDate() == null) {
							out.println("null");
						} else {
							out.println(sdfDate.format(vaccination.getAdministeredDate()));
						}
						out.println("    </td>");
						out.println("    <td>");
						if (!StringUtils.isEmpty(vaccination.getVaccineMvxCode())) {
							Code mvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_MANUFACTURER_CODE,
								vaccination.getVaccineMvxCode());
							if (mvxCode == null) {
								out.print("Unknown MVX");
							} else {
								out.print(mvxCode.getLabel());
							}
							out.println(" (" + vaccination.getVaccineMvxCode() + ")");
						}
						out.println("    </td>");
						out.println("    <td>" + vaccination.getLotnumber() + "</td>");
						out.println("    <td>");
						if (!StringUtils.isEmpty(vaccination.getInformationSource())) {
							Code informationCode =
								codeMap.getCodeForCodeset(CodesetType.VACCINATION_INFORMATION_SOURCE,
									vaccination.getInformationSource());
							if (informationCode != null) {
								out.print(informationCode.getLabel());
								out.println(" (" + vaccination.getInformationSource() + ")");
							}
						}
						out.println("    </td>");
						out.println("    <td>");
						if (!StringUtils.isEmpty(vaccination.getActionCode())) {
							Code actionCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_ACTION_CODE,
								vaccination.getActionCode());
							if (actionCode != null) {
								out.print(actionCode.getLabel());
								out.println(" (" + vaccination.getActionCode() + ")");
							}
						}
						out.println("    </td>");
						out.println("  </tr>");
					}
					out.println("  </tbody>");
					out.println("</table>");
				}

				List<ObservationReported> observationReportedList =
					getObservationList(vaccination);

				if (observationReportedList.size() != 0) {
					out.println("<h4>Observations</h4>");
					printObservations(out, observationReportedList);
				}
				out.println("  </div>");

				Bundle bundle = fhirClient.search().forResource(Subscription.class).returnBundle(Bundle.class).execute();

				/**
				 * Setting external identifier in reference
				 */
				immunization.getPatient().setIdentifier(new Identifier()
					.setValue(vaccination.getPatientReported().getExternalLink())
					.setSystem(vaccination.getPatientReported().getPatientReportedAuthority()));
				IParser parser = repositoryClientFactory.getFhirContext().newJsonParser().setPrettyPrint(true);

				printSubscriptions(out, parser, bundle, immunization);


				{
					out.println("<div class=\"w3-container\">");
					out.println("<h4>FHIR Api Shortcuts</h4>");
					String apiBaseUrl = "/iis/fhir/" + tenant.getOrganizationName();
					{
						String link = apiBaseUrl + "/Immunization?_id=" + vaccination.getVaccinationId();
						out.println("<div>FHIR Immunization: <a href=\"" + link + "\">" + link + "</a></div>");
					}
					{
						String link = apiBaseUrl + "/Immunization?patient=" + vaccination.getPatientReportedId();
						out.println("<div>Other immunizations of same patient: <a href=\"" + link + "\">" + link + "</a></div>");
					}
					out.println("</div>");
				}

			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		HomeServlet.doFooter(out);
		out.flush();
		out.close();
	}

	@SuppressWarnings("unchecked")
	public List<ObservationReported> getObservationList(VaccinationMaster vaccination) {
		List<ObservationReported> observationReportedList;
		{
			observationReportedList = fhirRequester.searchObservationReportedList(
				new SearchParameterMap(Observation.SP_PATIENT, new ReferenceParam(vaccination.getPatientReportedId())));
//				Observation.PATIENT.hasId(vaccination.getPatientReportedId()));
			Set<String> suppressSet = LoincIdentifier.getSuppressIdentifierCodeSet();
			for (Iterator<ObservationReported> it = observationReportedList.iterator(); it.hasNext(); ) {
				ObservationReported observationReported = it.next();
				if (suppressSet.contains(observationReported.getIdentifierCode())) {
					it.remove();
				}
			}
		}
		return observationReportedList;
	}


	protected Immunization getImmunizationFromParameter(HttpServletRequest req, IGenericClient fhirClient) {
		Immunization immunization = null;
		if (req.getParameter(PARAM_VACCINATION_REPORTED_ID) != null) {
			immunization = fhirClient.read().resource(Immunization.class).withId(req.getParameter(PARAM_VACCINATION_REPORTED_ID)).execute();
		}
		return immunization;
	}

}
