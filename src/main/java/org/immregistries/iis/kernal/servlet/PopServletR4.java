package org.immregistries.iis.kernal.servlet;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Reference;
import org.immregistries.iis.kernal.fhir.annotations.OnR4Condition;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.logic.IncomingMessageHandler;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.InternalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.model.OrgMaster;
import org.immregistries.smm.transform.ScenarioManager;
import org.immregistries.smm.transform.TestCaseMessage;
import org.immregistries.smm.transform.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import static org.immregistries.iis.kernal.servlet.LoginServlet.*;

@RestController()
@RequestMapping("/pop")
@Conditional(OnR4Condition.class)
public class PopServletR4  {
	Logger logger = LoggerFactory.getLogger(PopServletR4.class);
	public static final String PARAM_MESSAGE = "MESSAGEDATA";
	@Autowired
	RepositoryClientFactory repositoryClientFactory;
	@Autowired
	private IncomingMessageHandler handler;

	@PostMapping()
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		try {
			String message = req.getParameter(PARAM_MESSAGE);
			OrgMaster orgMaster = ServletHelper.getOrgMaster();

			String ack = "";
			String[] messages;
			StringBuilder ackBuilder = new StringBuilder();
			Session dataSession = PopServlet.getDataSession();
			try {
				if (orgMaster == null) {
					resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					out.println(
						"Access is not authorized. Facilityid, userid and/or password are not recognized. ");
				} else {
					HomeServlet.doHeader(out, "IIS Sandbox - PopResult");

					messages = message.split("MSH\\|\\^~\\\\&\\|");
					if (messages.length > 2) {
						req.setAttribute("groupPatientIds", new ArrayList<String>());
					}
					for (String msh : messages) {
						if (!msh.isBlank()) {
							ackBuilder.append(handler.process("MSH|^~\\&|" + msh, orgMaster));
							ackBuilder.append("\r\n");
						}
					}
					ack = ackBuilder.toString();
					ArrayList<String> groupPatientIds = (ArrayList<String>) req.getAttribute("groupPatientIds");
					if (groupPatientIds != null) {
						Group group = new Group();
						for (String id :
							groupPatientIds) {
							group.addMember().setEntity(new Reference().setReference("Patient/" + id));
						}
						repositoryClientFactory.newGenericClient(req).create().resource(group).execute();
					}
				}
			} finally {
				dataSession.close();
			}
//      resp.setContentType("text/plain");
			out.println("<p>");
			out.print(ack);
			out.println("<p>");

		} catch (Exception e) {
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			e.printStackTrace(out);
			e.printStackTrace(System.err);
		}
		out.flush();
		out.close();
	}

	@GetMapping()
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		OrgAccess orgAccess = ServletHelper.getOrgAccess();
		String userId = null;
		String password = null;
		String facilityId = null;
		try {
			String message = req.getParameter(PARAM_MESSAGE);
			if (message == null || message.equals("")) {
				TestCaseMessage testCaseMessage =
					ScenarioManager.createTestCaseMessage(ScenarioManager.SCENARIO_1_R_ADMIN_CHILD);
				Transformer transformer = new Transformer();
				transformer.transform(testCaseMessage);
				message = testCaseMessage.getMessageText();
			}
			if (StringUtils.isNotBlank(req.getParameter(PARAM_USERID))) {
				userId = req.getParameter(PARAM_USERID);
			}
			if (StringUtils.isBlank(userId)) {
				userId = "Mercy";
			}
			if (StringUtils.isNotBlank(req.getParameter(PARAM_PASSWORD))) {
				password = req.getParameter(PARAM_PASSWORD);
			}
			if (StringUtils.isBlank(password)) {
				password = "password1234";
			}
			if (req.getParameter(PARAM_TENANTID) == null || req.getParameter(PARAM_TENANTID).isBlank()) {
				facilityId = req.getParameter(PARAM_TENANTID);
				if (StringUtils.isBlank(facilityId)) {
					facilityId = "Mercy-Healthcare";
				}
			}

			{
				HomeServlet.doHeader(out, "IIS Sandbox - Pop");
				out.println("    <h2>Send Now</h2>");
				out.println("    <form action=\"pop\" method=\"POST\" target=\"_blank\">");
				out.println("      <h3>VXU Message</h3>");
				out.println("      <textarea class=\"w3-input\" name=\"" + PARAM_MESSAGE
					+ "\" rows=\"15\" cols=\"160\">" + message + "</textarea></td>");
				out.println("    <div class=\"w3-container w3-half w3-margin-top\">");


				if (orgAccess == null) {
					// TODO duplicate login form ?
//					out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_USERID
//						+ "\" value=\"" + userId + "\"/>");
//					out.println("      <label>User Id</label>");
//					out.println("      <input class=\"w3-input\" type=\"password\" name=\"" + PARAM_PASSWORD
//						+ "\"/>");
//					out.println("      <label>Password</label>");
//					out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_FACILITYID
//						+ "\" value=\"" + facilityId + "\"/>");
//					out.println("      <label>Facility Id</label>");
					out.println("<input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"submit\" value=\"Submit\"/>");
					out.println("    <span class=\"w3-yellow\">Test Data Only</span>");
				} else {

					out.println("    <div class=\"w3-container w3-card-4\">");
//					out.println("      <h3>Authentication</h3>");
//					out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_USERID
//						+ "\" value=\"" + orgAccess.getAccessName() + "\"/ disabled>");
//					out.println("      <label>User Id</label>");
//					out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_FACILITYID
//						+ "\" value=\"" + orgAccess.getOrg().getOrganizationName() + "\" disabled/>");
//					out.println("      <label>Facility Id</label>");
//					out.println("      <br/>");
					out.println("<input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"submit\" value=\"Submit\"/>");
					out.println("    <span class=\"w3-yellow\">Test Data Only</span>");

					out.println("    </div>");
				}


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
