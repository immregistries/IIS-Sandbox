package org.immregistries.iis.kernal.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.persisted.util.HibernateConfig;
import org.immregistries.smm.transform.ScenarioManager;
import org.immregistries.smm.transform.TestCaseMessage;
import org.immregistries.smm.transform.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.immregistries.iis.kernal.rest.PopRestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;

import static org.immregistries.iis.kernal.servlet.PopController.POP_BASE_PATH;

/**
 * Generated from PopServlet, changed to se PathVariable functionality
 */
@RestController()
@RequestMapping({ POP_BASE_PATH, TenantController.TENANT_PATH + POP_BASE_PATH })
public class PopController {
	public static final String POP_PATH_KEY = "pop";
	public static final String POP_BASE_PATH = "/" + POP_PATH_KEY;
	public static final String PARAM_MESSAGE = "MESSAGEDATA";
	public static final String PARAM_FACILITY_NAME = "FACILITY_NAME";
	public static final String MSH_HEADER_REGEX = "MSH\\|\\^~\\\\&\\|";
	public static final String MSH_HEADER = "MSH|^~\\&|";
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private PopRestController popRestController;

	@PostMapping
	// @Transactional
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		Session dataSession = null;
		try {
			dataSession = HibernateConfig.getDataSession();
			Tenant tenant = CurrentTenantUtil.getTenant(req, dataSession);

			String ack = "";

			String message = req.getParameter(PARAM_MESSAGE);
			String facility_name = req.getParameter(PARAM_FACILITY_NAME);
			if (tenant == null) {
				resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				out.println("Access is not authorized. FacilityId, userid and/or password are not recognized. ");
			} else {
				HomeController.doHeader(out, "IIS Sandbox - PopResult", tenant);

				PopRestController.PopRequest popRequest = new PopRestController.PopRequest();
				popRequest.setMessage(message);
				popRequest.setFacilityName(facility_name);
				ack = popRestController.postPop(popRequest, req);
			}
			// resp.setContentType("text/plain");
			out.println("<textarea name=\"ack\" readonly style=\"width: 100%; height: 90%;\" >");
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
		Tenant tenant = CurrentTenantUtil.getTenant(req, HibernateConfig.getDataSession());

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

			{
				HomeController.doHeader(out, "IIS Sandbox - Pop", tenant);
				out.println("    <h2>Send Now</h2>");
				printForm(out, "VXU Message", message, organizationName, POP_PATH_KEY);
				HomeController.doFooter(out);
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		out.flush();
		out.close();
	}

	public static void printForm(PrintWriter out, String title, String message, String organizationName,
			String formDestination) {
		out.println(
				"    <form action=\"" + formDestination + "\" method=\"POST\" target=\"_blank\" autocomplete=\"on\">");
		if (StringUtils.isNotBlank(title)) {
			out.println("      <h3>" + title + "</h3>");

		}
		out.println("      <textarea class=\"w3-input\" autocomplete=\"off\" name=\"" + PARAM_MESSAGE
				+ "\" rows=\"15\" cols=\"160\">" + message + "</textarea></td>");
		out.println("    <div class=\"w3-container w3-half w3-margin-top\">");

		out.println("    <div class=\"w3-container w3-card-4\">");
		out.println("		<input class=\"w3-input\" type=\"text\" auto name=\"" + PARAM_FACILITY_NAME + "\" value=\""
				+ organizationName + "\"/>");
		out.println("		<label>Sending organization name (Overriding the segments)</label>");
		out.println("		<br/>");

		out.println(
				"		<input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"submit\" value=\"Submit\"/>");
		out.println("     <span class=\"w3-yellow\">Test Data Only</span>");

		out.println("    </div>");

		out.println("    </div>");
		out.println("    </form>");
	}

}
