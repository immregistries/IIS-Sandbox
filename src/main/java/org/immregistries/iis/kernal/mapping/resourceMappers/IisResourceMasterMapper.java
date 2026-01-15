package org.immregistries.iis.kernal.mapping.resourceMappers;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.model.IisMappedToFhirResource;

public interface IisResourceMasterMapper<LocalResource extends IisMappedToFhirResource, FhirResourceType extends IBaseResource> {

	String fhirType();

	Class<LocalResource> localType();

	LocalResource localObject(FhirResourceType fhirResourceType);

	FhirResourceType fhirResource(LocalResource localResource);
}
