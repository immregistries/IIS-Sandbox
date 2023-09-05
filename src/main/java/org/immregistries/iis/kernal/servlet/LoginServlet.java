package org.immregistries.iis.kernal.servlet;

import org.hibernate.Session;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgMaster;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@RestController
@RequestMapping("/loginForm")
public class LoginServlet {

	public static final String PARAM_USERID = "USERID";
	public static final String PARAM_PASSWORD = "PASSWORD";
	public static final String PARAM_TENANTID = "TENANTID";
	public static final String PARAM_ORG_ID = "orgId";

	public static final String PARAM_ACTION = "action";
	public static final String ACTION_LOGIN = "Login";

	@PostMapping()
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		doGet(req, resp);
	}

	@GetMapping()
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		Session dataSession = PopServlet.getDataSession();
		try {
			HomeServlet.doHeader(out, "IIS Sandbox");
			OrgAccess orgAccess = ServletHelper.getOrgAccess();
			if (orgAccess == null) { // LOGIN FORM, inherited, could be made in a separate class and improved
				String userId = req.getParameter(PARAM_USERID);
				String tenantId = req.getParameter(PARAM_TENANTID);
				if (userId == null) {
					userId = "";
				}
				if (tenantId == null) {
					tenantId = "";
				}
				if (req.getParameter(PARAM_ORG_ID) != null) {
					OrgMaster orgMaster = dataSession.get(OrgMaster.class,
						Integer.parseInt(req.getParameter(PARAM_ORG_ID)));
					tenantId = orgMaster.getOrganizationName();
				}
				out.println("<div class=\"w3-container w3-card-4\">");
				out.println("	<h2>Login</h2>");
				out.println("	<form method=\"POST\" action=\"login\" class=\"w3-container w3-card-4 w3-half\">");
				out.println("		<input class=\"w3-input\" type=\"text\" name=\"" + PARAM_USERID + "\" value=\"" + userId + "\" required/>");
				out.println("		<label>User Id</label>");
				out.println("		<input class=\"w3-input\" type=\"password\" name=\"" + PARAM_PASSWORD + "\" value=\"\"/>");
				out.println("		<label>Password</label>");
				out.println("		<input class=\"w3-input\" type=\"text\" name=\"" + PARAM_TENANTID + "\" value=\"" + tenantId + "\"/>");
				out.println("		<label>Tenant Name (optional)</label><br/>");
				out.println("		<input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_LOGIN + "\"/>");
				out.println("	</form>");
				out.println("	<div class=\"w3-container w3-card-4 w3-half\">");
				out.println("		<h3>OAuth2</h3>");
				out.println("		<a href=\"oauth2/authorization/github\" class=\"w3-button w3-section w3-teal w3-ripple\">GitHub</a>\n");
				out.println("	</div>");
				out.println("</div>");
			}
		} catch (Exception e) {
			System.err.println("Unable to render page: " + e.getMessage());
			e.printStackTrace(System.err);
		} finally {
			dataSession.close();
		}
		HomeServlet.doFooter(out);
		out.flush();
		out.close();
	}

}
