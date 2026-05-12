package org.immregistries.iis.kernal.service;

import org.immregistries.iis.kernal.controllers.servlet.TenantController;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.services.api.IWellKnownKeyApiService;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.MalformedURLException;
import java.util.Map;

@Service
public class WellKnownKeyApiService implements IWellKnownKeyApiService {

	public String generateKeyIssuerUrl(Tenant tenant, UriComponentsBuilder uriBuilder) {
		UriComponentsBuilder uriComponentsBuilder = uriBuilder;
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
