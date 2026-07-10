package org.immregistries.iis.kernal.controllers.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.immregistries.iis.kernal.controllers.rest.AuthenticationRestController;
import org.immregistries.iis.kernal.controllers.rest.TenantRestController;
import org.immregistries.iis.kernal.controllers.servlet.util.UiUtil;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;

@RestController
@RequestMapping("/loginForm")
public class LoginFormController {

	@Autowired
	private AuthenticationRestController authenticationRestController;
	@Autowired
	private TenantRestController tenantRestController;
	@Autowired
	private UiUtil uiUtil;

	public static final String LOGIN_PARAM_USERID = "USERID";
	public static final String LOGIN_PARAM_PASSWORD = "PASSWORD";
	public static final String LOGIN_PARAM_TENANT_NAME = "TENANTID";
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
		String locationHeader = req.getHeader("referer");
		try {
			uiUtil.doHeader(out, "IIS Sandbox");
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			// LOGIN FORM, inherited, could be made in a separate class and improved
			if (!authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
				String userId = req.getParameter(LOGIN_PARAM_USERID);
				String tenantName = req.getParameter(LOGIN_PARAM_TENANT_NAME);
				if (userId == null) {
					userId = "";
				}
				if (tenantName == null) {
					tenantName = "";
				}
				if (req.getParameter(PARAM_ORG_ID) != null) {
					Tenant tenant = tenantRestController.getTenant(null, Integer.parseInt(req.getParameter(PARAM_ORG_ID)));
					tenantName = tenant.getOrganizationName();
				}
				out.println("<div class=\"w3-container w3-card-4\">");
				out.println("	<h2>Login</h2>");
				out.println("	<form method=\"POST\" action=\"login\" class=\"w3-container w3-card-4 w3-half\">");
				out.println("		<input class=\"w3-input\" type=\"hidden\" name=\"referer\" value=\""
						+ locationHeader + "\"/>");

				out.println("		<input class=\"w3-input\" type=\"text\" name=\"" + LOGIN_PARAM_USERID
						+ "\" value=\"" + userId + "\" required autofocus/>");
				out.println("		<label>User Id</label>");
				out.println("		<input class=\"w3-input\" type=\"password\" name=\"" + LOGIN_PARAM_PASSWORD
						+ "\" value=\"\"/>");
				out.println("		<label>Password</label>");
				out.println("		<input class=\"w3-input\" type=\"text\" name=\"" + LOGIN_PARAM_TENANT_NAME
						+ "\" value=\"" + tenantName + "\"/>");
				out.println("		<label>Tenant Name (optional)</label>");
				out.println("		<br/>");
				out.println("		<input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\""
						+ PARAM_ACTION + "\" value=\"" + ACTION_LOGIN + "\"/>");
				out.println("	</form>");
				out.println("	<div class=\"w3-container w3-card-4 w3-half\">");
				out.println("		<h3>OAuth2</h3>");
				out.println(
						"		<a href=\"oauth2/authorization/github\" class=\"w3-button w3-section w3-teal w3-ripple\">GitHub</a>\n");
				out.println("	</div>");
				out.println("</div>");
			}
		} catch (Exception e) {
			System.err.println("Unable to render page: " + e.getMessage());
			e.printStackTrace(System.err);
		}
		uiUtil.doFooter(out);
		out.flush();
		out.close();
	}

}
