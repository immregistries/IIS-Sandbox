package org.immregistries.iis.kernal;

import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import static org.immregistries.iis.kernal.Application.FHIR_PATH_EXTENSION;

@Service
public class ApiUrlService {

	public @NotNull String fhirServerBasePath(Tenant tenant) {
		return Application.IIS_PATH_BASE + FHIR_PATH_EXTENSION + "/" + tenant.getOrganizationName();
	}
}