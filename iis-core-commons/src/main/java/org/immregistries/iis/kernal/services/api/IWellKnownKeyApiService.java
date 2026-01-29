package org.immregistries.iis.kernal.services.api;

import jakarta.servlet.http.HttpServletRequest;
import org.immregistries.iis.kernal.persisted.entities.Tenant;

public interface IWellKnownKeyApiService {

	String getKeyIssuerUrl(HttpServletRequest request, Tenant tenant);

}
