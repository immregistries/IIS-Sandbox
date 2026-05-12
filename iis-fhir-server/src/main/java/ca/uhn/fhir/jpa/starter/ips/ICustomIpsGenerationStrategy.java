package ca.uhn.fhir.jpa.starter.ips;

import ca.uhn.fhir.jpa.ips.api.IIpsGenerationStrategy;

public interface ICustomIpsGenerationStrategy extends IIpsGenerationStrategy {
	String $_MDM_QUERY_LINKS = "$mdm-query-links";


//	IBaseBundle everything(IIdType theOriginalSubjectId, Section theSection);
//
//	List<IAnyResource> extractResourcesFromBundle(IpsSectionContext theIpsSectionContext, IBaseBundle iBaseBundle);
}
