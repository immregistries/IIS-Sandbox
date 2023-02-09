package org.immregistries.iis.kernal.fhir.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.interceptor.ExceptionHandlingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.interceptor.Interceptor;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;


@Interceptor
public class ExceptionFilteringInterceptor extends ExceptionHandlingInterceptor {
	private static final Logger logger = LoggerFactory.getLogger(ExceptionFilteringInterceptor.class);


	/**
	 * Overides Exception handling to filter MDM exception due to establishinf manual links in MdmCustomInterceptor
	 * @param theRequestDetails
	 * @param theException
	 * @param theRequest
	 * @param theResponse
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	@Hook(Pointcut.SERVER_HANDLE_EXCEPTION)
	public boolean handleException(RequestDetails theRequestDetails, BaseServerResponseException theException, HttpServletRequest theRequest, HttpServletResponse theResponse) throws ServletException, IOException {
		if (theException instanceof InternalErrorException) {
			logger.info(theException.getMessage());
			if (theException.getMessage().startsWith("HAPI-0760: MDM system is not allowed to modify links on manually created links")){
				//Ignore
				return false;
			}
		}
		return super.handleException(theRequestDetails,theException,theRequest,theResponse);
	}

}
