package org.immregistries.iis.kernal.servlet;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hl7.fhir.r5.model.Group;
import org.hl7.fhir.r5.model.Reference;
import org.immregistries.iis.kernal.fhir.annotations.OnR5Condition;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.logic.IncomingMessageHandler;
import org.immregistries.iis.kernal.model.UserAccess;
import org.immregistries.iis.kernal.InternalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.model.Tenant;
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

@RestController()
@RequestMapping({"/pop","/tenant/{tenantId}/pop"})
@Conditional(OnR5Condition.class)
public class PopServlet {
	Logger logger = LoggerFactory.getLogger(PopServlet.class);
	public static final String PARAM_MESSAGE = "MESSAGEDATA";
	private static SessionFactory factory;
	@Autowired
	RepositoryClientFactory repositoryClientFactory;
	@Autowired
	private IncomingMessageHandler handler;

	public static Session getDataSession() {
		if (factory == null) {
			factory = new Configuration().configure().buildSessionFactory();
		}
		return factory.openSession();
	}

	@PostMapping
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		try {
			String message = req.getParameter(PARAM_MESSAGE);
			Tenant tenant = ServletHelper.getTenant();

			String ack = "";
			String[] messages;
			StringBuilder ackBuilder = new StringBuilder();
			Session dataSession = getDataSession();
			try {
				if (tenant == null) {
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
							ackBuilder.append(handler.process("MSH|^~\\&|" + msh, tenant));
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

	@GetMapping
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		UserAccess userAccess = ServletHelper.getUserAccess();
		try {
			String message = req.getParameter(PARAM_MESSAGE);
			if (message == null || message.equals("")) {
				TestCaseMessage testCaseMessage =
					ScenarioManager.createTestCaseMessage(ScenarioManager.SCENARIO_1_R_ADMIN_CHILD);
				Transformer transformer = new Transformer();
				transformer.transform(testCaseMessage);
				message = testCaseMessage.getMessageText();
			}


			{
				HomeServlet.doHeader(out, "IIS Sandbox - Pop");
				out.println("    <h2>Send Now</h2>");
				out.println("    <form action=\"pop\" method=\"POST\" target=\"_blank\">");
				out.println("      <h3>VXU Message</h3>");
				out.println("      <textarea class=\"w3-input\" name=\"" + PARAM_MESSAGE
					+ "\" rows=\"15\" cols=\"160\">" + message + "</textarea></td>");
				out.println("    <div class=\"w3-container w3-half w3-margin-top\">");


				if (userAccess == null) {
					out.println("<input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"submit\" value=\"Submit\"/>");
					out.println("    <span class=\"w3-yellow\">Test Data Only</span>");
				} else {
					out.println("    <div class=\"w3-container w3-card-4\">");
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
