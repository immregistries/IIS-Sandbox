package org.immregistries.iis.kernal.mapping.internalClient;

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
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.model.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;

import static org.immregistries.iis.kernal.fhir.interceptors.CustomAuthorizationInterceptor.CONNECTATHON_USER;
import static org.immregistries.iis.kernal.fhir.security.ServletHelper.GITHUB_PREFIX;
import static org.immregistries.iis.kernal.fhir.security.ServletHelper.SESSION_TENANT;

/**
 * Generates fhir client to interact with the jpa repository
 */
@Component
public class RepositoryClientFactory extends ApacheRestfulClientFactory implements ITestingUiClientFactory {
	public static final String FHIR_CLIENT = "fhirClient";
	@Autowired
	IFhirSystemDao fhirSystemDao;
	private final Logger logger = LoggerFactory.getLogger(RepositoryClientFactory.class);
	private LoggingInterceptor loggingInterceptor;

	@Autowired
	Environment environment;

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
	}

	public IGenericClient newGenericClient(Tenant tenant) {
		asynchInit();
		IGenericClient client;
		String port = "8080";
		if(environment.getProperty("server.port") != null) {
			port = environment.getProperty("server.port");
		}
		client = newGenericClient( "http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":" + port + "/iis/fhir/" + tenant.getOrganizationName());
		IClientInterceptor authInterceptor;
		if (tenant.getOrganizationName().equals(CONNECTATHON_USER) && tenant.getUserAccess().getAccessName() == null) {
			/**
			 * SPECIFIC Connection User for Connectathon
			 * specific auth when logged in with token,
			 * AccessName is null and AccessKey bears token,
			 *
			 * see SessionAuthorizationInterceptor
			 */
			authInterceptor = new BearerTokenAuthInterceptor(tenant.getUserAccess().getAccessKey());
		} else if (tenant.getUserAccess().getAccessName().startsWith(GITHUB_PREFIX)) {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

			authInterceptor = new BearerTokenAuthInterceptor((String) authentication.getCredentials());
		} else {
			authInterceptor = new BasicAuthInterceptor(tenant.getUserAccess().getAccessName(), tenant.getUserAccess().getAccessKey());
		}
		client.registerInterceptor(authInterceptor);
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		request.setAttribute(FHIR_CLIENT, client);
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
		Tenant tenant = (Tenant) theRequestDetails.getAttribute(SESSION_TENANT);
		if (tenant == null) {
			throw new AuthenticationException();
		}
		return newGenericClient(tenant);
	}

	public IGenericClient newGenericClient(HttpServletRequest request) {
		asynchInit();
		if (request.getAttribute(FHIR_CLIENT) == null) {
			Tenant tenant = ServletHelper.getTenant();
			if (tenant != null) {
				request.setAttribute(FHIR_CLIENT, newGenericClient(tenant));
			} else {
				request.setAttribute(FHIR_CLIENT, null);
			}
		}
		return (IGenericClient) request.getAttribute(FHIR_CLIENT);
	}


	public IGenericClient getFhirClient() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		return newGenericClient(request);
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
