package org.immregistries.iis.kernal.mapping.Interfaces;

import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;

public interface RelatedPersonMapper<RelatedPerson> {
	void fillGuardianInformation(PatientMaster patientMaster, RelatedPerson relatedPerson);
	RelatedPerson getFhirRelatedPersonFromPatient(PatientMaster patientMaster);
}
