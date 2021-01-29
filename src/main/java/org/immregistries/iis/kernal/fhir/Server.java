package org.immregistries.iis.kernal.fhir;


import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.narrative.INarrativeGenerator;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;


import ca.uhn.fhir.rest.server.tenant.UrlBaseTenantIdentificationStrategy;

@WebServlet(urlPatterns = {"/fhir/*"}, displayName = "FHIR Server")
public class Server extends RestfulServer {
  private static final long serialVersionUID = 1L;

  /**
   * The initialize method is automatically called when the servlet is starting up, so it can
   * be used to configure the servlet to define resource providers, or set up
   * configuration, interceptors, etc.
   */

  @Override
  protected void initialize() throws ServletException {
    this.setDefaultResponseEncoding(EncodingEnum.XML);

    String serverBaseUrl = "http://localhost:8080/iis-sandbox/fhir";
    setTenantIdentificationStrategy(new UrlBaseTenantIdentificationStrategy());
    setServerAddressStrategy(new HardcodedServerAddressStrategy(serverBaseUrl));

    List<IResourceProvider> resourceProviders = new ArrayList<IResourceProvider>();
    resourceProviders.add(new RestfulPatientResourceProvider());
    resourceProviders.add(new RestfulImmunizationProvider());
    resourceProviders.add(new RestfulPersonResourceProvider());
    resourceProviders.add(new RestfulMedicationAdministrationProvider());
    setResourceProviders(resourceProviders);

    INarrativeGenerator narrativeGen = new DefaultThymeleafNarrativeGenerator();
    getFhirContext().setNarrativeGenerator(narrativeGen);


  }
}
