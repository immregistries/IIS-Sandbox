package org.immregistries.iis.kernal.logic.api;

import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.jetbrains.annotations.NotNull;

public interface IApiUrlService {
	@NotNull String fhirServerBasePath(Tenant tenant);

}
