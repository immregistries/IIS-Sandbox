package org.immregistries.iis.kernal.logic.macro;

import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.jetbrains.annotations.NotNull;

public interface MacroEndpointService {

	@NotNull Tenant generateTenantAndContent(String bundleString, UserAccess userAccess);

}

