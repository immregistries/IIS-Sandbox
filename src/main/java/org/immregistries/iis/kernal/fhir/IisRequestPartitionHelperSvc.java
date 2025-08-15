package org.immregistries.iis.kernal.fhir;

import ca.uhn.fhir.jpa.partition.IRequestPartitionHelperSvc;
import ca.uhn.fhir.jpa.partition.RequestPartitionHelperSvc;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;


/**
 * Used for logging and debugging
 */
@Service
@Primary
public class IisRequestPartitionHelperSvc extends RequestPartitionHelperSvc implements IRequestPartitionHelperSvc {
//	private final Logger logger = LoggerFactory.getLogger(this.getClass());

//	@Override
//	public RequestPartitionId determineReadPartitionForRequestForSearchType(
//		@Nullable RequestDetails theRequest,
//		@Nonnull String theResourceType,
//		@Nonnull SearchParameterMap theParams) {
//		return super.determineReadPartitionForRequestForSearchType(theRequest,theResourceType,theParams);
//	}
}
