package org.immregistries.iis.kernal.service;

import org.immregistries.iis.kernal.Application;
import org.immregistries.iis.kernal.logic.api.IApiUrlService;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
public class ApiUrlServiceImpl implements IApiUrlService {

	public @NotNull String fhirServerBasePath(Tenant tenant) {
		return Application.fhirServerBasePath(tenant);
	}

}
