package org.immregistries.iis.kernal.fhir;

import ca.uhn.fhir.jpa.partition.IRequestPartitionHelperSvc;
import ca.uhn.fhir.jpa.partition.RequestPartitionHelperSvc;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;


@Service
@Primary
public class IisRequestPartitionHelperSvc extends RequestPartitionHelperSvc implements IRequestPartitionHelperSvc {
//	@Autowired
//	PartitionCreationInterceptor partitionCreationInterceptor;


//	@Override
//	public @NotNull RequestPartitionId determineReadPartitionForRequest(@Nullable RequestDetails requestDetails, @NotNull ReadPartitionIdRequestDetails readPartitionIdRequestDetails) {
//		return partitionCreationInterceptor.partitionIdentifyRead(requestDetails);
//	}
//
//	@Override
//	public RequestPartitionId determineGenericPartitionForRequest(RequestDetails requestDetails) {
//		return partitionCreationInterceptor.partitionIdentifyRead(requestDetails);
//	}
//
//	@Override
//	public @NotNull RequestPartitionId determineCreatePartitionForRequest(@Nullable RequestDetails requestDetails, @NotNull IBaseResource iBaseResource, @NotNull String s) {
//		return partitionCreationInterceptor.partitionIdentifyRead(requestDetails);
//	}
//
//	@Override
//	public RequestPartitionId validateAndNormalizePartitionNames(RequestPartitionId requestPartitionId) {
//		return null;
//	}
}
