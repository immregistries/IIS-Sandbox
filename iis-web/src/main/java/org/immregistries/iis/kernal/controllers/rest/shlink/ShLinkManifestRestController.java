package org.immregistries.iis.kernal.controllers.rest.shlink;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.controllers.IisPathVariable;
import org.immregistries.iis.kernal.controllers.IisRestParam;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.logic.shlink.ShLinkManifestGenerator;
import org.immregistries.iis.kernal.logic.shlink.ShLinkManifestStoreService;
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
	public final static String SHLINKS_CONTROLLER_REST_BASE_URL = IisRestPath.BasePath.REST_PATH + "/link";

	@Autowired
	private ShLinkManifestGenerator shLinkManifestGenerator;
	@Autowired
	private ShLinkManifestStoreService shlinkManifestStoreService;
	@Autowired
	private TenantAuthService tenantAuthService;

	@GetMapping(IisPathVariable.PlaceHolder.MANIFEST_ID_PLACEHOLDER)
	public ShLinkManifest getManifest(@PathVariable(IisPathVariable.Key.MANIFEST_ID) String manifestId) {
		return shlinkManifestStoreService.readManifest(manifestId);
	}

	@PostMapping(IisPathVariable.PlaceHolder.MANIFEST_ID_PLACEHOLDER)
	protected ShLinkManifest readShLinkManifest(
			@PathVariable(IisPathVariable.Key.MANIFEST_ID) String manifestId,
			@PathVariable(IisPathVariable.Key.TENANT_ID) int tenantId,
			@RequestParam(value = IisRestParam.ShLink.RECIPIENT, required = false) String recipient,
			@RequestParam(value = IisRestParam.ShLink.PASSCODE, required = false) String passcode,
			@RequestParam(value = IisRestParam.ShLink.EMBEDDED_LENGTH_MAX, required = false) String embeddedLengthMax) {
		Tenant tenant = null;
		if (StringUtils.isNoneBlank(passcode)) {
			tenant = tenantAuthService.authenticateTenantNoUsername(tenantId, passcode);
		}
		if (tenant == null) {
			throw new AuthenticationCredentialsNotFoundException("Invalid passcode");
		}
		return shlinkManifestStoreService.readManifest(manifestId);
	}

	@GetMapping()
	public List<ShLinkManifest> getManifestAll() {
		return shlinkManifestStoreService.getAllManifests();
	}

	@GetMapping("/$generate")
	public ShLinkManifest genManifest(HttpServletRequest req) {
		ShLinkManifest shLinkManifest = shLinkManifestGenerator.generateManifest(CurrentTenantUtil.getTenant(req));
		return shlinkManifestStoreService.saveManifest(shLinkManifest);
	}

}
