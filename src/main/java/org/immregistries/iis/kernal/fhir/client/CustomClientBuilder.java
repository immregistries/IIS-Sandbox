package org.immregistries.iis.kernal.fhir.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.client.interceptor.UrlTenantSelectionInterceptor;
import org.immregistries.iis.kernal.fhir.Context;


/**
 * CustomClientBuilder
 * 
 * Generates the FHIR Context of the skeleton
 * 
 * 
 */
public class CustomClientBuilder {

    private static final FhirContext CTX = Context.getCtx();

    private final IGenericClient client;

    public CustomClientBuilder(String serverURL, String tenantId, String username, String password){
        // Deactivate the request for server metadata
        CTX.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        // Create a client
        this.client = CTX.newRestfulGenericClient(serverURL);

        // Register a logging interceptor
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        loggingInterceptor.setLogRequestSummary(true);
        loggingInterceptor.setLogRequestBody(true);
        this.client.registerInterceptor(loggingInterceptor);

        // Register a tenancy interceptor to add /$tenantid to the url
        UrlTenantSelectionInterceptor tenantSelection = new UrlTenantSelectionInterceptor(tenantId);
        this.client.registerInterceptor(tenantSelection);
        // Create an HTTP basic auth interceptor
        IClientInterceptor authInterceptor = new BasicAuthInterceptor(username, password);
        this.client.registerInterceptor(authInterceptor);
    }

    public IGenericClient getClient() {
        return client;
    }

    public static FhirContext getCTX() {
        return CTX;
    }
    
    
}
