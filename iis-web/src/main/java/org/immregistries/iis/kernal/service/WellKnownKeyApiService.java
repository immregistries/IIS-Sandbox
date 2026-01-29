package org.immregistries.iis.kernal.service;

import jakarta.servlet.http.HttpServletRequest;
import org.immregistries.iis.kernal.controllers.servlet.TenantController;
import org.immregistries.iis.kernal.services.api.IWellKnownKeyApiService;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.MalformedURLException;
import java.util.Map;

@Service
public class WellKnownKeyApiService  implements IWellKnownKeyApiService {

	public String getKeyIssuerUrl(HttpServletRequest request, Tenant tenant) {
		UriComponentsBuilder uriComponentsBuilder = ServletUriComponentsBuilder.fromRequest(request);
		uriComponentsBuilder.replacePath(TenantController.TENANT_PATH);
		uriComponentsBuilder.build(Map.of(TenantController.TENANT_NAME, tenant.getOrganizationName()));
		String issuerUrl = null;
		try {
			issuerUrl = uriComponentsBuilder.build().toUri().toURL().toString();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		return issuerUrl;
	}

}
