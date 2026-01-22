package org.immregistries.iis.kernal.controllers.rest.shlink;

import org.immregistries.iis.kernal.controllers.filters.RestTenantUrlFilter;
import org.immregistries.iis.kernal.controllers.rest.RestUrlUtil;
import org.immregistries.iis.kernal.logic.KeyStoreService;
import org.immregistries.iis.kernal.persisted.entities.IisKey;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.immregistries.iis.kernal.security.UserAccessUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping()
public class IisKeyRestController {

	public static final String IIS_KEYS = "/iisKeys";
	public static final String KEY_ID = "keyId";

	public static final String KEY_ID_PLACEHOLDER = "/{" + KEY_ID + "}";
	@Autowired
	KeyStoreService keyStoreService;
	@Autowired
	UserAccessUtil userAccessUtil;

	@GetMapping(RestUrlUtil.REST_PATH + IIS_KEYS)
	public List<IisKey> getKeys() {
		UserAccess userAccess = userAccessUtil.getUserAccess();
		return keyStoreService.getKeys(userAccess);
	}

	@GetMapping({ RestUrlUtil.REST_PATH + IIS_KEYS + KEY_ID_PLACEHOLDER + "/$getOrCreate",
			RestUrlUtil.REST_TENANT_PATH + IIS_KEYS + KEY_ID_PLACEHOLDER + "/$getOrCreate"})
	public IisKey getOrCreateKey(
			@RequestAttribute(value = RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE, required = false) Tenant tenant,
			@PathVariable(KEY_ID) String keyId) {
		UserAccess userAccess = userAccessUtil.getUserAccess();
		return keyStoreService.getIisSigningKeyOrCreate(keyId, userAccess, tenant);
	}
}
