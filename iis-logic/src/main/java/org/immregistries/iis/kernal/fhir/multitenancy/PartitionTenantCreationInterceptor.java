package org.immregistries.iis.kernal.fhir.multitenancy;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.entity.PartitionEntity;
import ca.uhn.fhir.jpa.partition.IPartitionLookupSvc;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.interceptor.partition.RequestTenantPartitionInterceptor;
import jakarta.annotation.Nonnull;
import jakarta.interceptor.Interceptor;
import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.GlobalConstants;
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
	private IPartitionLookupSvc partitionLookupSvc;

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
		String partitionName = extractPartitionName(theRequestDetails);
		return  getOrCreatePartitionId(partitionName);
	}

	public RequestPartitionId getOrCreatePartitionId(String partitionName) {
		if (StringUtils.isBlank(partitionName)) { // ALL partitions and DEFAULT partition are set to be the same
			partitionName = GlobalConstants.DEFAULT_USER;
//			return RequestPartitionId.defaultPartition();
		}
		if (partitionName.equals("default") || partitionName.equals(GlobalConstants.DEFAULT_USER) ) {
			return RequestPartitionId.defaultPartition();
		}
		try {
			PartitionEntity partitionEntity = partitionLookupSvc.getPartitionByName(partitionName);
			return partitionEntity.toRequestPartitionId();
		} catch (ResourceNotFoundException e) {
			return createPartition(partitionName);
		}
	}

	public static String extractPartitionName(RequestDetails requestDetails) {
		String tenantId = requestDetails.getTenantId();
		if (StringUtils.isBlank(tenantId)) {
			throw new InvalidRequestException(Msg.code(343) + "No tenant ID was specified");
		} else {
			if (requestDetails.getTenantId().equals("ConnectathonUnsafe")) {
				return GlobalConstants.CONNECTATHON_USER;
			}
//			String[] ids = tenantId.split(PARTITION_NAME_SEPARATOR);
//			if (ids.length < 2){
//				throw new InvalidRequestException(Msg.code(343) + "No facility ID has been specified, expected structure is fhir/{tenantId}-{facilityId}");
//			}
			return tenantId;
		}
	}

	private RequestPartitionId createPartition(String tenantName) {
		int idAttempt = partitionLookupSvc.generateRandomUnusedPartitionId();
		PartitionEntity partitionEntity = partitionLookupSvc.createPartition(new PartitionEntity().setName(tenantName).setId(idAttempt), new SystemRequestDetails());

		//Create subscription topics
//		if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
//			if (mySubscriptionTopicDao == null) {
//				mySubscriptionTopicDao = myDaoRegistry.getResourceDao("SubscriptionTopic");
//			}
//			RequestDetails requestDetails =  SystemRequestDetails.forRequestPartitionId(partitionEntity.toRequestPartitionId());
//			SubscriptionTopic topic = SubscriptionTopicController.getDataQualityIssuesSubscriptionTopic();
//			try {
//				mySubscriptionTopicDao.read(topic.getIdElement(), requestDetails);
//			} catch (ResourceNotFoundException | ResourceGoneException e) {
//				mySubscriptionTopicDao.update(topic, requestDetails);
//			}
			//		SubscriptionTopic groupTopic = SubscriptionTopicController.getGroupSubscriptionTopic();
	//		try {
	//			mySubscriptionTopicDao.read(groupTopic.getIdElement(), requestDetails);
	//		} catch (ResourceNotFoundException | ResourceGoneException e) {
	//			mySubscriptionTopicDao.update(groupTopic, requestDetails);
	//		}
//		}

		return partitionEntity.toRequestPartitionId();
	}


}
