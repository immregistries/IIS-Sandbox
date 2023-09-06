package org.immregistries.iis.kernal.fhir.interceptors;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.dao.data.IPartitionDao;
import ca.uhn.fhir.jpa.partition.IPartitionLookupSvc;
import ca.uhn.fhir.jpa.partition.PartitionManagementProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.interceptor.partition.RequestTenantPartitionInterceptor;
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

/**
 * Intercepts requests, checks if partition aimed at exists, otherwise creates new partition
 * registered in BaseJpaRestfulServer
 */
@Component
@Interceptor
public class PartitionCreationInterceptor extends RequestTenantPartitionInterceptor {
	@Autowired
	IPartitionLookupSvc partitionLookupSvc;
	@Autowired
	private IPartitionDao myPartitionDao;
	@Autowired
	private PartitionManagementProvider partitionManagementProvider;
	private final Logger ourLog = LoggerFactory.getLogger(PartitionCreationInterceptor.class);

	public static final String PARTITION_NAME_SEPARATOR = "-"; // TEMP TODO find good url structure

	@Override
	@Nonnull
	protected RequestPartitionId extractPartitionIdFromRequest(RequestDetails theRequestDetails) {
		String partitionName = extractPartitionName(theRequestDetails);
		try {
			partitionLookupSvc.getPartitionByName(partitionName);
			return RequestPartitionId.fromPartitionName(partitionName);
		} catch (ResourceNotFoundException e) {
			return createPartition(theRequestDetails);
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

	private RequestPartitionId createPartition(RequestDetails theRequestDetails) {
		// Reminder tenantId = partitionName != partitionId
		StringType partitionName = new StringType(PartitionCreationInterceptor.extractPartitionName(theRequestDetails));
		Random random = new Random();
		int idAttempt = random.nextInt(10000);
		int number_of_attempts = 0;
		while (number_of_attempts < 1000 && myPartitionDao.existsById(idAttempt)){
			idAttempt = random.nextInt(10000);
			number_of_attempts++;
		}
		if (number_of_attempts == 1000){
//			throw new Exception("Impossible to generate new partition id after 1000 attempts");
		}
		Parameters inParams = new Parameters();
		inParams.addParameter().setName("id").setValue(new IntegerType(idAttempt));
		inParams.addParameter().setName("name").setValue(partitionName);
		inParams.addParameter().setName("description").setValue(partitionName);
		partitionManagementProvider.addPartition(inParams,new IntegerType(idAttempt),partitionName,partitionName);
		return RequestPartitionId.fromPartitionId(idAttempt);
	}

}
