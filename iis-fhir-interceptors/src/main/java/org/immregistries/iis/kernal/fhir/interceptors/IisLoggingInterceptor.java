package org.immregistries.iis.kernal.fhir.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.immregistries.iis.kernal.fhir.IisFhirInterceptor;
import org.immregistries.iis.kernal.fhir.interceptors.multitenancy.PartitionTenantCreationInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Primary
@Interceptor
@Component
public class IisLoggingInterceptor extends LoggingInterceptor implements IisFhirInterceptor {

	@Autowired
	PartitionTenantCreationInterceptor partitionTenantCreationInterceptor;

	private final Logger myLogger = LoggerFactory.getLogger(IisLoggingInterceptor.class);

	@Hook(Pointcut.SERVER_HANDLE_EXCEPTION)
	@Override
	public boolean handleException(RequestDetails theRequestDetails, BaseServerResponseException theException, HttpServletRequest theServletRequest, HttpServletResponse theServletResponse) throws ServletException, IOException {
		this.myLogger.info("Request ID\n {}\n {}\n {}\n", theRequestDetails.getTenantId(), RequestPartitionId.defaultPartition().getPartitionNames(), partitionTenantCreationInterceptor.partitionIdentifyRead(theRequestDetails).getFirstPartitionIdOrNull());

		theException.printStackTrace();
		this.myLogger.error(theException.toString());
		return super.handleException(theRequestDetails, theException, theServletRequest, theServletResponse);

	}


}
