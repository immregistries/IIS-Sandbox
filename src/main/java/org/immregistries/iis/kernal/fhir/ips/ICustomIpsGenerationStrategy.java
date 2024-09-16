package org.immregistries.iis.kernal.fhir.ips;

import ca.uhn.fhir.jpa.ips.api.IIpsGenerationStrategy;
import ca.uhn.fhir.jpa.ips.api.IpsContext;
import ca.uhn.fhir.jpa.ips.api.SectionRegistry;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import java.util.List;

public interface ICustomIpsGenerationStrategy extends IIpsGenerationStrategy {

//	IBaseBundle everything(IIdType theOriginalSubjectId, SectionRegistry.Section theSection) ;
//	List<IBaseResource> extractResourcesFromBundle(IpsContext.IpsSectionContext theIpsSectionContext, IBaseBundle iBaseBundle);
}
