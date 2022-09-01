package ca.uhn.fhir.jpa.starter.interceptors;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.entity.PartitionEntity;
import ca.uhn.fhir.jpa.partition.IPartitionLookupSvc;
import ca.uhn.fhir.jpa.partition.PartitionManagementProvider;
import ca.uhn.fhir.jpa.rp.r5.SubscriptionTopicResourceProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
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
	SubscriptionTopicResourceProvider subscriptionTopicResourceProvider;
	@Autowired
	private IPartitionLookupSvc partitionLookupSvc;
	@Autowired
	private PartitionManagementProvider partitionManagementProvider;
	private final Logger ourLog = LoggerFactory.getLogger(PartitionCreationInterceptor.class);

	@Override
	@Nonnull
	protected RequestPartitionId extractPartitionIdFromRequest(RequestDetails theRequestDetails) {
		String tenantId = theRequestDetails.getTenantId();
		if (StringUtils.isBlank(tenantId)) {
			throw new InternalErrorException(Msg.code(343) + "No tenant ID has been specified");
		} else {
//			if(tenantId.equals("DEFAULT")){
//				return RequestPartitionId.defaultPartition();
//			}
			RequestPartitionId requestPartitionId = null;
			try {
				partitionLookupSvc.getPartitionByName(theRequestDetails.getTenantId());
			} catch (ResourceNotFoundException e) {
				createPartition(theRequestDetails);
			}
			requestPartitionId = RequestPartitionId.fromPartitionName(tenantId);
			return requestPartitionId;
		}
	}

	private void createPartition(RequestDetails theRequestDetails) {
		StringType tenantId = new StringType(theRequestDetails.getTenantId());
		Random random = new Random();
		int id = random.nextInt(10000);
		int number_of_attempts = 0;
		while (number_of_attempts < 100){
			try {
				partitionLookupSvc.getPartitionById(id);
			} catch (ResourceNotFoundException ee) {
				id = random.nextInt(10000);
			}
			number_of_attempts++;
		}
		Parameters inParams = new Parameters();
		inParams.addParameter().setName("id").setValue(new IntegerType(id));
		inParams.addParameter().setName("name").setValue(tenantId);
		inParams.addParameter().setName("description").setValue(tenantId);
		partitionManagementProvider.addPartition(inParams,new IntegerType(id),tenantId,tenantId);
	}

}
