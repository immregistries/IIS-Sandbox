package org.immregistries.iis.kernal.controllers.rest.shlink;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.controllers.rest.RestUrlUtil;
import org.immregistries.iis.kernal.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.security.TenantUtil;
import org.immregistries.iis.kernal.logic.shlink.ShLinkUtilService;
import org.immregistries.iis.kernal.persisted.model.ShLinkManifest;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.persisted.repository.ShlinkManifestRepository;
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
	public final static String SHLINKS_CONTROLLER_REST_BASE_URL = RestUrlUtil.REST +  "/link";


	@Autowired
	ShLinkUtilService shLinkUtilService;
	@Autowired
	private ShlinkManifestRepository shlinkManifestRepository;
	@Autowired
	TenantUtil tenantUtil;

	@GetMapping("/{manifestId}")
	public ShLinkManifest getManifest(@PathVariable("manifestId") String manifestId) {
		return shLinkUtilService.readShLinkManifest(manifestId);
	}

	@PostMapping("/{manifestId}")
	protected ShLinkManifest readShLinkManifest(
		@PathVariable("manifestId") String manifestId,
		@PathVariable(PARAM_TENANT_ID) int tenantId,
		@RequestParam(value = "recipient", required = false) String recipient,
		@RequestParam(value = "passcode", required = false) String passcode,
		@RequestParam(value = "embeddedLengthMax", required = false) String embeddedLengthMax) {
		Tenant tenant = null;
		if (StringUtils.isNoneBlank(passcode)) {
			tenant = tenantUtil.authenticateTenantNoUsername(tenantId, passcode);
		}
		if (tenant == null) {
			throw new AuthenticationCredentialsNotFoundException("Invalid passcode");
		}
		ShLinkManifest shLinkManifest = shLinkUtilService.readShLinkManifest(manifestId);
		return shLinkManifest;
	}

	@GetMapping()
	public List<ShLinkManifest> getManifestAll() {
		return shlinkManifestRepository.findAll();
	}

	@GetMapping("/$generate")
	public ShLinkManifest genManifest(HttpServletRequest req) {
		ShLinkManifest shLinkManifest = shLinkUtilService.generateManifest(CurrentTenantUtil.getTenant(req));
		return shLinkUtilService.saveManifest(shLinkManifest);
	}

}
