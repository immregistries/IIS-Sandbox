package org.immregistries.iis.kernal.fhir.interceptors;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.entity.PartitionEntity;
import ca.uhn.fhir.jpa.partition.IPartitionLookupSvc;
import ca.uhn.fhir.model.api.IFhirVersion;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceGoneException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.interceptor.partition.RequestTenantPartitionInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.SubscriptionTopic;
import org.immregistries.iis.kernal.servlet.SubscriptionTopicController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.interceptor.Interceptor;

import static org.immregistries.iis.kernal.fhir.interceptors.SessionAuthorizationInterceptor.CONNECTATHON_USER;
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
	@Autowired
	public DaoRegistry myDaoRegistry;
	@Autowired
	FhirContext fhirContext;
	private IFhirResourceDao<IBaseResource> mySubscriptionTopicDao;
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
			if (requestDetails.getTenantId().equals("ConnectathonUnsafe")) {
				return CONNECTATHON_USER;
			}
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

		//Create subscription topics
		if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
			if (mySubscriptionTopicDao == null) {
				mySubscriptionTopicDao = myDaoRegistry.getResourceDao("SubscriptionTopic");
			}
			RequestDetails requestDetails = new SystemRequestDetails();
			requestDetails.setTenantId(tenantName);
			SubscriptionTopic topic = SubscriptionTopicController.getDataQualityIssuesSubscriptionTopic();
			try {
				mySubscriptionTopicDao.read(topic.getIdElement(), requestDetails);
			} catch (ResourceNotFoundException | ResourceGoneException e) {
				mySubscriptionTopicDao.update(topic, requestDetails);
			}
			//		SubscriptionTopic groupTopic = SubscriptionTopicController.getGroupSubscriptionTopic();
	//		try {
	//			mySubscriptionTopicDao.read(groupTopic.getIdElement(), requestDetails);
	//		} catch (ResourceNotFoundException | ResourceGoneException e) {
	//			mySubscriptionTopicDao.update(groupTopic, requestDetails);
	//		}
		}

		return RequestPartitionId.fromPartitionId(idAttempt);
	}

}
