 package org.immregistries.iis.kernal.servlet;

import org.hibernate.Session;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.logic.AbstractIncomingMessageHandler;
import org.immregistries.iis.kernal.model.Tenant;
import org.immregistries.smm.cdc.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

@RestController
@RequestMapping("/soap")
public class SoapController extends HttpServlet {

	@Autowired
	AbstractIncomingMessageHandler handler;

	@PostMapping
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {

		String path = req.getPathInfo();
		final String processorName =
			path == null ? "" : (path.startsWith("/") ? path.substring(1) : path);
		CDCWSDLServer server = new CDCWSDLServer() {
			@Override
			public void process(SubmitSingleMessage ssm, PrintWriter out) throws Fault {

				String message = ssm.getHl7Message();
				String userId = ssm.getUsername();
				String password = ssm.getPassword();
				String facilityId = ssm.getFacilityID();
				String ack = "";
				Session dataSession = ServletHelper.getDataSession();
				String[] messages;
				StringBuilder ackBuilder = new StringBuilder();
				try {
					Tenant tenant = ServletHelper.authenticateTenant(userId, password, facilityId, dataSession);
					if (tenant == null) {
						throw new SecurityException("Username/password combination is unrecognized");
					} else {
						HttpSession session = req.getSession(true);
						session.setAttribute("tenant", tenant);
						messages = message.split("MSH\\|\\^~\\\\&\\|");
						for (String msh : messages) {
							if (!msh.isBlank()) {
								ackBuilder.append(handler.process("MSH|^~\\&|" + msh, tenant, null));
								ackBuilder.append("\n");
							}
						}
						ack = ackBuilder.toString();
					}
				} catch (Exception e) {
					throw new UnknownFault("Unable to process request: " + e.getMessage(), e);
				} finally {
					dataSession.close();
				}
				out.print(ack);
			}

			@Override
			public String getEchoBackMessage(String message) {
				return "End-point is ready. Echoing: " + message;
			}

			@Override
			public void authorize(SubmitSingleMessage ssm) throws Fault {
				String userId = ssm.getUsername();
				String password = ssm.getPassword();
				String facilityId = ssm.getFacilityID();
				Session dataSession = ServletHelper.getDataSession();
				try {
					if ("NPE".equals(userId) && "NPE".equals(password)) {
						throw new UnknownFault("Unknown Fault");
					}
					Tenant tenant = ServletHelper.authenticateTenant(userId, password, facilityId, dataSession);
					if (tenant == null) {
						throw new SecurityFault("Username/password combination is unrecognized");
					}
				} finally {
					dataSession.close();
				}
			}
		};
		server.setProcessorName(processorName);
		server.process(req, resp);
	}

	@GetMapping
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		String wsdl = req.getParameter("wsdl");
		if (wsdl != null) {
			resp.setContentType("text/xml");
			PrintWriter out = new PrintWriter(resp.getOutputStream());
			CDCWSDLServer.printWSDL(out, "http://localhost:8282/wsdl-demo");
			out.close();
		} else {
			resp.setContentType("text/html;charset=UTF-8");

			PrintWriter out = resp.getWriter();
			try {
				HomeServlet.doHeader(out, "IIS Sandbox", ServletHelper.getTenant());
				out.println("<h2>CDC SOAP Endpoint</h2>");
				out.println("<p>");
				out.println("This demonstration system supports the use of the ");
				out.println(
					"<a href=\"http://www.cdc.gov/vaccines/programs/iis/technical-guidance/soap/wsdl.html\">CDC ");
				out.println("WSDL</a>");
				out.println(" which has been defined to support the transport of HL7 messages ");
				out.println("sent to Immunization Information Systems (IIS).  ");
				out.println("</p>");
				out.println("<h2>Usage Instructions</h2>");
				out.println("<h3>WSDL</h3>");
				out.println("<p><a href=\"soap?wsdl=true\">See WSDL</a></p>");
				out.println("<h3>Authentication</h3>");
				out.println(
					"<p>Authentication credentials can be established by submitting a username and password to a facility "
						+ "not already defined in the IIS Sandbox. Submitting new credentials will cause IIS Sandbox to create an "
						+ "organization to represent the facility and a user access account for the supplied credentials. Access to "
						+ "this account and facility/organization data will be allowed to anyone submitting the correct credentials. "
						+ "There is some additional functionality to support testing of specific transport issues:</p>");
				out.println("<ul>");
				out.println(
					"  <li><b>Bad Credentials</b>: Simply change the password or username for any currently established "
						+ "account and it will generate an unauthorized exception. This can be repeated as often as possible, the "
						+ "account will not lock. </li>");
				out.println(
					"  <li><b>NPE/NPE</b>: Using this as the username and password will trigger an Null Pointer Exception. "
						+ "This can be used to simulate the situation where an unexpected error occurs. </li>");
				out.println("</ul>");
				out.println("<h3>Content</h3>");
				out.println("<p>HL7 VXU or QBP message is expected in payload.  </p>");
				out.println("<h3>Multiple Messages</h3>");
				out.println(
					"<p>If the message contains more than one MSH segment a Message Too Large Fault ");
				out.println("will be returned. ");
				out.println(
					"Use this feature to test situations where the IIS can not process more than one message. </p>");
				out.println("<h2>Alternative Behavior</h2>");
				out.println("<p>Additional end points are available, which provide different behaviors ");
				out.println("(some good and some bad). ");
				out.println("These can be used to demonstrate different or bad interactions. </p>");
				ProcessorFactory.printExplanations(out);
			} finally {
				out.close();
			}
			HomeServlet.doFooter(out);
		}
	}

}
