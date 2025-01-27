package org.immregistries.iis.kernal.mapping.interfaces;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.model.AbstractMappedObject;

public interface IisFhirMapperMasterReported<Master extends AbstractMappedObject, Reported extends Master, FhirResourceType extends IBaseResource> extends IisFhirMapperMaster<Master, FhirResourceType> {

	Reported localObjectReportedWithMaster(FhirResourceType fhirResource);

	Reported localObjectReported(FhirResourceType fhirResource);

	Master localObject(FhirResourceType fhirResource);

	FhirResourceType fhirResource(Master master);
}
