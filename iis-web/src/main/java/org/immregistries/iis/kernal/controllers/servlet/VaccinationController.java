package org.immregistries.iis.kernal.controllers.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.ReferenceParam;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.Application;
import org.immregistries.iis.kernal.controllers.rest.CodeMapRestController;
import org.immregistries.iis.kernal.controllers.rest.PatientRestController;
import org.immregistries.iis.kernal.controllers.rest.VaccinationRestController;
import org.immregistries.iis.kernal.controllers.servlet.util.PatientServletUtil;
import org.immregistries.iis.kernal.controllers.servlet.util.RedirectUtil;
import org.immregistries.iis.kernal.controllers.servlet.util.UiUtil;
import org.immregistries.iis.kernal.controllers.servlet.util.UrlTenantUtil;
import org.immregistries.iis.kernal.mapping.IisFhirClientFactory;
import org.immregistries.iis.kernal.mapping.mappers.resources.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.requesters.FhirRequesterUtil;
import org.immregistries.iis.kernal.mapping.requesters.FhirSearchRequester;
import org.immregistries.iis.kernal.model.IisPatient;
import org.immregistries.iis.kernal.model.IisVaccination;
import org.immregistries.iis.kernal.model.ObservationReported;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.enums.LoincIdentifier;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.security.CurrentTenantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.immregistries.iis.kernal.controllers.servlet.VaccinationController.VACCINATION_BASE_PATH;

@RestController
@RequestMapping({ VACCINATION_BASE_PATH, TenantController.TENANT_PATH + VACCINATION_BASE_PATH })
public class VaccinationController {
	public static final String VACCINATION_BASE_PATH = "/vaccination";

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public static final String PARAM_ACTION = "action";
	public static final String PARAM_RESOURCE = "resource";
	public static final String PARAM_VACCINATION_REPORTED_ID = "vaccinationReportedId";
	@Autowired
	IisFhirClientFactory iisFhirClientFactory;
	@Autowired
	ImmunizationMapper immunizationMapper;
	@Autowired
	FhirContext fhirContext;

	@Autowired
	VaccinationRestController vaccinationRestController;
	@Autowired
	PatientRestController patientRestController;
	@Autowired
	FhirSearchRequester fhirSearchRequester;
	@Autowired
	CodeMapRestController codeMapRestController;
	

	@PostMapping
	protected void doPost(HttpServletRequest req, HttpServletResponse resp
	// , @PathVariable(name = TenantController.PATH_VARIABLE_TENANT_NAME, required =
	// false) String tenantName dealt with in filter
	)
			throws ServletException, IOException {
		doGet(req, resp);
	}

