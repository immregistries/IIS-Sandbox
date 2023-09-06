package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.model.UserAccess;
import org.immregistries.iis.kernal.model.Tenant;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import static org.immregistries.iis.kernal.fhir.interceptors.PartitionCreationInterceptor.PARTITION_NAME_SEPARATOR;
import static org.immregistries.iis.kernal.fhir.security.ServletHelper.SESSION_TENANT;

@RestController()
@RequestMapping("/tenant")
public class TenantController {
	public static final String PARAM_ACTION = "action";

	public static final String ACTION_SWITCH = "Switch";
	public static final String PARAM_TENANT_NAME = "tenantName";
	public static final String PARAM_TENANT_ID = "tenantId";

	@PostMapping()
//	@Transactional()
	protected void doPost(HttpServletRequest req, HttpServletResponse resp, @RequestParam(name= PARAM_TENANT_NAME, required = false) String tenantName)
		throws ServletException, IOException {
		UserAccess userAccess = ServletHelper.getUserAccess();
		Session dataSession = PopServlet.getDataSession();
		try {
			if (StringUtils.isNotBlank(tenantName)) {
				if (tenantName.indexOf(PARTITION_NAME_SEPARATOR) > 0) {
					throw new InvalidRequestException("Invalid tenant name , should not use -");
				}
				Tenant tenant = ServletHelper.authenticateTenant(userAccess, tenantName, dataSession);
			}
		} finally {
			dataSession.close();
		}
		doGet(req, resp);
	}


	@GetMapping()
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		HttpSession session = req.getSession(false);
		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		String action = req.getParameter(PARAM_ACTION);
		String tenantId = req.getParameter(PARAM_TENANT_ID);

		Session dataSession = PopServlet.getDataSession();
		try {
			Tenant tenant = ServletHelper.getTenant();
			UserAccess userAccess = ServletHelper.getUserAccess();
			if (userAccess != null && session != null) {
				Query query = dataSession.createQuery("from Tenant where userAccess=?0 order by organizationName");
				query.setParameter(0, userAccess);
				List<Tenant> tenantList = query.list();
				for (Tenant tenantMember : tenantList) {
					if (TenantController.ACTION_SWITCH.equals(action) && String.valueOf(tenantMember.getOrgId()).equals(tenantId)) {
						tenant = tenantMember;
						session.setAttribute(SESSION_TENANT, tenant);
					}
				}
				/**
				 * print starts after potential tenant switch
				 */
				HomeServlet.doHeader(out, "IIS Sandbox - Home");

				out.println("<div class=\"w3-container w3-half w3-margin-top\">");

				out.println("	<h2>Tenant List</h2>");

				out.println("<ul class=\"w3-ul w3-hoverable\">");
				for (Tenant tenantMember : tenantList) {
					if (tenantMember.equals(tenant)) {
						out.println("<li>" + tenantMember.getOrganizationName() + " (selected)</li>");
					} else  {
						String link = "tenant?" + PARAM_ACTION + "="
							+ ACTION_SWITCH + "&" + PARAM_TENANT_ID + "="
							+ tenantMember.getOrgId();
						out.println("<li><a href=\"" + link + "\">" + tenantMember.getOrganizationName() + "</a></li>");
					}
				}
				out.println("	  </ul>");
				out.println("</div>");


				out.println("<div class=\"w3-container w3-half w3-margin-top\">");
				out.println("    <h3>Add Tenant</h3>");
				out.println("    <form method=\"POST\" action=\"tenant\" class=\"w3-container w3-card-4\">");
				out.println("      <label>Tenant Name</label>");
				out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_TENANT_NAME + "\" value=\"\"/>");
				out.println("		<input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" value=\"Add\"/> ");
				out.println("    </form>");
				out.println("</div>");

				out.println("<div class=\"w3-container w3-margin-top\">");
				out.println("	<div class=\"w3-panel w3-yellow\"><p class=\"w3-left-align\">" +
					"Tenants are separated testing environments, One Tenant &#8792; One IIS equivalent, Different Facilities can be registered as information sources to the Tenants" +
					"</p></div>"); // TODO better explanation

				out.println("</div>");


				HomeServlet.doFooter(out);
			}
		} finally {
			dataSession.close();
		}
		out.flush();
		out.close();

	}
}
