package org.immregistries.iis.kernal.mapping.mappers.resources.r4;

import org.hl7.fhir.r4.model.Resource;
import org.immregistries.iis.kernal.mapping.mappers.IR4Mapper;
import org.immregistries.iis.kernal.model.IisMappedToFhirResource;

/**
 * Interface to prepare a potential coexistence of mappers in certain contexts
 *
 * @param <LocalResource> extends IisMappedToFhirResource
 * @param <FhirResource>  extends R4 Resource
 */
public interface IR4ResourceMapper<LocalResource extends IisMappedToFhirResource, FhirResource extends Resource> extends IR4Mapper<LocalResource, FhirResource> {
}
