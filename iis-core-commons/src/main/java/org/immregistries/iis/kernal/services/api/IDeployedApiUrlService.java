package org.immregistries.iis.kernal.services.api;

import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.jetbrains.annotations.NotNull;

public interface IDeployedApiUrlService {
	@NotNull String fhirServerBasePath(Tenant tenant);

}
