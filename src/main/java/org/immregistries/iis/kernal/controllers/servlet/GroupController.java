package org.immregistries.iis.kernal.controllers.servlet;

import ca.uhn.fhir.context.FhirContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hl7.fhir.r5.model.Group;
import org.hl7.fhir.r5.model.Identifier;
import org.hl7.fhir.r5.model.Parameters;
import org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.controllers.rest.GroupRestController;
import org.immregistries.iis.kernal.controllers.servlet.util.UiUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Prototype of Servlet tool for group which would allow to
 * - generate accurate group for bulk and subscription
 * - provide option to add/remove a member
 * - link with a subscription
 *
 */
@RestController
@RequestMapping({ "/group", TenantController.TENANT_PATH + "/group" })
public class GroupController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	FhirContext fhirContext;
	@Autowired
	GroupRestController groupRestController;

	@PostMapping
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doGet(req, resp);

		// Reference orgReference =
		// repositoryClientFactory.getFhirContext().newJsonParser().parseResource(Reference.class,orgString);

	}

	@GetMapping
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		UiUtil.doHeader(out, "IIS Sandbox - Groups", CurrentTenantUtil.getTenant());
		Group group = groupRestController.generateGroup(req);
		out.println("<p>");
		out.println(fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(group));
		out.println("</p>");
		Parameters parameters = new Parameters().addParameter("test", new Identifier().setValue("identifierTest"));
		out.println("<p>");
		out.println(fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(parameters));
		out.println("</p>");
		UiUtil.doFooter(out);
		out.flush();
		out.close();
	}

}
