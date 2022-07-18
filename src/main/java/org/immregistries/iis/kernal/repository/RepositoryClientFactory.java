package org.immregistries.iis.kernal.repository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.apache.ApacheRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.UrlTenantSelectionInterceptor;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.util.ITestingUiClientFactory;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;

@Component
public class RepositoryClientFactory extends ApacheRestfulClientFactory implements ITestingUiClientFactory {
	 @Autowired
	 private IFhirSystemDao fhirSystemDao;
    private final Logger logger = LoggerFactory.getLogger(RepositoryClientFactory.class);
    private IClientInterceptor authInterceptor;
    private LoggingInterceptor loggingInterceptor;
    private UrlTenantSelectionInterceptor urlTenantSelectionInterceptor;
	 private static String serverBase = "http://localhost:8080/fhir";

	@Autowired
	public RepositoryClientFactory(){
		super();
		setServerValidationMode(ServerValidationModeEnum.NEVER);
	}

	private void asynchInit() {
		if (this.getFhirContext() == null ){
			setFhirContext(fhirSystemDao.getContext());
			loggingInterceptor = new LoggingInterceptor();
			loggingInterceptor.setLogger(logger);
		}
		if (serverBase.equals("")) {
			serverBase = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString() + "/fhir";
		}
	}

	public synchronized IGenericClient newGenericClient(OrgAccess orgAccess) {
		asynchInit();
		IGenericClient client = newGenericClient(serverBase);
		urlTenantSelectionInterceptor = new UrlTenantSelectionInterceptor(orgAccess.getAccessName());
		client.registerInterceptor(urlTenantSelectionInterceptor);
		return client;
	}


	public synchronized IGenericClient newGenericClient() {
		asynchInit();
		return newGenericClient(serverBase);
	}

	 @Override
    public synchronized IGenericClient newGenericClient(String theServerBase) {
		  asynchInit();
        IGenericClient client = super.newGenericClient(theServerBase);
		  authInterceptor = new BearerTokenAuthInterceptor();
		  client.registerInterceptor(loggingInterceptor);
		  client.registerInterceptor(authInterceptor);
        return client;
    }



	@Override
	public IGenericClient newClient(FhirContext fhirContext, HttpServletRequest httpServletRequest, String s) {
		//TODO deal with authentication/authorisation
		return null;
	}
}
