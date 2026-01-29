package org.immregistries.iis.kernal.fhir.interceptors.multitenancy;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.partition.RequestTenantPartitionInterceptor;
import jakarta.annotation.Nonnull;
import jakarta.interceptor.Interceptor;
import org.immregistries.iis.kernal.services.PartitionCreationService;
import org.immregistries.iis.kernal.services.PartitionNameExtractorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Intercepts requests, checks if partition aimed at exists, otherwise creates new partition
 * registered in BaseJpaRestfulServer
 */
@Component
@Interceptor
public class PartitionTenantCreationInterceptor extends RequestTenantPartitionInterceptor {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	public PartitionNameExtractorService partitionNameExtractorService;
	@Autowired
	public PartitionCreationService partitionCreationService;

	@Hook(value = Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
	public boolean partitionIdentifyPostProcessed(RequestDetails theRequestDetails) {
		extractPartitionIdFromRequest(theRequestDetails);
		return true;
	}

	@Hook(value = Pointcut.STORAGE_PARTITION_IDENTIFY_READ, order = -1000)
	public RequestPartitionId partitionIdentifyRead(RequestDetails theRequestDetails) {
		return extractPartitionIdFromRequest(theRequestDetails);
	}

	@Hook(value = Pointcut.STORAGE_PARTITION_IDENTIFY_CREATE, order = -1000)
	public RequestPartitionId partitionIdentifyCreate(RequestDetails theRequestDetails) {
		return extractPartitionIdFromRequest(theRequestDetails);
	}

	@Hook(value = Pointcut.STORAGE_PARTITION_IDENTIFY_ANY, order = -1000)
	public RequestPartitionId partitionIdentifyAny(RequestDetails theRequestDetails) {
		return this.extractPartitionIdFromRequest(theRequestDetails);
	}

	@Override
	@Nonnull
	protected RequestPartitionId extractPartitionIdFromRequest(RequestDetails theRequestDetails) {
		String partitionName = partitionNameExtractorService.extractPartitionName(theRequestDetails);
		return  partitionCreationService.getOrCreatePartitionId(partitionName);
	}


}
