package org.immregistries.iis.kernal.Fhir;


import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import javax.servlet.annotation.WebServlet;
import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(urlPatterns= {"/fhir/*"}, displayName="FHIR Server")
public class Server extends RestfulServer {
    private static final long serialVersionUID = 1L;

    /**
     * The initialize method is automatically called when the servlet is starting up, so it can
     * be used to configure the servlet to define resource providers, or set up
     * configuration, interceptors, etc.
     */
    @Override
    protected void initialize() throws ServletException {
        /*
         * The servlet defines any number of resource providers, and
         * configures itself to use them by calling
         * setResourceProviders()
         */
        List<IResourceProvider> resourceProviders = new ArrayList<IResourceProvider>();
        resourceProviders.add(new RestfulPatientResourceProvider());
        setResourceProviders(resourceProviders);
    }
}
