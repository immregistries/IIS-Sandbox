package org.immregistries.iis.kernal.repository;

import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.apache.ApacheRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Component
public class RepositoryClientFactory extends ApacheRestfulClientFactory {
	 @Autowired
	 private IFhirSystemDao fhirSystemDao;
    private final Logger logger = LoggerFactory.getLogger(RepositoryClientFactory.class);
    private IClientInterceptor authInterceptor;
	 private static String serverBase = "http://localhost:8080/fhir";

	@Autowired
	public RepositoryClientFactory(){
		super();
		setServerValidationMode(ServerValidationModeEnum.NEVER);
	}

	private void asynchInit() {
		if (this.getFhirContext() == null ){
			setFhirContext(fhirSystemDao.getContext());
		}
		if (serverBase.equals("")) {
			serverBase = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString() + "/fhir";
		}
	}

	public synchronized IGenericClient newGenericClient(OrgAccess orgAccess) {
		asynchInit();
		return newGenericClient(serverBase + "/" + orgAccess.getAccessName());
	}

	public synchronized IGenericClient newGenericClient(RequestDetails theRequestDetails) {
		asynchInit();
		if (theRequestDetails.getTenantId() != null) {
			return newGenericClient(serverBase + "/" + theRequestDetails.getTenantId());
		} else {
			return newGenericClient(serverBase);
		}
	}

    @Override
    public synchronized IGenericClient newGenericClient(String theServerBase) {
		  asynchInit();
        IGenericClient client = super.newGenericClient(theServerBase);
		  authInterceptor = new BearerTokenAuthInterceptor();
		  client.registerInterceptor(authInterceptor);
        return client;
    }



}
