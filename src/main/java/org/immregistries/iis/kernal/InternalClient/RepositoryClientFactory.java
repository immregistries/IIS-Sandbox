package org.immregistries.iis.kernal.InternalClient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.apache.ApacheRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.util.ITestingUiClientFactory;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static org.immregistries.iis.kernal.fhir.interceptors.SessionAuthorizationInterceptor.CONNECTATHON_USER;

/**
 * Generates fhir client to interact with the jpa repository
 */
@Component
public class RepositoryClientFactory extends ApacheRestfulClientFactory implements ITestingUiClientFactory {
	@Autowired
	private IFhirSystemDao fhirSystemDao;
	private final Logger logger = LoggerFactory.getLogger(RepositoryClientFactory.class);
	private LoggingInterceptor loggingInterceptor;
	private static String serverBase = "";

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

	public IGenericClient newGenericClient(OrgAccess orgAccess) {
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

	/**
	 * Used to get a fhir client within HAPIFHIR Interceptors
	 *
	 * @param theRequestDetails
	 * @return
	 */
	public IGenericClient newGenericClient(RequestDetails theRequestDetails) {
		asynchInit();
		OrgAccess orgAccess = (OrgAccess) theRequestDetails.getAttribute("orgAccess");
		if (orgAccess == null) {
			throw new AuthenticationException();
		}
		return newGenericClient(orgAccess);
	}

	/**
	 * Used to get a fhir client within Java Servlets
	 *
	 * @param session
	 * @return
	 */
	public IGenericClient newGenericClient(HttpSession session) {
		asynchInit();
		if (session.getAttribute("fhirClient") == null) {
			OrgAccess orgAccess = (OrgAccess) session.getAttribute("orgAccess");
			session.setAttribute("fhirClient", newGenericClient(orgAccess));
		}
		return (IGenericClient) session.getAttribute("fhirClient");
	}


	public IGenericClient getFhirClientFromSession() {
		HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession(false);
		return newGenericClient(session);
	}


	/**
	 * Used for manual subscription trigger
	 *
	 * @param theServerBase
	 * @return
	 */
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
		return null;
	}
}