package org.immregistries.iis.kernal.controllers.servlet.shlink;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.controllers.WellKnownKeyController;
import org.immregistries.iis.kernal.controllers.rest.shlink.IisKeyRestController;
import org.immregistries.iis.kernal.controllers.servlet.TenantController;
import org.immregistries.iis.kernal.controllers.servlet.util.UiUtil;
import org.immregistries.iis.kernal.controllers.servlet.util.UrlTenantUtil;
import org.immregistries.iis.kernal.persisted.entities.IisKey;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.security.CurrentTenantUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@RestController
@RequestMapping({IisRestPath.BasePath.IIS_KEYS_PATH, TenantController.TENANT_PATH + IisRestPath.BasePath.IIS_KEYS_PATH})
public class IisKeyController {

	@Autowired
	private IisKeyRestController iisKeyRestController;

	@PostMapping
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doGet(req, resp);
	}

	@GetMapping
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		try {
			Tenant tenant = CurrentTenantUtil.getTenantRedirectIfNone(req, resp);
			UiUtil.doHeader(out, "IIS Sandbox Keystore", CurrentTenantUtil.getTenant());
			out.println("    <div class=\"w3-container w3-half w3-margin-top\">");
			out.println("    <h2>Facility: " + tenant.getOrganizationName() + "</h2>");
			out.println("    <h3>Keys used for signing Smart Health Cards (generated for the user)</h3>");
			out.println("    </div>");

			out.println("    <div class=\"w3-container\">");
			List<IisKey> iisKeys = iisKeyRestController.getKeys();
			printIisKeys(out, iisKeys, tenant);
			out.println("    </div>");
		} catch (Exception e) {
			System.err.println("Unable to render page: " + e.getMessage());
			e.printStackTrace(System.err);
		}
		UiUtil.doFooter(out);
		out.flush();
		out.close();

	}

	protected static void printIisKeys(PrintWriter out, List<IisKey> iisKeys, Tenant tenant) {
		out.println("<a href=\""
				+ UrlTenantUtil.tenantifyPathWithContextPath(tenant, WellKnownKeyController.WELL_KNOWN_PATH_SUFFIX)
				+ "\">well-known</a>");

		if (iisKeys.isEmpty()) {
			out.println("<em>No Key found</em>");
		} else {
			int count = 0;
			for (IisKey iisKey : iisKeys) {
				count++;
				printIisKey(out, iisKey);
			}
		}
	}

	public static void printIisKey(PrintWriter out, IisKey iisKey) {
		out.println("<h4>Key id : " + iisKey.getKeyId() + "</h4>");
		out.println("<textarea textarea name=\"shlink\" readonly style=\"width: 100%; height: 3em;\" >" +
				iisKey.jwk().toPublicJWK().toJSONString() + "</textarea>");
	}

}
