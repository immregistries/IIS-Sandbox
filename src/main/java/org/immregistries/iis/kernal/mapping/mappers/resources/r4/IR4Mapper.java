package org.immregistries.iis.kernal.mapping.mappers.resources.r4;

import org.hl7.fhir.r4.model.Resource;
import org.immregistries.iis.kernal.mapping.mappers.IisMapper;
import org.immregistries.iis.kernal.model.IisMappedToFhirResource;

public interface IR4Mapper<LocalResource extends IisMappedToFhirResource, FhirResource extends Resource> extends IisMapper<LocalResource, FhirResource> {

}
