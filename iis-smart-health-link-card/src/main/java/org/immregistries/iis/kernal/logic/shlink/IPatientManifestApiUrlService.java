package org.immregistries.iis.kernal.logic.shlink;

import jakarta.servlet.http.HttpServletRequest;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.jetbrains.annotations.NotNull;

public interface IPatientManifestApiUrlService {
	@NotNull String getManifestUrl(HttpServletRequest req, IAnyResource patientSelected, Tenant tenant);

	@NotNull String getManifestUrl(String baseUrl, IAnyResource patientSelected, Tenant tenant);
}
