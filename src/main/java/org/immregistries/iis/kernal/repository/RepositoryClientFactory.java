package org.immregistries.iis.kernal.repository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.apache.ApacheRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.impl.HttpBasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
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

import static ca.uhn.fhir.jpa.starter.interceptors.SessionAuthorizationInterceptor.CONNECTATHON_USER;

@Component
public class RepositoryClientFactory extends ApacheRestfulClientFactory implements ITestingUiClientFactory {
	 @Autowired
	 private IFhirSystemDao fhirSystemDao;
    private final Logger logger = LoggerFactory.getLogger(RepositoryClientFactory.class);
    private LoggingInterceptor loggingInterceptor;
	 private static String serverBase = "";
	 private static final String key = "wertyuhkjbasv!#$GFRqer678GaefgAgdf:[rW4r5ty1gv2y1532efu1yeb1 k!@$534t"; // TODO automatic generation at start and chang regularly

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
		IGenericClient client = newGenericClient(serverBase + "/" + orgAccess.getOrg().getOrganizationName());
		IClientInterceptor authInterceptor;
		if (orgAccess.getOrg().getOrganizationName().equals(CONNECTATHON_USER) && orgAccess.getAccessName() == null) {
			/**
			 * SPECIFIC Connection User for Connectathon
			 * specific auth when logged in with token,
			 * AccessName is null and AccessKey bears token,
			 *
			 * see SessionAuthorizationInterceptor
			 */
			authInterceptor = new BearerTokenAuthInterceptor(orgAccess.getAccessKey());
		} else {
			authInterceptor = new BasicAuthInterceptor(orgAccess.getAccessName(), orgAccess.getAccessKey());
		}
		client.registerInterceptor(authInterceptor);
		return client;
	}

	@Override
	public synchronized IGenericClient newGenericClient(String theServerBase) {
		asynchInit();
		IGenericClient client = super.newGenericClient(theServerBase);
		client.registerInterceptor(loggingInterceptor);
		AdditionalRequestHeadersInterceptor interceptor = new AdditionalRequestHeadersInterceptor();
		interceptor.addHeaderValue("Cache-Control", "no-cache");
		 client.registerInterceptor(interceptor);
		 return client;
	 }

	@Override
	public IGenericClient newClient(FhirContext fhirContext, HttpServletRequest httpServletRequest, String s) {
		//TODO deal with authentication/authorisation
		return null;
	}
}
