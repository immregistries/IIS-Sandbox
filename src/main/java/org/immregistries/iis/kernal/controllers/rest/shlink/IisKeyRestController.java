package org.immregistries.iis.kernal.controllers.rest.shlink;

import org.immregistries.iis.kernal.controllers.rest.filters.RestTenantUrlFilter;
import org.immregistries.iis.kernal.controllers.RestConstants;
import org.immregistries.iis.kernal.logic.KeyStoreService;
import org.immregistries.iis.kernal.persisted.entities.IisKey;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.immregistries.iis.kernal.security.UserAccessUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.immregistries.iis.kernal.controllers.RestConstants.Param.KEY_ID;
import static org.immregistries.iis.kernal.controllers.RestConstants.Path.Key.IIS_KEYS;
import static org.immregistries.iis.kernal.controllers.RestConstants.Path.REST_PATH;
import static org.immregistries.iis.kernal.controllers.RestConstants.PathVariable.PlaceHolder.KEY_ID_PLACEHOLDER;

@RestController
@RequestMapping()
public class IisKeyRestController {


	@Autowired
	private KeyStoreService keyStoreService;
	@Autowired
	private UserAccessUtil userAccessUtil;

	@GetMapping(REST_PATH + IIS_KEYS)
	public List<IisKey> getKeys() {
		UserAccess userAccess = userAccessUtil.getUserAccess();
		return keyStoreService.getKeys(userAccess);
	}

	@GetMapping({ REST_PATH + IIS_KEYS + KEY_ID_PLACEHOLDER + "/$getOrCreate",
			RestConstants.Path.REST_TENANT_PATH + IIS_KEYS + KEY_ID_PLACEHOLDER + "/$getOrCreate"})
	public IisKey getOrCreateKey(
			@RequestAttribute(value = RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE, required = false) Tenant tenant,
			@PathVariable(KEY_ID) String keyId) {
		UserAccess userAccess = userAccessUtil.getUserAccess();
		return keyStoreService.getIisSigningKeyOrCreate(keyId, userAccess, tenant);
	}
}
