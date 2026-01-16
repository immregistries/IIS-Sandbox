package org.immregistries.iis.kernal.mapping.mappers.resources.r5;

import org.hl7.fhir.r5.model.Resource;
import org.immregistries.iis.kernal.mapping.mappers.IisMapper;
import org.immregistries.iis.kernal.model.IisMappedToFhirResource;

public interface IR5Mapper<LocalResource extends IisMappedToFhirResource, FhirResource extends Resource> extends IisMapper<LocalResource, FhirResource> {
}
