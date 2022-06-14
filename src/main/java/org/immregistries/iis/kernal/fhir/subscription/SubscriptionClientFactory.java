package org.immregistries.iis.kernal.fhir.subscription;

import ca.uhn.fhir.rest.client.apache.ApacheRestfulClientFactory;
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

public class SubscriptionClientFactory extends ApacheRestfulClientFactory {

    private final Logger logger = LoggerFactory.getLogger(SubscriptionClientFactory.class);
    private String secret;
    private LoggingInterceptor loggingInterceptor;
    private IClientInterceptor authInterceptor;

    @Override
    public synchronized IGenericClient newGenericClient(String theServerBase) {
        IGenericClient client = super.newGenericClient(theServerBase);
        client.registerInterceptor(loggingInterceptor);
        client.registerInterceptor(authInterceptor);
        return client;
    }



    public SubscriptionClientFactory(Subscription subscription){
        // Deactivate the request for server metadata
        setFhirContext(Context.getCtx());
        setServerValidationMode(ServerValidationModeEnum.NEVER);

        loggingInterceptor = new LoggingInterceptor();
        loggingInterceptor.setLoggerName("Subscription Client");
        loggingInterceptor.setLogger(logger);
        /**
         *  No tenancy interceptor : Already set in endpoint
         */
        secret = null;
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
            authInterceptor = new BearerTokenAuthInterceptor(secret);
        }
    }

}
