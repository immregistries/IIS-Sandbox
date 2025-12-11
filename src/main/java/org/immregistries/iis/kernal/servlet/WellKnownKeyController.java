package org.immregistries.iis.kernal.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.immregistries.iis.kernal.fhir.security.UserAccessUtil;
import org.immregistries.iis.kernal.logic.KeyStoreService;
import org.immregistries.iis.kernal.persisted.model.IisKey;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.persisted.model.UserAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.immregistries.iis.kernal.servlet.WellKnownKeyController.WELL_KNOWN_PATH_SUFFIX;

@RestController
@RequestMapping({ TenantController.TENANT_PATH + WELL_KNOWN_PATH_SUFFIX, WELL_KNOWN_PATH_SUFFIX })
public class WellKnownKeyController {

	public static final String WELL_KNOWN_PATH_SUFFIX = "/.well-known/jwks.json";

	@Autowired
	KeyStoreService keyStoreService;

	@GetMapping()
	protected Set<?> doGetWellKnown(HttpServletRequest req, HttpServletResponse resp,
			@PathVariable(name = TenantController.PATH_VARIABLE_TENANT_NAME, required = false) String tenantName)
			throws ServletException, IOException {
		UserAccess userAccess = UserAccessUtil.get().getUserAccess();
		resp.setContentType("application/json");
		// Tenant tenant = CurrentTenantUtil.getTenantRedirectIfNone(req, resp,
		// dataSession);
		List<IisKey> iisKeys = keyStoreService.getKeys(userAccess);
		return iisKeys.stream().map(iisKey -> iisKey.jwk().toPublicJWK().toJSONObject())
				.collect(Collectors.toSet());
	}

	public static String getKeyIssuerUrl(HttpServletRequest request, Tenant tenant) {
		UriComponentsBuilder uriComponentsBuilder = ServletUriComponentsBuilder.fromRequest(request);
		uriComponentsBuilder.replacePath(TenantController.TENANT_PATH);
		uriComponentsBuilder.build(Map.of(TenantController.PATH_VARIABLE_TENANT_NAME, tenant.getOrganizationName()));
		String issuerUrl = null;
		try {
			issuerUrl = uriComponentsBuilder.build().toUri().toURL().toString();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		return issuerUrl;
	}

}
