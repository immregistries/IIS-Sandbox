 package org.immregistries.iis.kernal.servlet;

 import jakarta.servlet.ServletException;
 import jakarta.servlet.http.HttpServletRequest;
 import jakarta.servlet.http.HttpServletResponse;
 import org.immregistries.iis.kernal.fhir.interceptors.PartitionCreationInterceptor;
 import org.immregistries.iis.kernal.fhir.security.ServletHelper;
 import org.immregistries.iis.kernal.logic.BaseIISSOAPServer;
 import org.immregistries.iis.kernal.logic.messageHandling.V2IncomingMessageHandler;
 import org.immregistries.iis.kernal.model.persisted.Tenant;
 import org.immregistries.smm.cdc.*;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.web.bind.annotation.*;

 import java.io.IOException;
 import java.io.PrintWriter;

 import static org.immregistries.iis.kernal.servlet.SoapController.SOAP_BASE_PATH;
 import static org.immregistries.iis.kernal.servlet.TenantController.PATH_VARIABLE_TENANT_NAME;

 @RestController
 @RequestMapping({SOAP_BASE_PATH, TenantController.TENANT_PATH + SOAP_BASE_PATH})
 public class SoapController {

	 public static final String SOAP_BASE_PATH = "/soap";
	 @Autowired
	 private V2IncomingMessageHandler handler;
	 @Autowired
	 private PartitionCreationInterceptor partitionCreationInterceptor;

	@PostMapping
	protected void doPost(HttpServletRequest req, HttpServletResponse resp, @PathVariable(name = PATH_VARIABLE_TENANT_NAME, required = false) String tenantName)
		throws ServletException, IOException {

		String path = req.getPathInfo();
		final String processorName =
			path == null ? "" : (path.startsWith("/") ? path.substring(1) : path);
		CDCWSDLServer server = new BaseIISSOAPServer(partitionCreationInterceptor, tenantName) {
			@Override
			public void process(SubmitSingleMessage ssm, PrintWriter out) throws Fault {
				String message = ssm.getHl7Message();

				String ack = "";
				try {
					/*
					 * Tenant is accessed through RequestContext, and was previously set through the authorize method of WSDL server
					 */
					Tenant tenant = ServletHelper.getTenant();
					if (tenant == null) {
						throw new SecurityException("Username/password combination is unrecognized");
					} else {
						ack = handler.process(message, tenant, null);
					}
				} catch (Exception e) {
					throw new UnknownFault("Unable to process request: " + e.getMessage(), e);
				}
				out.print(ack);
			}
		};
		server.setProcessorName(processorName);
		server.process(req, resp);
	}

	@GetMapping
	protected void doGet(HttpServletRequest req, HttpServletResponse resp, @PathVariable(name = PATH_VARIABLE_TENANT_NAME, required = false) String tenantName)
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
				Tenant tenant = ServletHelper.getTenant();
				HomeController.doHeader(out, "IIS Sandbox", tenant);
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
				out.println("<p><a href=\"" + ServletHelper.tenantifyPathWithContextPath(tenant, "soap") + "\">See WSDL</a></p>");
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
			HomeController.doFooter(out);
		}
	}

}
