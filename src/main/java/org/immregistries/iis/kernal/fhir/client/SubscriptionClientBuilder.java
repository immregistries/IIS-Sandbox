package org.immregistries.iis.kernal.fhir.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.client.interceptor.UrlTenantSelectionInterceptor;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.Subscription;
import org.hl7.fhir.r5.model.SubscriptionStatus;
import org.immregistries.iis.kernal.fhir.Context;

public class SubscriptionClientBuilder {
    private static final FhirContext CTX = Context.getCtx();

    private final IGenericClient client;

    public SubscriptionClientBuilder(Subscription subscription){
        String secret = null;
        // Deactivate the request for server metadata
        CTX.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        // Create a client
        this.client = CTX.newRestfulGenericClient(subscription.getEndpoint());

        // Registering a logging interceptor
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        loggingInterceptor.setLogRequestSummary(true);
        loggingInterceptor.setLogRequestBody(true);
        this.client.registerInterceptor(loggingInterceptor);

        // No tenancy interceptor : Already set in endpoint

        for (StringType header: subscription.getHeader()) {
            if (header.asStringValue().startsWith("Authorization: Bearer ")) {
                secret = header.asStringValue().substring("Authorization: Bearer ".length());
                break;
            }
        }
        if (secret == null) {
            throw new InvalidRequestException("No secret specified in Subscription.header");
        }
        // Creating an auth interceptor to bear the secret
        IClientInterceptor authInterceptor = new BearerTokenAuthInterceptor(secret);
//        IClientInterceptor authInterceptor = new BasicAuthInterceptor(facilityId, subscription.getHeader().get(0).getValue());
        this.client.registerInterceptor(authInterceptor);
    }

    public IGenericClient getClient() {
        return client;
    }


}
