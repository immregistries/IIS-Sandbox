package org.immregistries.iis.kernal.controllers.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.immregistries.iis.kernal.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.controllers.servlet.util.UiUtil;
import org.immregistries.iis.kernal.controllers.servlet.util.UrlTenantUtil;
import org.immregistries.smm.cdc.CDCWSDLServer;
import org.immregistries.smm.cdc.ProcessorFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;

import static org.immregistries.iis.kernal.controllers.servlet.SoapDescriptionController.SOAP_BASE_PATH;

@RestController
@RequestMapping({ SOAP_BASE_PATH, TenantController.TENANT_PATH + SOAP_BASE_PATH })
public class SoapDescriptionController {

	public static final String SOAP_BASE_PATH = "/soap";

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
				Tenant tenant = CurrentTenantUtil.getTenant();
				UiUtil.doHeader(out, "IIS Sandbox", tenant);
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
				out.println("<p><a href=\"" + UrlTenantUtil.tenantifyPathWithContextPath(tenant, "soap")
						+ "\">See WSDL</a></p>");
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
			UiUtil.doFooter(out);
		}
	}

}
