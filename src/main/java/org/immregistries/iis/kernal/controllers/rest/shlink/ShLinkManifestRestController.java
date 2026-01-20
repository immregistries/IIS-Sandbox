package org.immregistries.iis.kernal.controllers.rest.shlink;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.controllers.rest.RestUrlUtil;
import org.immregistries.iis.kernal.logic.shlink.ShLinkManifestGenerator;
import org.immregistries.iis.kernal.logic.shlink.ShLinkManifestService;
import org.immregistries.iis.kernal.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.security.TenantAuthService;
import org.immregistries.iis.kernal.persisted.model.ShLinkManifest;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.immregistries.iis.kernal.controllers.servlet.TenantController.PARAM_TENANT_ID;

@RestController
@RequestMapping(ShLinkManifestRestController.SHLINKS_CONTROLLER_REST_BASE_URL)
public class ShLinkManifestRestController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	public static final String MANIFEST_ID = "manifestId";
	public static final String MANIFEST_ID_PLACEHOLDER = "/{" + MANIFEST_ID + "}";
	public static final String RECIPIENT_PARAM = "recipient";
	public static final String PASSCODE_PARAM = "passcode";
	public static final String EMBEDDED_LENGTH_MAX_PARAM = "embeddedLengthMax";
	public final static String SHLINKS_CONTROLLER_REST_BASE_URL = RestUrlUtil.REST_PATH +  "/link";

	@Autowired
	private ShLinkManifestGenerator shLinkManifestGenerator;
	@Autowired
	private ShLinkManifestService shlinkManifestService;
	@Autowired
	private TenantAuthService tenantAuthService;

	@GetMapping(MANIFEST_ID_PLACEHOLDER)
	public ShLinkManifest getManifest(@PathVariable(MANIFEST_ID) String manifestId) {
		return shlinkManifestService.readManifest(manifestId);
	}

	@PostMapping(MANIFEST_ID_PLACEHOLDER)
	protected ShLinkManifest readShLinkManifest(
		@PathVariable(MANIFEST_ID) String manifestId,
		@PathVariable(PARAM_TENANT_ID) int tenantId,
		@RequestParam(value = RECIPIENT_PARAM, required = false) String recipient,
		@RequestParam(value = PASSCODE_PARAM, required = false) String passcode,
		@RequestParam(value = EMBEDDED_LENGTH_MAX_PARAM, required = false) String embeddedLengthMax) {
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
