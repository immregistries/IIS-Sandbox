package org.immregistries.iis.kernal.mapping.resourceMappers;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.model.IisMappedToFhirResource;

public interface IisResourceMasterMapper<Master extends IisMappedToFhirResource, FhirResourceType extends IBaseResource> {

	String fhirType();
	Class<Master> localMasterType();

	Master localObject(FhirResourceType fhirResourceType);

	FhirResourceType fhirResource(Master master);
}
