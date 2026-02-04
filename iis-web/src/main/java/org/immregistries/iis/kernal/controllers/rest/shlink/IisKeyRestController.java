package org.immregistries.iis.kernal.controllers.rest.shlink;

import org.immregistries.iis.kernal.GlobalConstants;
import org.immregistries.iis.kernal.controllers.IisRestParam;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.persisted.entities.IisKey;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.immregistries.iis.kernal.security.UserAccessUtil;
import org.immregistries.iis.kernal.services.KeyStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.immregistries.iis.kernal.controllers.IisPathVariable.PlaceHolder.KEY_ID_PLACEHOLDER;
import static org.immregistries.iis.kernal.controllers.IisRestPath.BasePath.REST_PATH;
import static org.immregistries.iis.kernal.controllers.IisRestPath.Key.IIS_KEYS_KEY;

@RestController
@RequestMapping()
public class IisKeyRestController {


	@Autowired
	private KeyStoreService keyStoreService;
	@Autowired
	private UserAccessUtil userAccessUtil;

	@GetMapping(REST_PATH + IIS_KEYS_KEY)
	public List<IisKey> getKeys() {
		UserAccess userAccess = userAccessUtil.getUserAccess();
		return keyStoreService.getKeys(userAccess);
	}

	@GetMapping({ REST_PATH + IIS_KEYS_KEY + KEY_ID_PLACEHOLDER + "/$getOrCreate",
			IisRestPath.REST_TENANT_PATH + IIS_KEYS_KEY + KEY_ID_PLACEHOLDER + "/$getOrCreate"})
	public IisKey getOrCreateKey(
			@RequestAttribute(value = GlobalConstants.TENANT_REQUEST_ATTRIBUTE, required = false) Tenant tenant,
			@PathVariable(IisRestParam.KEY_ID) String keyId) {
		UserAccess userAccess = userAccessUtil.getUserAccess();
		return keyStoreService.getIisSigningKeyOrCreate(keyId, userAccess, tenant);
	}
}
