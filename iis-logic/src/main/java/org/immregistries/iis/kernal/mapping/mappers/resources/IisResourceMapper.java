package org.immregistries.iis.kernal.mapping.mappers.resources;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.mapping.mappers.IisMapper;
import org.immregistries.iis.kernal.model.IisMappedToFhirResource;

public interface IisResourceMapper<LocalResource extends IisMappedToFhirResource, FhirResourceType extends IAnyResource>  extends IisMapper<LocalResource, FhirResourceType> {

	LocalResource localObject(FhirResourceType fhirResourceType);

	FhirResourceType fhirObject(LocalResource localResource);
}
