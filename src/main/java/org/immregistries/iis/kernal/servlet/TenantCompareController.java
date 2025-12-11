package org.immregistries.iis.kernal.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.fhir.security.UserAccessUtil;
import org.immregistries.iis.kernal.logic.TenantCompareService;
import org.immregistries.iis.kernal.persisted.model.UserAccess;
import org.immregistries.iis.kernal.persisted.util.HibernateConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ca.uhn.fhir.context.FhirContext;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@RestController
@RequestMapping({ "/tenantCompare", TenantController.TENANT_PATH + "/tenantCompare" })
public class TenantCompareController {
	public static final String INCLUDE_GOLDEN = "includeGolden";
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public static final String TENANT_IDS = "tenantIds";

	@Autowired
	private TenantCompareService tenantCompareService;

	@Autowired
	private FhirContext fhirContext;

	@PostMapping
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String tenantIds = req.getParameter(TENANT_IDS);
		doGet(req, resp, tenantIds);
	}

	/**
	 * Currently adapted only for origins loaded in the right order,
	 * TODO add cross resource checks with ids and matching
	 *
	 * @param req
	 * @param resp
	 * @throws ServletException
	 * @throws IOException
	 */
	@GetMapping
	protected void doGet(HttpServletRequest req, HttpServletResponse resp,
			@RequestParam(name = TENANT_IDS) @NotBlank String tenantIds) throws ServletException, IOException {
		String[] tenantNames = tenantIds.split(",");

		boolean includeGolden = StringUtils.equalsIgnoreCase("true", req.getParameter(INCLUDE_GOLDEN));

		logger.info("Testing Tenant comparison");
		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		HomeController.doHeader(out, "Tenant Comparison", CurrentTenantUtil.getTenant(req));
		try {
			UserAccess userAccess = UserAccessUtil.getUserAccess();
			if (userAccess == null) {
				throw new AuthenticationCredentialsNotFoundException("");
			}
			List<IBaseParameters> diffs = tenantCompareService.compareTenants(tenantNames, userAccess,
					includeGolden);
			for (IBaseParameters diff : diffs) {
				out.println(
						"<pre>" + fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(diff)
								+ "</pre>");
			}
		} finally {
			out.flush();
			out.close();
		}
	}

}
