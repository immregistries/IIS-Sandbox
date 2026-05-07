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
import java.util.Map;

@RestController
@RequestMapping(IisRestPath.SH_LINKS_STORED_MANIFEST_FULL_PATH)
public class ShLinkManifestRestController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private ShLinkManifestGenerator shLinkManifestGenerator;
	@Autowired
	private ShLinkManifestStoreService shlinkManifestStoreService;
	@Autowired
	private TenantAuthService tenantAuthService;
	@Autowired
	private RequestTenantUtil requestTenantUtil;

	@GetMapping(IisPathVariable.PlaceHolder.MANIFEST_ID_PLACEHOLDER)
	public ShLinkManifest getManifest(@PathVariable(IisPathVariable.Key.MANIFEST_ID) String manifestId,
	                                  @RequestAttribute(value = IisRequestAttribute.TENANT_REQUEST_ATTRIBUTE, required = false) Tenant tenant
	) {
		ShLinkManifest shLinkManifest = shlinkManifestStoreService.readManifest(manifestId);
		Tenant manifestTenant = shLinkManifest.getTenant();
		String manifestTenantName = manifestTenant.getOrganizationName();
		if (shLinkManifest.getPasswordProtected()) {
			/*
			 * If no passcode was specified at shlink creation, using iis sandbox authorization
			 */
			if (tenant == null || !Strings.CS.equals(manifestTenantName, tenant.getOrganizationName())) {
				throw new AuthenticationCredentialsNotFoundException("Invalid passcode");
			}
		}
		return shLinkManifest;
	}

	@PostMapping(IisPathVariable.PlaceHolder.MANIFEST_ID_PLACEHOLDER)
	protected ShLinkManifest readShLinkManifest(
			@PathVariable(IisPathVariable.Key.MANIFEST_ID) String manifestId,
			@RequestAttribute(value = IisRequestAttribute.TENANT_REQUEST_ATTRIBUTE, required = false) Tenant tenant,
			@RequestBody Map<String, String> payload) {
		String recipient = payload.get(IisRestParam.ShLink.RECIPIENT);
		String passcode = payload.get(IisRestParam.ShLink.PASSCODE);
		String maxLen = payload.get(IisRestParam.ShLink.EMBEDDED_LENGTH_MAX);
		ShLinkManifest shLinkManifest = shlinkManifestStoreService.readManifest(manifestId);
		Tenant manifestTenant = shLinkManifest.getTenant();
		String manifestTenantName = manifestTenant.getOrganizationName();
		if (shLinkManifest.getPasswordProtected()) {
			if (StringUtils.isNotBlank(shLinkManifest.getPasscode())) {
				if (!Strings.CS.equals(shLinkManifest.getPasscode(), passcode)) {
					throw new AuthenticationCredentialsNotFoundException("Invalid passcode");
				}
			}
			/*
			 * If no Manifest passcode was specified at shlink creation, using iis sandbox authorization
			 */
			if (StringUtils.isNotBlank(passcode)) {
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
