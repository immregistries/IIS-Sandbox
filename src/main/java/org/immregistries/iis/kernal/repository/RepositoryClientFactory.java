package org.immregistries.iis.kernal.repository;

import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.apache.ApacheRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Component
public class RepositoryClientFactory extends ApacheRestfulClientFactory {
	 @Autowired
	 private IFhirSystemDao fhirSystemDao;
    private final Logger logger = LoggerFactory.getLogger(RepositoryClientFactory.class);
//	 private LoggingInterceptor loggingInterceptor;
    private IClientInterceptor authInterceptor;
	 private static String theServerBase = "http://localhost:8080";

	@Autowired
	public RepositoryClientFactory(){
		super();
		setServerValidationMode(ServerValidationModeEnum.NEVER);
	}

	public synchronized IGenericClient newGenericClient(RequestDetails theRequestDetails) {
		if (this.getFhirContext() == null ){
			setFhirContext(fhirSystemDao.getContext());
		}
		if (theServerBase.equals("")) {
//			fhirSystemDao.get
			theServerBase = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString() + "/fhir";
		}
		if (theRequestDetails.getTenantId() != null) {
			return newGenericClient(theServerBase + "/" + theRequestDetails.getTenantId());
		} else {
			return newGenericClient(theServerBase);
		}
	}

    @Override
    public synchronized IGenericClient newGenericClient(String theServerBase) {
        IGenericClient client = super.newGenericClient(theServerBase);

//		 loggingInterceptor = new LoggingInterceptor();
//		 loggingInterceptor.setLoggerName("Subscription Client");
//		 loggingInterceptor.setLogger(logger);
//        client.registerInterceptor(loggingInterceptor);
		  authInterceptor = new BearerTokenAuthInterceptor();
		  client.registerInterceptor(authInterceptor);
        return client;
    }



}
