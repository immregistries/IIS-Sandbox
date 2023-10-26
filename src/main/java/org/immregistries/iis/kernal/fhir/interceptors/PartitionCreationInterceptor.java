package org.immregistries.iis.kernal.fhir.interceptors;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.dao.data.IPartitionDao;
import ca.uhn.fhir.jpa.entity.PartitionEntity;
import ca.uhn.fhir.jpa.partition.IPartitionLookupSvc;
import ca.uhn.fhir.jpa.partition.PartitionManagementProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.interceptor.partition.RequestTenantPartitionInterceptor;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.IntegerType;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.interceptor.Interceptor;
import java.util.Random;

import static org.immregistries.iis.kernal.fhir.interceptors.SessionAuthorizationInterceptor.DEFAULT_USER;

/**
 * Intercepts requests, checks if partition aimed at exists, otherwise creates new partition
 * registered in BaseJpaRestfulServer
 */
@Component
@Interceptor
public class PartitionCreationInterceptor extends RequestTenantPartitionInterceptor {
	@Autowired
	IPartitionLookupSvc partitionLookupSvc;
	private final Logger ourLog = LoggerFactory.getLogger(PartitionCreationInterceptor.class);

	public static final String PARTITION_NAME_SEPARATOR = "-"; // TEMP TODO find good url structure


	@Hook(Pointcut.STORAGE_PARTITION_IDENTIFY_READ)
	public RequestPartitionId partitionIdentifyRead(RequestDetails theRequestDetails) {
		return extractPartitionIdFromRequest(theRequestDetails);
	}

	@Hook(Pointcut.STORAGE_PARTITION_IDENTIFY_CREATE)
	public RequestPartitionId partitionIdentifyCreate(RequestDetails theRequestDetails) {
		return extractPartitionIdFromRequest(theRequestDetails);
	}

	@Override
	@Nonnull
	protected RequestPartitionId extractPartitionIdFromRequest(RequestDetails theRequestDetails) {
		String partitionName = extractPartitionName(theRequestDetails);
		return  getOrCreatePartitionId(partitionName);
	}

	public RequestPartitionId getOrCreatePartitionId(String partitionName) {
		if (StringUtils.isBlank(partitionName)) { // ALL partitions and DEFAULT partition are set to be the same
			partitionName = DEFAULT_USER;
//			return RequestPartitionId.defaultPartition();
		}
		if (partitionName.equals("default") || partitionName.equals(DEFAULT_USER) ) {
			return RequestPartitionId.defaultPartition();
		}
		try {
			partitionLookupSvc.getPartitionByName(partitionName);
			return RequestPartitionId.fromPartitionName(partitionName);
		} catch (ResourceNotFoundException e) {
			return createPartition(partitionName);
		}
	}

	public static String extractPartitionName(RequestDetails requestDetails) {
		String tenantId = requestDetails.getTenantId();
		if (StringUtils.isBlank(tenantId)) {
			throw new InvalidRequestException(Msg.code(343) + "No tenant ID has been specified, expected structure is fhir/{tenantId}-{facilityId}");
		} else {
			String[] ids = tenantId.split(PARTITION_NAME_SEPARATOR);
//			if (ids.length < 2){
//				throw new InvalidRequestException(Msg.code(343) + "No facility ID has been specified, expected structure is fhir/{tenantId}-{facilityId}");
//			}
			return ids[0];
		}
	}

	private RequestPartitionId createPartition(String tenantName) {
		int idAttempt = partitionLookupSvc.generateRandomUnusedPartitionId();
		partitionLookupSvc.createPartition(new PartitionEntity().setName(tenantName).setId(idAttempt), new SystemRequestDetails());
		return RequestPartitionId.fromPartitionId(idAttempt);
	}

}
