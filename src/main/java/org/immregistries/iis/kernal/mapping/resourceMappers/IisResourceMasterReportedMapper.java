package org.immregistries.iis.kernal.mapping.resourceMappers;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.model.IisMappedToFhirResource;

public interface IisResourceMasterReportedMapper<Master extends IisMappedToFhirResource, Reported extends IisMappedToFhirResource, FhirResourceType extends IBaseResource> extends IisResourceMasterMapper<Master, FhirResourceType> {

	Class<Reported> localReportedType();

	Reported localObjectReportedWithMaster(FhirResourceType fhirResource);

	Reported localObjectReported(FhirResourceType fhirResource);

	Master localObject(FhirResourceType fhirResource);

	FhirResourceType fhirResource(Master master);
}
