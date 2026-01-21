package org.immregistries.iis.kernal.logic.match;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.mismo.match.model.Patient;

public interface PatientMismoConversionService<FhirPatient extends IAnyResource> {

	Patient convert(FhirPatient patient);

	Patient convertIncludingLink(FhirPatient patient);


}
