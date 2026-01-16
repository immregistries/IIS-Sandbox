package org.immregistries.iis.kernal.mapping;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.apache.ApacheRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.immregistries.iis.kernal.Application;

import org.immregistries.iis.kernal.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.MalformedURLException;
import java.net.URL;

import static org.immregistries.iis.kernal.fhir.interceptors.IisAuthorizationInterceptor.CONNECTATHON_USER;
import static org.immregistries.iis.kernal.security.UserAccessUtil.GITHUB_PREFIX;
import static org.immregistries.iis.kernal.security.CurrentTenantUtil.SESSION_REQUEST_TENANT;

/**
 * Generates fhir client to interact with the jpa repository
 */
@Component
public class IisFhirClientFactory extends ApacheRestfulClientFactory {
	public static final String FHIR_CLIENT_REQUEST_ATTRIBUTE = "fhirClient";

	@Autowired
	public void setFhirContext(FhirContext fhirContext) {
		super.setFhirContext(fhirContext);
	}

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private LoggingInterceptor loggingInterceptor;

	@Autowired
	public IisFhirClientFactory() {
		super();
		setServerValidationMode(ServerValidationModeEnum.NEVER);
	}

	protected void asynchInit() {
		if (loggingInterceptor == null) {
//			super.setFhirContext(fhirContext);
			loggingInterceptor = new LoggingInterceptor();
			loggingInterceptor.setLogger(logger);
		}
	}

	public IGenericClient newGenericClient(Tenant tenant, HttpServletRequest httpServletRequest) {
		asynchInit();
		IGenericClient client;
		URL serverBase = extractServerBase(tenant, httpServletRequest);
		client = newGenericClient(serverBase.toString());
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
			authInterceptor = new BasicAuthInterceptor(tenant.getUserAccess().getAccessName(),
					tenant.getUserAccess().getAccessKey());
		}
		client.registerInterceptor(authInterceptor);
		return client;
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

	public IGenericClient getOrCreateFhirClientFromContext() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
			.getRequest();
		return getOrCreateGenericClient(request);
	}

	public IGenericClient getOrCreateGenericClient(HttpServletRequest request) {
		asynchInit();
		if (request.getAttribute(FHIR_CLIENT_REQUEST_ATTRIBUTE) == null) {
			Tenant tenant = CurrentTenantUtil.getTenant(request);
			if (tenant != null) {
				request.setAttribute(FHIR_CLIENT_REQUEST_ATTRIBUTE, newGenericClient(tenant, request));
			} else {
				request.setAttribute(FHIR_CLIENT_REQUEST_ATTRIBUTE, null);
			}
		}
		return (IGenericClient) request.getAttribute(FHIR_CLIENT_REQUEST_ATTRIBUTE);
	}

	/**
	 * Used to get a fhir client within HAPIFHIR Interceptors
	 *
	 * @param theRequestDetails
	 * @return
	 */
	public IGenericClient getOrCreateGenericClient(ServletRequestDetails theRequestDetails) {
		asynchInit();
		Tenant tenant = (Tenant) theRequestDetails.getAttribute(SESSION_REQUEST_TENANT);
		if (tenant == null) {
			throw new AuthenticationException();
		}
		return newGenericClient(tenant, theRequestDetails.getServletRequest());
	}

	private URL extractServerBase(Tenant tenant, HttpServletRequest httpServletRequest) {
		UriComponentsBuilder uriComponentsBuilder = ServletUriComponentsBuilder.fromRequestUri(httpServletRequest);
		URL serverBase;
		try {
			uriComponentsBuilder.replacePath(Application.fhirServerBasePath(tenant));
			uriComponentsBuilder.replaceQuery("");
			serverBase = uriComponentsBuilder.build().toUri().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		return serverBase;
	}

}
