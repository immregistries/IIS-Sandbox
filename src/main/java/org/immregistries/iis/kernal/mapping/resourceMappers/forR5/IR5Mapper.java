package org.immregistries.iis.kernal.mapping.resourceMappers.forR5;

import org.hl7.fhir.r5.model.Resource;
import org.immregistries.iis.kernal.model.IisMappedToFhirResource;

public interface IR5Mapper<LocalResource extends IisMappedToFhirResource, FhirResource extends Resource> {
	String fhirResourceName();
	Class<LocalResource> localType();
}
