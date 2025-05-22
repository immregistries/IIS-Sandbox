package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.context.FhirContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.logic.V2ToFhirMessageHandler;
import org.immregistries.iis.kernal.mapping.interfaces.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.internalClient.IFhirRequester;
import org.immregistries.iis.kernal.mapping.internalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.model.Tenant;
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

import static org.immregistries.iis.kernal.servlet.V2ToFhirController.V2_TO_FHIR_BASE_PATH;


@RestController()
@RequestMapping({V2_TO_FHIR_BASE_PATH, TenantController.TENANT_PATH + V2_TO_FHIR_BASE_PATH})
@Conditional(OnR4Condition.class)
public class V2ToFhirController {
	public static final String V2_TO_FHIR = "v2ToFhir";
	public static final String V2_TO_FHIR_BASE_PATH = "/" + V2_TO_FHIR;
	public static final String PARAM_MESSAGE = "MESSAGEDATA";
	public static final String PARAM_FACILITY_NAME = "FACILITY_NAME";
	public static final String MSH_HEADER_REGEX = "MSH\\|\\^~\\\\&\\|";
	public static final String MSH_HEADER = "MSH|^~\\&|";
	@Autowired
	RepositoryClientFactory repositoryClientFactory;
	@Autowired
	ImmunizationMapper immunizationMapper;
	@Autowired
	IFhirRequester fhirRequester;
	@Autowired
	V2ToFhirMessageHandler v2ToFhirMessageHandler;
	@Autowired
	FhirContext fhirContext;

	@PostMapping
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		Session dataSession = null;
		try {
			dataSession = ServletHelper.getDataSession();
			Tenant tenant = ServletHelper.getTenant(req, dataSession);
			String ack = "";
			String[] messages;
			StringBuilder stringBuilder = new StringBuilder();
			String message = req.getParameter(PARAM_MESSAGE);
			String facility_name = req.getParameter(PARAM_FACILITY_NAME);
			if (tenant == null) {
				resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				out.println("Access is not authorized. FacilityId, userid and/or password are not recognized. ");
			} else {
				HomeServlet.doHeader(out, "IIS Sandbox - V2ToFhir Result", tenant);

				messages = message.split(MSH_HEADER_REGEX);
				for (String msh : messages) {
					if (!msh.isBlank()) {
						stringBuilder.append(v2ToFhirMessageHandler.process(MSH_HEADER + msh, tenant, facility_name));
					}
				}
				ack = stringBuilder.toString();
			}
//      resp.setContentType("text/plain");
			out.println("<textarea name=\"result\" readonly style=\"width: 100%; height: 90%;\" >");
			out.print(ack);
			out.println("</textarea>");

		} catch (Exception e) {
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			e.printStackTrace(out);
			e.printStackTrace(System.err);
		} finally {
			if (dataSession != null) {
				dataSession.close();
			}
			out.flush();
			out.close();
		}
	}

	@GetMapping
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/html");
		Tenant tenant = ServletHelper.getTenant(req, ServletHelper.getDataSession());

		PrintWriter out = new PrintWriter(resp.getOutputStream());
		try {
			String message = req.getParameter(PARAM_MESSAGE);
			String organizationName = req.getParameter(PARAM_FACILITY_NAME);
			if (organizationName == null) {
				organizationName = "";
			}
			if (StringUtils.isBlank(message)) {
				TestCaseMessage testCaseMessage =
					ScenarioManager.createTestCaseMessage(ScenarioManager.SCENARIO_1_R_ADMIN_CHILD);
				Transformer transformer = new Transformer();
				transformer.transform(testCaseMessage);
				message = testCaseMessage.getMessageText();
			}


			{
				HomeServlet.doHeader(out, "IIS Sandbox - v2ToFhir", tenant);
				out.println("    <h2>Send Now</h2>");
				out.println("    <form action=\"" + V2_TO_FHIR + "\" method=\"POST\" target=\"_blank\" autocomplete=\"on\">");
				out.println("      <h3>VXU Message</h3>");
				out.println("      <textarea class=\"w3-input\" autocomplete=\"off\" name=\"" + PARAM_MESSAGE
					+ "\" rows=\"15\" cols=\"160\">" + message + "</textarea></td>");
				out.println("    <div class=\"w3-container w3-half w3-margin-top\">");


				out.println("    <div class=\"w3-container w3-card-4\">");
				out.println("		<input class=\"w3-input\" type=\"text\" auto name=\"" + PARAM_FACILITY_NAME + "\" value=\"" + organizationName + "\"/>");
				out.println("		<label>Sending organization name (Overriding the segments)</label>");
				out.println("		<br/>");

				out.println("		<input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"submit\" value=\"Submit\"/>");
				out.println("     <span class=\"w3-yellow\">Test Data Only</span>");

				out.println("    </div>");


				out.println("    </div>");
				out.println("    </form>");
				HomeServlet.doFooter(out);
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		out.flush();
		out.close();
	}

}
