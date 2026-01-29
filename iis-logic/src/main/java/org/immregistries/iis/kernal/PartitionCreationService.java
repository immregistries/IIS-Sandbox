package org.immregistries.iis.kernal;

import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.entity.PartitionEntity;
import ca.uhn.fhir.jpa.partition.IPartitionLookupSvc;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PartitionCreationService {

	@Autowired
	private IPartitionLookupSvc partitionLookupSvc;

	public RequestPartitionId getOrCreatePartitionId(String partitionName) {
		if (StringUtils.isBlank(partitionName)) { // ALL partitions and DEFAULT partition are set to be the same
			partitionName = GlobalConstants.DEFAULT_USER;
//			return RequestPartitionId.defaultPartition();
		}
		if (Strings.CI.equals(partitionName,GlobalConstants.DEFAULT_USER)) {
			return RequestPartitionId.defaultPartition();
		}
		try {
			PartitionEntity partitionEntity = partitionLookupSvc.getPartitionByName(partitionName);
			return partitionEntity.toRequestPartitionId();
		} catch (ResourceNotFoundException e) {
			return createPartition(partitionName);
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
