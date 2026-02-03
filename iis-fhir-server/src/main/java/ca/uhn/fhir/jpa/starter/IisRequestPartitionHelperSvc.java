package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.jpa.partition.IRequestPartitionHelperSvc;
import ca.uhn.fhir.jpa.partition.RequestPartitionHelperSvc;


/**
 * Used for logging and debugging
 */
//@Service
//@Primary
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
