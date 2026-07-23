package org.immregistries.iis.kernal.mapping.requesters;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.model.*;

import java.util.Optional;
import java.util.stream.Stream;

public interface IFhirReadRequester {
	IBaseResource read(String fhirType, String id);

	IisPatient readAsPatient(String id);

	PatientMaster readAsPatientMaster(String id);

	PatientReported readAsPatientReported(String id);

	ModelPerson readPractitionerAsPerson(String id);

	OrgLocation readAsOrgLocation(String id);

	VaccinationReported readAsVaccinationReported(String id);

	IisVaccination readAsVaccination(String id);

	VaccinationMaster readAsVaccinationMaster(String id);

	Optional<String> readGoldenResourceId(String reportId);

	PatientMaster readPatientMasterWithMdmLink(String patientId);

	VaccinationMaster readVaccinationMasterWithMdmLink(String vaccinationReportedId);

	Stream<String> readMdmlinksReportedIds(String masterId);
}
