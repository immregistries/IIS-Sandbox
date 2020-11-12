package org.immregistries.iis.kernal.fhir;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.narrative.INarrativeGenerator;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import javax.servlet.annotation.WebServlet;
import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;
import ca.uhn.fhir.rest.server.tenant.UrlBaseTenantIdentificationStrategy;

@WebServlet(urlPatterns= {"/fhir/*"}, displayName="FHIR Server")
public class Server extends RestfulServer {
    private static final long serialVersionUID = 1L;

    /**
     * The initialize method is automatically called when the servlet is starting up, so it can
     * be used to configure the servlet to define resource providers, or set up
     * configuration, interceptors, etc.
     */

    /*public Server() {



        // ...add some resource providers, etc...
        List<IResourceProvider> resourceProviders = new ArrayList<IResourceProvider>();

        setResourceProviders(resourceProviders);
    }*/
    @Override
    protected void initialize() throws ServletException {
        //setFhirContext(FhirContext.forR4());

        this.setDefaultResponseEncoding(EncodingEnum.XML);

        String serverBaseUrl = "http://localhost:8080/iis-sandbox/fhir";
        setTenantIdentificationStrategy(new UrlBaseTenantIdentificationStrategy());
        setServerAddressStrategy(new HardcodedServerAddressStrategy(serverBaseUrl));
        /*
         * The servlet defines any number of resource providers, and
         * configures itself to use them by calling
         * setResourceProviders()
         */
        List<IResourceProvider> resourceProviders = new ArrayList<IResourceProvider>();
        resourceProviders.add(new RestfulPatientResourceProvider());
        resourceProviders.add(new RestfuImmunizationProvider());

        setResourceProviders(resourceProviders);

        INarrativeGenerator narrativeGen = new DefaultThymeleafNarrativeGenerator();
        getFhirContext().setNarrativeGenerator(narrativeGen);

        //registerInterceptor(new ResponseHighlighterInterceptor());
    }
}
