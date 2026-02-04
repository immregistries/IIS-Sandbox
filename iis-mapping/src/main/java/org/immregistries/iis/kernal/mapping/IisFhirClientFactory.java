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
import org.immregistries.iis.kernal.GlobalConstants;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.immregistries.iis.kernal.security.RequestTenantUtil;
import org.immregistries.iis.kernal.services.api.IDeployedApiUrlService;
import org.jetbrains.annotations.NotNull;
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

import static org.immregistries.iis.kernal.GlobalConstants.SESSION_REQUEST_TENANT;
import static org.immregistries.iis.kernal.security.UserAccessUtil.GITHUB_PREFIX;

/**
 * Generates fhir client to interact with the jpa repository
 */
@Component
public class IisFhirClientFactory extends ApacheRestfulClientFactory {
	private static final String CACHE_CONTROL = "Cache-Control";
	private static final String NO_CACHE = "no-cache";

	@Autowired
	public void setFhirContext(FhirContext fhirContext) {
		super.setFhirContext(fhirContext);
	}

	@Autowired
	private IDeployedApiUrlService apiUrlService;
	@Autowired
	private RequestTenantUtil requestTenantUtil;

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private LoggingInterceptor loggingInterceptor;

	private LoggingInterceptor getLoggingInterceptor() {
		if (loggingInterceptor == null) {
			loggingInterceptor = new LoggingInterceptor();
			loggingInterceptor.setLogger(logger);
		}
		return loggingInterceptor;
	}

	@Autowired
	public IisFhirClientFactory() {
		super();
		setServerValidationMode(ServerValidationModeEnum.NEVER);
	}


	/**
	 *
	 * @param tenant             Tenant for Authorization
	 * @param httpServletRequest HttpServletRequest to extract Server base url from
	 * @return client
	 */
	public IGenericClient newGenericClient(Tenant tenant, HttpServletRequest httpServletRequest) {
		IGenericClient client;
		URL serverBase = extractServerBase(tenant, httpServletRequest);
		client = newGenericClient(serverBase.toString());
		IClientInterceptor authInterceptor = getClientAuthInterceptor(tenant);
		client.registerInterceptor(authInterceptor);
		return client;
	}

	private @NotNull IClientInterceptor getClientAuthInterceptor(Tenant tenant) {
		IClientInterceptor authInterceptor;
		UserAccess userAccess = tenant.getUserAccess();
		String accessName = userAccess.getAccessName();
		String accessKey = userAccess.getAccessKey();
		if (tenant.getOrganizationName().equals(GlobalConstants.CONNECTATHON_USER) && accessName == null) {
			/**
			 * SPECIFIC Connection User for Connectathon
			 * specific auth when logged in with token,
			 * AccessName is null and AccessKey bears token,
			 *
			 * see SessionAuthorizationInterceptor
			 */
			authInterceptor = new BearerTokenAuthInterceptor(accessKey);
		} else if (accessName.startsWith(GITHUB_PREFIX)) {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			authInterceptor = new BearerTokenAuthInterceptor((String) authentication.getCredentials());
		} else {
			authInterceptor = new BasicAuthInterceptor(accessName, accessKey);
		}
		return authInterceptor;
	}


	/**
	 * Used for manual subscription trigger
	 *
	 * @param theServerBase
	 * @return unauthorized client
	 */
	@Override
	public synchronized IGenericClient newGenericClient(String theServerBase) {
		IGenericClient client = super.newGenericClient(theServerBase);
		client.registerInterceptor(getLoggingInterceptor());
		AdditionalRequestHeadersInterceptor interceptor = new AdditionalRequestHeadersInterceptor();
		interceptor.addHeaderValue(CACHE_CONTROL, NO_CACHE);
		client.registerInterceptor(interceptor);
		return client;
	}

	public IGenericClient getOrCreateFhirClientFromContext() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
			.getRequest();
		return getOrCreateGenericClient(request);
	}

	public IGenericClient getOrCreateGenericClient(HttpServletRequest request) {
		if (request.getAttribute(GlobalConstants.FHIR_CLIENT_REQUEST_ATTRIBUTE) == null) {
			Tenant tenant = requestTenantUtil.extractTenant(request);
			if (tenant != null) {
				request.setAttribute(GlobalConstants.FHIR_CLIENT_REQUEST_ATTRIBUTE, newGenericClient(tenant, request));
			} else {
				request.setAttribute(GlobalConstants.FHIR_CLIENT_REQUEST_ATTRIBUTE, null);
			}
		}
		return (IGenericClient) request.getAttribute(GlobalConstants.FHIR_CLIENT_REQUEST_ATTRIBUTE);
	}

	/**
	 * Used to get a fhir client within HAPIFHIR Interceptors
	 *
	 * @param theRequestDetails
	 * @return
	 */
	public IGenericClient getOrCreateGenericClient(ServletRequestDetails theRequestDetails) {
		Tenant tenant = requestTenantUtil.extractTenant(theRequestDetails);
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
			uriComponentsBuilder.replacePath(apiUrlService.fhirServerBasePath(tenant));
			uriComponentsBuilder.replaceQuery("");
			serverBase = uriComponentsBuilder.build().toUri().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		return serverBase;
	}

}
