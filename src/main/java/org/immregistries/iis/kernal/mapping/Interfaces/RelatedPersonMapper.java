package org.immregistries.iis.kernal.mapping.Interfaces;

import org.immregistries.iis.kernal.model.PatientReported;

public interface RelatedPersonMapper<RelatedPerson> {
	PatientReported fillGuardianInformation(PatientReported patientReported, RelatedPerson relatedPerson);
	RelatedPerson getFhirRelatedPersonFromPatient(PatientReported patientReported);
}
