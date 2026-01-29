package org.immregistries.iis.kernal.mapping.mappers.resources.r5;

import org.hl7.fhir.r5.model.Resource;
import org.immregistries.iis.kernal.mapping.mappers.IR5Mapper;
import org.immregistries.iis.kernal.model.IisMappedToFhirResource;

/**
 * Interface to prepare a potential coexistence of mappers in certain contexts
 *
 * @param <LocalResource> extends IisMappedToFhirResource
 * @param <FhirResource>  extends R4 Resource
 */
public interface IR5ResourceMapper<LocalResource extends IisMappedToFhirResource, FhirResource extends Resource> extends IR5Mapper<LocalResource, FhirResource> {
}
