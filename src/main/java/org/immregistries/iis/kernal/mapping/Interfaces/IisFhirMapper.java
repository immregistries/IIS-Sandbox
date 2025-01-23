package org.immregistries.iis.kernal.mapping.Interfaces;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.model.AbstractMappedObject;

public interface IisFhirMapper<Master extends AbstractMappedObject, Reported extends AbstractMappedObject, FhirResourceType extends IBaseResource> {
	Reported getReported(FhirResourceType i);

	Master getMaster(FhirResourceType i);

	FhirResourceType getFhirResource(Master vr);
}
