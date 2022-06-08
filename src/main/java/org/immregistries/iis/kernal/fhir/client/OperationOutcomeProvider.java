package org.immregistries.iis.kernal.fhir.client;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.r5.model.OperationOutcome;

public class OperationOutcomeProvider implements IResourceProvider {

    @Override
    public Class<OperationOutcome> getResourceType() {
        return OperationOutcome.class;
    }

    @Create
    // Setting up a payload for subscription
    public OperationOutcome registerOperationOutcome(@ResourceParam OperationOutcome operationOutcome) {
//        operationOutcome.setIssue();
        return operationOutcome;
    }
}
