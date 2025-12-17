package org.immregistries.iis.kernal.controllers.rest;

import jakarta.servlet.ServletException;
import jakarta.validation.constraints.NotBlank;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.immregistries.iis.kernal.fhir.security.UserAccessUtil;
import org.immregistries.iis.kernal.logic.TenantCompareService;
import org.immregistries.iis.kernal.persisted.model.UserAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

import static org.immregistries.iis.kernal.controllers.rest.TenantCompareController.TENANT_COMPARE_BASE_PATH;

@RestController
@RequestMapping({
		RestUrlUtil.REST + TENANT_COMPARE_BASE_PATH })
public class TenantCompareController {
	public static final String TENANT_COMPARE_BASE_PATH = "/tenantCompare";

	public static final String INCLUDE_GOLDEN = "includeGolden";
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public static final String TENANT_IDS = "tenantIds";

	@Autowired
	private TenantCompareService tenantCompareService;

	@PostMapping
	protected List<IBaseParameters> tenantComparePost(@RequestParam(name = TENANT_IDS) @NotBlank String tenantIds,
			@RequestParam(name = INCLUDE_GOLDEN, required = false) boolean includeGolden) {
		return tenantCompareGet(tenantIds, includeGolden);
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
	@GetMapping()
	protected List<IBaseParameters> tenantCompareGet(
			@RequestParam(name = TENANT_IDS) @NotBlank String tenantIds,
			@RequestParam(name = INCLUDE_GOLDEN, required = false) boolean includeGolden) {
		String[] tenantNames = tenantIds.split(",");
		logger.info("Testing Tenant comparison for ids {} with golden={}", tenantNames, includeGolden);
		UserAccess userAccess = UserAccessUtil.get().getUserAccess();
		if (userAccess == null) {
			throw new AuthenticationCredentialsNotFoundException("");
		}
		List<IBaseParameters> diffs = tenantCompareService.compareTenants(tenantNames, userAccess,
				includeGolden);
		return diffs;
	}

}
