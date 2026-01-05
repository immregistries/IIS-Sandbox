package org.immregistries.iis.kernal.controllers.rest.shlink;

import org.immregistries.iis.kernal.controllers.filters.RestTenantUrlFilter;
import org.immregistries.iis.kernal.security.UserAccessUtil;
import org.immregistries.iis.kernal.logic.KeyStoreService;
import org.immregistries.iis.kernal.persisted.model.IisKey;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.persisted.model.UserAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping()
public class IisKeyRestController {

	@Autowired
	KeyStoreService keyStoreService;
	@Autowired
	UserAccessUtil userAccessUtil;


	@GetMapping("/rest/iisKeys")
	public List<IisKey> getKeys() {
		UserAccess userAccess = userAccessUtil.getUserAccess();
		return keyStoreService.getKeys(userAccess);
	}

	@GetMapping({"/rest/iisKeys/{id}/$getOrCreate", "/rest/tenant/{tenantId}/iisKeys/{id}/$getOrCreate"})
	public IisKey getOrCreateKey(@RequestAttribute(value = RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE, required = false) Tenant tenant,
										  @PathVariable("id") String keyId) {
		UserAccess userAccess = userAccessUtil.getUserAccess();
		return keyStoreService.getIisSigningKeyOrCreate(keyId, userAccess, tenant);
	}
}
