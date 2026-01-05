package org.immregistries.iis.kernal.controllers.servlet;

import ca.uhn.fhir.context.FhirContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.immregistries.iis.kernal.controllers.rest.PopRestController;
import org.immregistries.iis.kernal.controllers.rest.V2ToFhirRestController;
import org.immregistries.iis.kernal.controllers.servlet.util.UiUtil;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;

import static org.immregistries.iis.kernal.controllers.servlet.FhirMessagingController.FHIR_MESSAGING_BASE_PATH;
import static org.immregistries.iis.kernal.controllers.servlet.PopController.PARAM_FACILITY_NAME;
import static org.immregistries.iis.kernal.controllers.servlet.PopController.PARAM_MESSAGE;

@RestController()
@RequestMapping({FHIR_MESSAGING_BASE_PATH, TenantController.TENANT_PATH + FHIR_MESSAGING_BASE_PATH})
@Conditional(OnR4Condition.class)
public class FhirMessagingController {
	public static final String FHIR_MESSAGING_PATH_KEY = "fhirMessaging";
	public static final String FHIR_MESSAGING_BASE_PATH = "/" + FHIR_MESSAGING_PATH_KEY;
	public static final String ORIGINAL_TEXT_EXTENSION_URL = "http://hl7.org/fhir/StructureDefinition/originalText";
	@Autowired
	FhirContext fhirContext;

	@Autowired
	PopRestController popRestController;
	@Autowired
	V2ToFhirRestController v2ToFhirRestController;

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
				String sampleMessage = popRestController.getSampleMessage();
				Bundle bundle = v2ToFhirRestController.convertV2ToFhir(sampleMessage, null);
				message = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);
			}

			UiUtil.doHeader(out, "IIS Sandbox - FHIR Messaging", tenant);
			out.println("<h2>Experimental FHIR Messaging Endpoint</h2>");
			PopController.printForm(out, "FHIR Bundle", message, organizationName, "rest/" + FHIR_MESSAGING_PATH_KEY);
			UiUtil.doFooter(out);

		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		out.flush();
		out.close();
	}




}
