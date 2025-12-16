package org.immregistries.iis.kernal.controllers.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.hl7v2.HL7Exception;
import gov.cdc.izgw.v2tofhir.converter.MessageParser;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.mapping.interfaces.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.internalClient.IFhirRequester;
import org.immregistries.iis.kernal.mapping.internalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.controllers.servlet.util.UiUtil;
import org.immregistries.smm.transform.ScenarioManager;
import org.immregistries.smm.transform.TestCaseMessage;
import org.immregistries.smm.transform.Transformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;

import static org.immregistries.iis.kernal.controllers.servlet.PopController.PARAM_FACILITY_NAME;
import static org.immregistries.iis.kernal.controllers.servlet.PopController.PARAM_MESSAGE;
import static org.immregistries.iis.kernal.controllers.servlet.V2ToFhirController.V2_TO_FHIR_BASE_PATH;

@RestController()
@RequestMapping({ V2_TO_FHIR_BASE_PATH, TenantController.TENANT_PATH + V2_TO_FHIR_BASE_PATH })
@Conditional(OnR4Condition.class)
public class V2ToFhirController {
	public static final String V2_TO_FHIR_PATH_KEY = "v2ToFhir";
	public static final String V2_TO_FHIR_BASE_PATH = "/" + V2_TO_FHIR_PATH_KEY;

	@Autowired
	FhirContext fhirContext;

	@PostMapping
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		try {
			Tenant tenant = CurrentTenantUtil.getTenant(req);
			String result = "";
			String message = req.getParameter(PARAM_MESSAGE);
			String facility_name = req.getParameter(PARAM_FACILITY_NAME);
			if (tenant == null) {
				resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				out.println("Access is not authorized. FacilityId, userid and/or password are not recognized. ");
			} else {
				UiUtil.doHeader(out, "IIS Sandbox - V2ToFhir Result", tenant);
				MessageParser parser = new MessageParser();
				try {
					Bundle bundle = parser.convert(message);

					result = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);
					// MethodOutcome result =
					// repositoryClientFactory.getFhirClient().create().resource(bundle).execute();
					// return
					// fhirContext.newJsonParser().encodeResourceToString(result.getResource());
				} catch (HL7Exception e) {
					throw new RuntimeException(e);
				}
			}
			// resp.setContentType("text/plain");
			out.println("<textarea name=\"result\" readonly style=\"width: 100%; height: 90%;\" >");
			out.print(result);
			out.println("</textarea>");

		} catch (Exception e) {
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			e.printStackTrace(out);
			e.printStackTrace(System.err);
		} finally {
			out.flush();
			out.close();
		}
	}

	@GetMapping
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/html");
		Tenant tenant = CurrentTenantUtil.getTenant(req);

		PrintWriter out = new PrintWriter(resp.getOutputStream());
		try {
			String message = req.getParameter(PARAM_MESSAGE);
			String organizationName = req.getParameter(PARAM_FACILITY_NAME);
			if (organizationName == null) {
				organizationName = "";
			}
			if (StringUtils.isBlank(message)) {
				TestCaseMessage testCaseMessage = ScenarioManager
						.createTestCaseMessage(ScenarioManager.SCENARIO_1_R_ADMIN_CHILD);
				Transformer transformer = new Transformer();
				transformer.transform(testCaseMessage);
				message = testCaseMessage.getMessageText();
			}

			UiUtil.doHeader(out, "IIS Sandbox - v2ToFhir", tenant);
			out.println("<h2>Convert to FHIR</h2>");
			PopController.printForm(out, "V2 Message", message, organizationName, V2_TO_FHIR_PATH_KEY);
			UiUtil.doFooter(out);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		out.flush();
		out.close();
	}

}
