package org.immregistries.iis.kernal.services.api;

import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.springframework.web.util.UriComponentsBuilder;

public interface IWellKnownKeyApiService {

	String generateKeyIssuerUrl(Tenant tenant, UriComponentsBuilder uriBuilder);

}
