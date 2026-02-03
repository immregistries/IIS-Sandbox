package org.immregistries.iis.kernal.controllers.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotBlank;
import org.immregistries.iis.kernal.controllers.rest.TenantRestController;
import org.immregistries.iis.kernal.controllers.servlet.util.UiUtil;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.immregistries.iis.kernal.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.security.TenantAuthService;
import org.immregistries.iis.kernal.security.UserAccessUtil;
import org.immregistries.iis.kernal.services.api.IDeployedApiUrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Tenant management UI page
 */
@RestController()
@RequestMapping({ TenantController.TENANT_BASE_PATH, TenantController.TENANT_PATH,
		TenantController.TENANT_PATH + TenantController.TENANT_BASE_PATH })
public class TenantController {
	public static final String TENANT_BASE_PATH = "/tenant";
	public static final String TENANT_NAME = "tenantName";
	public static final String TENANT_NAME_PLACEHOLDER = "/{" + TENANT_NAME + "}";
	public static final String TENANT_PATH = TENANT_BASE_PATH + TENANT_NAME_PLACEHOLDER;

	public static final String PARAM_ACTION = "action";

	public static final String ACTION_SWITCH = "Switch";
	public static final String PARAM_TENANT_NAME = "tenantName";
	public static final String PARAM_TENANT_ID = "tenantId";

	@Autowired
	private TenantRestController tenantRestController;
	@Autowired
	private TenantAuthService tenantAuthService;
	@Autowired
	private UiUtil uiUtil;
	@Autowired
	private IDeployedApiUrlService deployedApiUrlService;

	/**
	 * Adds a new tenant from form
	 *
	 * @param req        request
	 * @param resp       response
	 * @param tenantName form parameter
	 * @throws ServletException servlet exception
	 * @throws IOException      outputStream exception
	 */
	@PostMapping()
	protected void doPost(HttpServletRequest req, HttpServletResponse resp,
			@RequestParam(name = PARAM_TENANT_NAME) @NotBlank String tenantName)
			throws ServletException, IOException {
		UserAccess userAccess = UserAccessUtil.get().getUserAccess();
		tenantAuthService.authenticateTenant(userAccess, tenantName);
		resp.sendRedirect(deployedApiUrlService.getContextPath() + TENANT_BASE_PATH + "/" + tenantName + TENANT_BASE_PATH);
		doGet(req, resp);
	}

	/**
	 * UI tenant page
	 * Allows tenant switching
	 * TODO switch to controller based URI Variable paradigm ?
	 *
	 * @param req  request
	 * @param resp response
	 * @throws ServletException servlet exception
	 * @throws IOException      outputStream exception
	 */
	@GetMapping()
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		HttpSession session = req.getSession(false);
		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		String action = req.getParameter(PARAM_ACTION);
		String tenantId = req.getParameter(PARAM_TENANT_ID);

		Tenant tenant = CurrentTenantUtil.getTenant(req);
		UserAccess userAccess = UserAccessUtil.get().getUserAccess();
		if (userAccess != null && session != null) {
			List<Tenant> tenantList = tenantRestController.getTenants(req);
			for (Tenant tenantMember : tenantList) {
				if (ACTION_SWITCH.equals(action) && String.valueOf(tenantMember.getOrgId()).equals(tenantId)) {
					tenant = tenantMember;
					// session.setAttribute(SESSION_TENANT, tenant);
				}
			}
			/*
			 * print starts after potential tenant switch
			 */
			uiUtil.doHeader(out, "IIS Sandbox - Home", tenant);

			out.println("	<h1>Create or select Tenant to proceed</h1>");

			out.println("<div class=\"w3-container w3-half w3-margin-top\">");

			out.println("	<h2>Tenant List</h2>");

			out.println("<ul class=\"w3-ul w3-hoverable\">");
			for (Tenant tenantMember : tenantList) {
				if (tenantMember.equals(tenant)) {
					out.println("<li>" + tenantMember.getOrganizationName() + " (selected)</li>");
				} else {
					String link = deployedApiUrlService.getContextPath() + TenantController.TENANT_BASE_PATH + "/"
							+ tenantMember.getOrganizationName() + TenantController.TENANT_BASE_PATH;
					out.println("<li><a href=\"" + link + "\">" + tenantMember.getOrganizationName() + "</a></li>");
				}
			}
			out.println("	  </ul>");
			out.println("</div>");

			out.println("<div class=\"w3-container w3-half w3-margin-top\">");
			out.println("    <h3>Add Tenant</h3>");
			out.println("    <form method=\"POST\" action=\"tenant\" class=\"w3-container w3-card-4\">"); // TODO
																											// forbid
																											// space
																											// in
																											// input
			out.println("      <label>Tenant Name</label>");
			out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_TENANT_NAME
					+ "\" value=\"\"/>");
			out.println(
					"		<input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" value=\"Create\"/> ");
			out.println("    </form>");
			out.println("</div>");

			out.println("<div class=\"w3-container w3-margin-top\">");
			out.println("	<div class=\"w3-panel w3-yellow\"><p class=\"w3-left-align\">" +
					"Tenants are separated testing environments, One Tenant &#8792; One IIS equivalent, Different Facilities can be registered as information sources to the Tenants"
					+
					"</p></div>"); // TODO better explanation

			uiUtil.printFlavors(out, true);
			out.println("</div>");

			uiUtil.doFooter(out);
		}
		out.flush();
		out.close();
	}
}
