package org.immregistries.iis.kernal.logic.macro;

import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.persisted.model.UserAccess;
import org.jetbrains.annotations.NotNull;

public interface MacroEndpointService {

	@NotNull Tenant generateTenantAndContent(String bundleString, UserAccess userAccess);

}

