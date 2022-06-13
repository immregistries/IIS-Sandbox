package org.immregistries.iis.kernal.fhir.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.Subscription;
import org.immregistries.iis.kernal.fhir.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubscriptionClientBuilder {

    private final Logger logger = LoggerFactory.getLogger(SubscriptionClientBuilder.class);

    private final IGenericClient client;

    public SubscriptionClientBuilder(Subscription subscription){
        // Deactivate the request for server metadata
        Context.getCtx().getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        // Create a client
        this.client = Context.getCtx().newRestfulGenericClient(subscription.getEndpoint());
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        loggingInterceptor.setLoggerName("Subscription Client");
        loggingInterceptor.setLogger(logger);
        this.client.registerInterceptor(loggingInterceptor);

        /**
         *  No tenancy interceptor : Already set in endpoint
         */
        String secret = null;
        for (StringType header: subscription.getHeader()) {
            if (header.asStringValue().startsWith("Authorization: Bearer ")) {
                secret = header.asStringValue().substring("Authorization: Bearer ".length());
                break;
            }
        }
        if (secret == null) {
            throw new InvalidRequestException("No secret specified in Subscription.header");
        } else {
            // Creating an auth interceptor to bear the secret
            IClientInterceptor authInterceptor = new BearerTokenAuthInterceptor(secret);
            this.client.registerInterceptor(authInterceptor);
        }

    }

    public IGenericClient getClient() {
        return client;
    }


}
