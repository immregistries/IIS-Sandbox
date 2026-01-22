package org.immregistries.iis.kernal.model;

import org.immregistries.iis.kernal.persisted.entities.Tenant;

public interface ITenantTiedObject {

	Tenant getTenant();

	void setTenant(Tenant tenant);
}
