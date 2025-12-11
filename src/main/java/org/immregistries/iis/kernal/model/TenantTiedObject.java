package org.immregistries.iis.kernal.model;

import org.immregistries.iis.kernal.persisted.model.Tenant;

public interface TenantTiedObject {

	Tenant getTenant();

	void setTenant(Tenant tenant);
}
