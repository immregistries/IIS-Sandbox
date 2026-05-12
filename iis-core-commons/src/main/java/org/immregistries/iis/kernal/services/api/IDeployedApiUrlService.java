package org.immregistries.iis.kernal.services.api;

import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.jetbrains.annotations.NotNull;

public interface IDeployedApiUrlService {
	String getContextPath();
	@NotNull String fhirServerBasePath(Tenant tenant);

}
