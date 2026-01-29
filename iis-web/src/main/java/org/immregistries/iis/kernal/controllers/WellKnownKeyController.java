package org.immregistries.iis.kernal.controllers;

import com.nimbusds.jose.jwk.JWK;
import org.immregistries.iis.kernal.controllers.servlet.TenantController;
import org.immregistries.iis.kernal.services.KeyStoreService;
import org.immregistries.iis.kernal.persisted.entities.IisKey;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.immregistries.iis.kernal.security.UserAccessUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

import static org.immregistries.iis.kernal.controllers.WellKnownKeyController.WELL_KNOWN_PATH_SUFFIX;

@RestController
@RequestMapping({ TenantController.TENANT_PATH + WELL_KNOWN_PATH_SUFFIX, WELL_KNOWN_PATH_SUFFIX })
public class WellKnownKeyController {

	public static final String WELL_KNOWN_PATH_SUFFIX = "/.well-known/jwks.json";

	@Autowired
	KeyStoreService keyStoreService;

	@GetMapping()
	public List<JWK> doGetWellKnown() {
		UserAccess userAccess = UserAccessUtil.get().getUserAccess();
		List<IisKey> iisKeys = keyStoreService.getKeys(userAccess);
		return iisKeys.stream().map(iisKey -> iisKey.jwk().toPublicJWK()).collect(Collectors.toList());
	}

}
