package org.immregistries.iis.kernal.controllers.rest;

import org.immregistries.iis.kernal.mapping.requesters.FhirReadRequester;
import org.immregistries.iis.kernal.mapping.requesters.FhirSearchRequester;
import org.immregistries.iis.kernal.mapping.requesters.IFhirSaveRequester;
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
