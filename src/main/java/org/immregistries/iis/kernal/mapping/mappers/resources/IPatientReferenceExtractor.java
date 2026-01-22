package org.immregistries.iis.kernal.mapping.mappers.resources;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.model.ModelReference;

public interface IPatientReferenceExtractor<FhirType extends IAnyResource> {

	ModelReference extractPatientReference(FhirType immunization);

}
