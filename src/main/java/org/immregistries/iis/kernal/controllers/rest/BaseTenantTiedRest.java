package org.immregistries.iis.kernal.controllers.rest;

import org.immregistries.iis.kernal.mapping.internalClient.IFhirRequester;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseTenantTiedRest {

    @Autowired
    @SuppressWarnings("rawtypes")
    protected IFhirRequester fhirRequester;

}
