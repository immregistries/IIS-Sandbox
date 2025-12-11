package org.immregistries.iis.kernal.servlet.shlink;

import com.nimbusds.jose.jwk.JWK;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.fhir.security.UserAccessUtil;
import org.immregistries.iis.kernal.logic.KeyStoreService;
import org.immregistries.iis.kernal.persisted.model.IisKey;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.persisted.model.UserAccess;
import org.immregistries.iis.kernal.servlet.TenantController;
import org.immregistries.iis.kernal.servlet.WellKnownKeyController;
import org.immregistries.iis.kernal.servlet.util.UiUtil;
import org.immregistries.iis.kernal.servlet.util.UrlTenantUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

import static org.immregistries.iis.kernal.servlet.shlink.IisKeyController.IIS_KEY_BASE_PATH;

@RestController
@RequestMapping({ IIS_KEY_BASE_PATH, TenantController.TENANT_PATH + IIS_KEY_BASE_PATH })
public class IisKeyController {
	public static final String IIS_KEY_BASE_PATH = "/iisKey";

	@Autowired
	KeyStoreService keyStoreService;

	@PostMapping
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doGet(req, resp);
	}

	// @GetMapping("/.well-known/jwks.json")
	@GetMapping("/.well-known/jwks.json")
	/**
	 * TODO link properly
	 *
	 * @param req
	 * @param resp
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	protected List<JWK> doGetWellKnown(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		UserAccess userAccess = UserAccessUtil.get().getUserAccess();
		resp.setContentType("application/json");
		// Tenant tenant = CurrentTenantUtil.getTenantRedirectIfNone(req, resp);
		List<IisKey> iisKeys = keyStoreService.getKeys(userAccess);
		return iisKeys.stream().map(iisKey -> iisKey.jwk().toPublicJWK()).collect(Collectors.toList());
	}

	@GetMapping
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		UserAccess userAccess = UserAccessUtil.get().getUserAccess();
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
			List<IisKey> iisKeys = keyStoreService.getKeys(userAccess);
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
