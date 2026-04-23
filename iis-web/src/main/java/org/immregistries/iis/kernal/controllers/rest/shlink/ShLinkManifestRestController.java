package org.immregistries.iis.kernal.controllers.rest.shlink;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.immregistries.iis.kernal.IisRequestAttribute;
import org.immregistries.iis.kernal.controllers.IisPathVariable;
import org.immregistries.iis.kernal.controllers.IisRestParam;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.logic.shlink.ShLinkManifestGenerator;
import org.immregistries.iis.kernal.logic.shlink.ShLinkManifestStoreService;
import org.immregistries.iis.kernal.persisted.entities.ShLinkManifest;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.security.RequestTenantUtil;
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
	@Autowired
	private RequestTenantUtil requestTenantUtil;

	@GetMapping(IisPathVariable.PlaceHolder.MANIFEST_ID_PLACEHOLDER)
	public ShLinkManifest getManifest(@PathVariable(IisPathVariable.Key.MANIFEST_ID) String manifestId) {
		return shlinkManifestStoreService.readManifest(manifestId);
	}

	@PostMapping(IisPathVariable.PlaceHolder.MANIFEST_ID_PLACEHOLDER)
	protected ShLinkManifest readShLinkManifest(
			@PathVariable(IisPathVariable.Key.MANIFEST_ID) String manifestId,
			@RequestAttribute(value = IisRequestAttribute.TENANT_REQUEST_ATTRIBUTE, required = false) Tenant tenant,
			@RequestParam(value = IisRestParam.ShLink.RECIPIENT, required = false) String recipient,
			@RequestParam(value = IisRestParam.ShLink.PASSCODE, required = false) String passcode,
			@RequestParam(value = IisRestParam.ShLink.EMBEDDED_LENGTH_MAX, required = false) String embeddedLengthMax) {
		ShLinkManifest shLinkManifest = shlinkManifestStoreService.readManifest(manifestId);
		Tenant manifestTenant = shLinkManifest.getTenant();
		String manifestTenantName = manifestTenant.getOrganizationName();
		if (shLinkManifest.getPasswordProtected()) {
			if (StringUtils.isNoneBlank(passcode)) {
				tenant = tenantAuthService.authenticateTenantNoUsername(manifestTenantName, passcode);
			}
			if (tenant == null || !Strings.CS.equals(manifestTenantName, tenant.getOrganizationName())) {
				throw new AuthenticationCredentialsNotFoundException("Invalid passcode");
			}
		}
		return shLinkManifest;
	}

	@GetMapping()
	public List<ShLinkManifest> getManifestAll() {
		return shlinkManifestStoreService.getAllManifests();
	}

	@GetMapping("/$generate")
	public ShLinkManifest genManifest(HttpServletRequest req) {
		ShLinkManifest shLinkManifest = shLinkManifestGenerator.generateManifest(requestTenantUtil.extractTenant(req));
		return shlinkManifestStoreService.saveManifest(shLinkManifest);
	}

}
