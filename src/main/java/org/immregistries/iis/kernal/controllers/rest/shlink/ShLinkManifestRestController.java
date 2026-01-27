package org.immregistries.iis.kernal.controllers.rest.shlink;

import org.immregistries.iis.kernal.controllers.RestConstants;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.logic.shlink.ShLinkManifestGenerator;
import org.immregistries.iis.kernal.logic.shlink.ShLinkManifestService;
import org.immregistries.iis.kernal.persisted.entities.ShLinkManifest;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.security.TenantAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ShLinkManifestRestController.SHLINKS_CONTROLLER_REST_BASE_URL)
public class ShLinkManifestRestController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	public final static String SHLINKS_CONTROLLER_REST_BASE_URL = RestConstants.Path.REST_PATH + "/link";

	@Autowired
	private ShLinkManifestGenerator shLinkManifestGenerator;
	@Autowired
	private ShLinkManifestService shlinkManifestService;
	@Autowired
	private TenantAuthService tenantAuthService;

	@GetMapping(RestConstants.PathVariable.PlaceHolder.MANIFEST_ID_PLACEHOLDER)
	public ShLinkManifest getManifest(@PathVariable(RestConstants.PathVariable.Key.MANIFEST_ID) String manifestId) {
		return shlinkManifestService.readManifest(manifestId);
	}

	@PostMapping(RestConstants.PathVariable.PlaceHolder.MANIFEST_ID_PLACEHOLDER)
	protected ShLinkManifest readShLinkManifest(
			@PathVariable(RestConstants.PathVariable.Key.MANIFEST_ID) String manifestId,
			@PathVariable(RestConstants.PathVariable.Key.TENANT_ID) int tenantId,
			@RequestParam(value = RestConstants.Param.RECIPIENT, required = false) String recipient,
			@RequestParam(value = RestConstants.Param.PASSCODE, required = false) String passcode,
			@RequestParam(value = RestConstants.Param.EMBEDDED_LENGTH_MAX, required = false) String embeddedLengthMax) {
		Tenant tenant = null;
		if (StringUtils.isNoneBlank(passcode)) {
			tenant = tenantAuthService.authenticateTenantNoUsername(tenantId, passcode);
		}
		if (tenant == null) {
			throw new AuthenticationCredentialsNotFoundException("Invalid passcode");
		}
		return shlinkManifestService.readManifest(manifestId);
	}

	@GetMapping()
	public List<ShLinkManifest> getManifestAll() {
		return shlinkManifestService.getAllManifests();
	}

	@GetMapping("/$generate")
	public ShLinkManifest genManifest(HttpServletRequest req) {
		ShLinkManifest shLinkManifest = shLinkManifestGenerator.generateManifest(CurrentTenantUtil.getTenant(req));
		return shlinkManifestService.saveManifest(shLinkManifest);
	}

}
