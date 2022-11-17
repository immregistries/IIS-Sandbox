package ca.uhn.fhir.jpa.starter.interceptors;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.dao.data.IPartitionDao;
import ca.uhn.fhir.jpa.partition.PartitionManagementProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
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
	private IPartitionDao myPartitionDao;
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
			if (myPartitionDao.findForName(theRequestDetails.getTenantId()).isPresent()) {
				return RequestPartitionId.fromPartitionName(tenantId);
			} else {
				return createPartition(theRequestDetails);
			}
		}
	}

	private RequestPartitionId createPartition(RequestDetails theRequestDetails) {
		// Reminder tenantId = partitionName != partitionId
		StringType partitionName = new StringType(theRequestDetails.getTenantId());
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
