package org.immregistries.iis.kernal.controllers.rest;

import org.immregistries.iis.kernal.controllers.RestConstants;

import jakarta.validation.constraints.NotBlank;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.immregistries.iis.kernal.logic.TenantCompareService;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.immregistries.iis.kernal.security.UserAccessUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({
		RestConstants.Path.REST_PATH + RestConstants.Path.TENANT_COMPARE_BASE_PATH })
public class TenantCompareController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private TenantCompareService tenantCompareService;

	@PostMapping
	protected List<IBaseParameters> tenantComparePost(
			@RequestParam(name = RestConstants.Param.TENANT_IDS) @NotBlank String tenantIds,
			@RequestParam(name = RestConstants.Param.INCLUDE_GOLDEN, required = false) boolean includeGolden) {
		return tenantCompareGet(tenantIds, includeGolden);
	}

	/**
	 * Currently adapted only for origins loaded in the right order,
	 * TODO add cross resource checks with ids and matching
	 *
	 * @param tenantIds     ids of tenants to be compared
	 * @param includeGolden option of use of MDM Golden/Master Records
	 */
	@GetMapping()
	protected List<IBaseParameters> tenantCompareGet(
			@RequestParam(name = RestConstants.Param.TENANT_IDS) @NotBlank String tenantIds,
			@RequestParam(name = RestConstants.Param.INCLUDE_GOLDEN, required = false) boolean includeGolden) {
		String[] tenantNames = tenantIds.split(",");
		logger.info("Testing Tenant comparison for ids {} with golden={}", tenantNames, includeGolden);
		UserAccess userAccess = UserAccessUtil.get().getUserAccess();
		if (userAccess == null) {
			throw new AuthenticationCredentialsNotFoundException("");
		}
		return tenantCompareService.compareTenants(tenantNames, userAccess,
				includeGolden);
	}

}
