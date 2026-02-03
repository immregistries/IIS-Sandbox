package org.immregistries.iis.kernal;

import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.services.api.IDeployedApiUrlService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.immregistries.iis.kernal.Application.FHIR_SERVER_PATH_EXTENSION;

@Service
public class DeployedUrlServiceImpl implements IDeployedApiUrlService {
	@Autowired
	private IisConfigService iisConfigService;

	public @NotNull String fhirServerBasePath(Tenant tenant) {
		return Application.IIS_PATH_BASE + FHIR_SERVER_PATH_EXTENSION + "/" + tenant.getOrganizationName();
	}
}
