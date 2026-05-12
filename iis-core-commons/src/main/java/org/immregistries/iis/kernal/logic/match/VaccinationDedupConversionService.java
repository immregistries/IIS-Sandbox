package org.immregistries.iis.kernal.logic.match;

import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import org.hl7.fhir.instance.model.api.IAnyResource;

public interface VaccinationDedupConversionService<FhirImmunization extends IAnyResource> {
	org.immregistries.vaccination_deduplication.Immunization convert(FhirImmunization fhirImmunization, RequestPartitionId theRequestPartitionId);
}
