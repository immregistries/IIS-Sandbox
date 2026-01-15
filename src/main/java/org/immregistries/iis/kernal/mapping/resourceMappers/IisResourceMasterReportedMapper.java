package org.immregistries.iis.kernal.mapping.resourceMappers;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.model.IisMappedToFhirResource;

public interface IisResourceMasterReportedMapper<Master extends LocalResource, Reported extends LocalResource, LocalResource extends IisMappedToFhirResource, FhirResourceType extends IAnyResource> extends IisResourceMasterMapper<LocalResource, FhirResourceType> {

	Class<Reported> localReportedType();

	Class<Master> localMasterType();

	Reported localObjectReportedWithMaster(FhirResourceType fhirResource);

	Reported localObjectReported(FhirResourceType fhirResource);

	Master localObjectMaster(FhirResourceType fhirResource);

}