	@GetMapping
	protected void doGet(HttpServletRequest req, HttpServletResponse resp
	// , @PathVariable(name = TenantController.PATH_VARIABLE_TENANT_NAME, required =
	// false) String tenantName dealt with in filter
	) throws ServletException, IOException {
		Tenant tenant = RedirectUtil.getTenantRedirectIfNone(req, resp);
		IGenericClient fhirClient = iisFhirClientFactory.getOrCreateGenericClient(req);

		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		try {
			IAnyResource immunizationResource = getImmunizationFromParameter(req, fhirClient);
			if (immunizationResource == null) {
				out.println("<h2>Failed to find Vaccination with id : "
						+ req.getParameter(PARAM_VACCINATION_REPORTED_ID) + "</h2>");
			}
			IisVaccination vaccination;

			if (FhirRequesterUtil.isGoldenRecord(immunizationResource)) {
				vaccination = immunizationMapper.localObject(immunizationResource);
			} else {
				vaccination = immunizationMapper.localObjectReported(immunizationResource);
			}

			String action = req.getParameter(PARAM_ACTION);
			if (action != null) {
			}
			CodeMap codeMap = codeMapRestController.getCodeMaps(tenant);
			String cvxPrint = "Unknown CVX";
			if (!StringUtils.isEmpty(vaccination.getVaccineCvxCode())) {
				Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE,
						vaccination.getVaccineCvxCode());
				if (cvxCode == null) {
					cvxPrint = "Unknown CVX (" + vaccination.getVaccineCvxCode() + ")";
				} else {
					cvxPrint = cvxCode.getLabel() + " (" + vaccination.getVaccineCvxCode() + ")";
				}
			}

			UiUtil.doHeader(out, "IIS Sandbox - Vaccinations", tenant);
			SimpleDateFormat sdfDate = new SimpleDateFormat("MM/dd/yyyy");

			out.println("<h2>Vaccination Record: " + cvxPrint + " " + sdfDate.format(vaccination.getAdministeredDate())
					+ "</h2>");
			PatientReported patientReportedSelected = (PatientReported) patientRestController.getPatient(vaccination.getPatientReportedId(), tenant);
			{
				out.println("<h4>Patient information</h4>");
				PatientServletUtil.printPatient(out, patientReportedSelected);

				out.println("  <div class=\"w3-container\">");
				out.println("<h4>Vaccination details</h4>");
				{
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
						out.println(cvxPrint);
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
							Code informationCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_INFORMATION_SOURCE,
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

				List<ObservationReported> observationReportedList = getObservationList(vaccination);

				if (!observationReportedList.isEmpty()) {
					out.println("<h4>Observations</h4>");
					PatientServletUtil.printObservationList(out, observationReportedList);
				}

				{
					List<? extends IisVaccination> relatedVaccinations = vaccinationRestController.getRelatedVaccinations(vaccination.getVaccinationId(), tenant, FhirRequesterUtil.isGoldenRecord(immunizationResource));
					out.println("<h4>Related Vaccination Records</h4>");
					printVaccinationList(out, relatedVaccinations, tenant, codeMap);
					UiUtil.printGoldenRecordExplanation(out, immunizationResource);
				}

				out.println("  </div>");

				if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
					org.hl7.fhir.r5.model.Bundle bundle = fhirClient.search()
							.forResource(org.hl7.fhir.r5.model.Subscription.class)
							.returnBundle(org.hl7.fhir.r5.model.Bundle.class).execute();

					org.hl7.fhir.r5.model.Immunization immunization = (org.hl7.fhir.r5.model.Immunization) immunizationResource;
					/*
					 * Setting external identifier in reference
					 */
					IisPatient iisPatient = vaccination.getPatientReported();
					immunization.getPatient().setIdentifier(new org.hl7.fhir.r5.model.Identifier()
						.setValue(iisPatient.getMainBusinessIdentifier().getValue())
						.setSystem(iisPatient.getMainBusinessIdentifier().getSystem()));
					IParser parser = iisFhirClientFactory.getFhirContext().newJsonParser().setPrettyPrint(true);

					PatientServletUtil.printSubscriptions(out, parser, bundle, immunization);
				}

				{
					out.println("<div class=\"w3-container\">");
					out.println("<h4>FHIR Api Shortcuts</h4>");
					String apiBaseUrl = Application.fhirServerBasePath(tenant);
					{
						String link = apiBaseUrl + "/Immunization?_id=" + vaccination.getVaccinationId();
						out.println("<div>FHIR Immunization: <a href=\"" + link + "\">" + link + "</a></div>");
					}
					{
						String link = apiBaseUrl + "/Immunization?patient=" + vaccination.getPatientReportedId();
						out.println("<div>Other immunizations of same patient: <a href=\"" + link + "\">" + link
								+ "</a></div>");
					}
					{
						String link;
						if (FhirRequesterUtil.isGoldenRecord(immunizationResource)) {
							link = apiBaseUrl + "/$mdm-query-links?goldenResourceId=" + vaccination.getVaccinationId();
						} else {
							link = apiBaseUrl + "/$mdm-query-links?resourceId=" + vaccination.getVaccinationId();
						}
						out.println(
								"<div>Related Immunization Records: <a href=\"" + link + "\">" + link + "</a></div>");
					}
					out.println("</div>");
				}

			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		UiUtil.doFooter(out);
		out.flush();
		out.close();
	}

	@SuppressWarnings("unchecked")
	public List<ObservationReported> getObservationList(IisVaccination vaccination) {
		List<ObservationReported> observationReportedList;
		{
			observationReportedList = fhirSearchRequester.searchObservationReportedList(
					new SearchParameterMap("patient", new ReferenceParam(vaccination.getPatientReportedId())));
			// Observation.PATIENT.hasId(vaccination.getPatientReportedId()));
			Set<String> suppressSet = LoincIdentifier.getSuppressIdentifierCodeSet();
			for (Iterator<ObservationReported> it = observationReportedList.iterator(); it.hasNext();) {
				ObservationReported observationReported = it.next();
				if (suppressSet.contains(observationReported.getIdentifierCode())) {
					it.remove();
				}
			}
		}
		return observationReportedList;
	}

	protected IAnyResource getImmunizationFromParameter(HttpServletRequest req, IGenericClient fhirClient) {
		IAnyResource immunization = null;
		if (req.getParameter(PARAM_VACCINATION_REPORTED_ID) != null) {
			immunization = (IAnyResource) fhirClient.read().resource("Immunization")
					.withId(req.getParameter(PARAM_VACCINATION_REPORTED_ID)).execute();
		}
		return immunization;
	}

	public static void printVaccinationList(PrintWriter out, List<? extends IisVaccination> vaccinationList, Tenant tenant, CodeMap codeMap) {
		SimpleDateFormat sdfDate = new SimpleDateFormat("MM/dd/yyyy");

		if (vaccinationList.isEmpty()) {
			out.println("<div class=\"w3-panel w3-yellow\"><p>No Vaccinations</p></div>");
		} else {
			out.println(
					"<table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
			out.println("  <tr class=\"w3-green\">");
			out.println("    <th>Vaccine</th>");
			out.println("    <th>Admin Date</th>");
			out.println("    <th>Manufacturer</th>");
			out.println("    <th>Lot Number</th>");
			out.println("    <th>Information</th>");
			out.println("    <th>Completion</th>");
			out.println("    <th>Action</th>");
			out.println("  </tr>");
			out.println("  <tbody>");
			for (IisVaccination vaccination : vaccinationList) {
				out.println("  <tr>");
				out.println("    <td>");
				String link = "vaccination?" + VaccinationController.PARAM_VACCINATION_REPORTED_ID + "="
						+ vaccination.getVaccinationId();
				out.println("      <a href=\"" + UrlTenantUtil.tenantifyPathWithContextPath(tenant, link) + "\">");
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
				out.println("      </a>");
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
					Code informationCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_INFORMATION_SOURCE,
							vaccination.getInformationSource());
					if (informationCode != null) {
						out.print(informationCode.getLabel());
						out.println(" (" + vaccination.getInformationSource() + ")");
					}
				}
				out.println("    </td>");
				out.println("    <td>");
				if (!StringUtils.isEmpty(vaccination.getCompletionStatus())) {
					Code completionCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_COMPLETION,
							vaccination.getCompletionStatus());
					if (completionCode != null) {
						out.print(completionCode.getLabel());
						out.println(" (" + vaccination.getCompletionStatus() + ")");
					}
				}
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
	}

}
