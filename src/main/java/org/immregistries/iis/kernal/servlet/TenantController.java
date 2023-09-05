package org.immregistries.iis.kernal.servlet;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgMaster;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;


@RestController()
@RequestMapping("/tenant")
public class TenantController {
	public static final String PARAM_ACTION = "action";

	public static final String ACTION_SWITCH = "Switch";
	public static final String PARAM_TENANT_NAME = "tenantName";
	public static final String PARAM_ORG_MASTER_ID = "orgMasterId";

	@PostMapping()
	@Transactional()
	protected void doPost(HttpServletRequest req, HttpServletResponse resp, @RequestParam(name= PARAM_TENANT_NAME, required = false) String tenantName)
		throws ServletException, IOException {
		OrgAccess orgAccess = ServletHelper.getOrgAccess();
		Session dataSession = PopServlet.getDataSession();
		try {
			if (StringUtils.isNotBlank(tenantName)) {
				OrgMaster orgMaster = ServletHelper.authenticateOrgMaster(orgAccess, tenantName, dataSession);
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
		String orgMasterId = req.getParameter(PARAM_ORG_MASTER_ID);

		Session dataSession = PopServlet.getDataSession();
		try {
			OrgMaster orgMaster = ServletHelper.getOrgMaster();
			OrgAccess orgAccess = ServletHelper.getOrgAccess();
			if (orgAccess != null && session != null) {
				Query query = dataSession.createQuery("from OrgMaster where orgAccess=?0 order by organizationName");
				query.setParameter(0, orgAccess);
				List<OrgMaster> orgMasterList = query.list();
				for (OrgMaster orgMasterMember : orgMasterList) {
					if (TenantController.ACTION_SWITCH.equals(action) && String.valueOf(orgMasterMember.getOrgId()).equals(orgMasterId)) {
						orgMaster = orgMasterMember;
						session.setAttribute("orgMaster",orgMaster);
					}
				}
				/**
				 * print starts after potential tenant switch
				 */
				HomeServlet.doHeader(out, "IIS Sandbox - Home");

				out.println("<div class=\"w3-container w3-half w3-margin-top\">");

				out.println("	<h2>Tenant List</h2>");

				out.println("<ul class=\"w3-ul w3-hoverable\">");
				for (OrgMaster orgMasterMember : orgMasterList) {
					if (orgMasterMember.equals(orgMaster)) {
						out.println("<li>" + orgMasterMember.getOrganizationName() + " (selected)</li>");
					} else  {
						String link = "tenant?" + PARAM_ACTION + "="
							+ ACTION_SWITCH + "&" + PARAM_ORG_MASTER_ID + "="
							+ orgMasterMember.getOrgId();
						out.println("<li><a href=\"" + link + "\">" + orgMasterMember.getOrganizationName() + "</a></li>");
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
				HomeServlet.doFooter(out);
			}
		} finally {
			dataSession.close();
		}
		out.flush();
		out.close();

	}
}
