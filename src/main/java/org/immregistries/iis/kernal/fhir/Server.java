package org.immregistries.iis.kernal.fhir;


import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.narrative.INarrativeGenerator;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.*;


import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.tenant.UrlBaseTenantIdentificationStrategy;
import org.immregistries.iis.kernal.fhir.subscription.SubscriptionProvider;
import org.immregistries.iis.kernal.fhir.subscription.SubscriptionTopicProvider;

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
    setFhirContext(Context.getCtx());

    String serverBaseUrl = "florence.immregistries.org/iis-sandbox/fhir";
//    setServerAddressStrategy(new HardcodedServerAddressStrategy(serverBaseUrl));
    setServerAddressStrategy(new ApacheProxyAddressStrategy(true));
//    setServerAddressStrategy(new IncomingRequestAddressStrategy());

    LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
    loggingInterceptor.setLoggerName("FHIR IIS");
    registerInterceptor(loggingInterceptor);

    setTenantIdentificationStrategy(new UrlBaseTenantIdentificationStrategy());

    List<IResourceProvider> resourceProviders = new ArrayList<IResourceProvider>();
    resourceProviders.add(new RestfulPatientResourceProvider());
    resourceProviders.add(new RestfulImmunizationProvider());
    resourceProviders.add(new RestfulPersonResourceProvider());
    resourceProviders.add(new RestfulMedicationAdministrationProvider());
    resourceProviders.add(new SubscriptionProvider());

    resourceProviders.add(new SubscriptionTopicProvider());
    setResourceProviders(resourceProviders);

    INarrativeGenerator narrativeGen = new DefaultThymeleafNarrativeGenerator();
    getFhirContext().setNarrativeGenerator(narrativeGen);
  }
}
