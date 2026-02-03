package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.jpa.ips.api.IIpsGenerationStrategy;
import ca.uhn.fhir.jpa.ips.api.IpsSectionContext;
import ca.uhn.fhir.jpa.ips.api.Section;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IIdType;

import java.util.List;

public interface ICustomIpsGenerationStrategy extends IIpsGenerationStrategy {
	String $_MDM_QUERY_LINKS = "$mdm-query-links";


	IBaseBundle everything(IIdType theOriginalSubjectId, Section theSection);

	List<IAnyResource> extractResourcesFromBundle(IpsSectionContext theIpsSectionContext, IBaseBundle iBaseBundle);
}
