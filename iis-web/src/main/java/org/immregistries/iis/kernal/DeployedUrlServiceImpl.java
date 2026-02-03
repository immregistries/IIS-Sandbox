package org.immregistries.iis.kernal;

import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.services.api.IDeployedApiUrlService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static org.immregistries.iis.kernal.HapiFhirServerRegistrationConfig.FHIR_SERVER_PATH_EXTENSION;

@Service
public class DeployedUrlServiceImpl implements IDeployedApiUrlService {
	private static final String $_SERVER_SERVLET_CONTEXT_PATH_IIS = "${server.servlet.contextPath:/iis}";

	@Autowired
	private IisConfigService iisConfigService;

	@Value($_SERVER_SERVLET_CONTEXT_PATH_IIS)
	private String contextPath;

	@Override
	public String getContextPath() {
		return contextPath;
	}

	public @NotNull String fhirServerBasePath(Tenant tenant) {
		return getContextPath() + FHIR_SERVER_PATH_EXTENSION + "/" + tenant.getOrganizationName();
	}
}
