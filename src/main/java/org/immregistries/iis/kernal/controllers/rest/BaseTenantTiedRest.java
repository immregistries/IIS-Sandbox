package org.immregistries.iis.kernal.controllers.rest;

import org.immregistries.iis.kernal.mapping.internalClient.FhirReadRequester;
import org.immregistries.iis.kernal.mapping.internalClient.FhirSearchRequester;
import org.immregistries.iis.kernal.mapping.internalClient.IFhirSaveRequester;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseTenantTiedRest {

    @Autowired
    @SuppressWarnings("rawtypes")
	 protected IFhirSaveRequester fhirRequester;
    @Autowired
    protected FhirReadRequester fhirReadRequester;
	@Autowired
	protected FhirSearchRequester fhirSearchRequester;

}
