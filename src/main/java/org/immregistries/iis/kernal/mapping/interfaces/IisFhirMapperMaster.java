package org.immregistries.iis.kernal.mapping.interfaces;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.model.AbstractMappedObject;

public interface IisFhirMapperMaster<Master extends AbstractMappedObject, FhirResourceType extends IBaseResource> {

	Master localObject(FhirResourceType fhirResourceType);

	FhirResourceType fhirResource(Master master);
}
