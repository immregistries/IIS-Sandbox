package org.immregistries.iis.kernal.mapping.internalClient;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.model.*;

import java.util.List;

/**
 * Helping service to execute Queries alongside fhir mapping
 * @param <Patient>
 * @param <Immunization>
 * @param <Location>
 * @param <Practitioner>
 * @param <Observation>
 * @param <Person>
 * @param <Organization>
 * @param <RelatedPerson>
 */
public interface IFhirRequester<
	Patient extends IBaseResource,
	Immunization extends IBaseResource,
	Location extends IBaseResource,
	Practitioner extends IBaseResource,
	Observation extends IBaseResource,
	Person extends IBaseResource,
	Organization extends IBaseResource,
	RelatedPerson extends IBaseResource> {

	PatientMaster searchPatientMaster(SearchParameterMap searchParameterMap);

	PatientReported searchPatientReported(SearchParameterMap searchParameterMap);

	List<PatientReported> searchPatientReportedList(SearchParameterMap searchParameterMap);

	List<PatientMaster> searchPatientMasterGoldenList(SearchParameterMap searchParameterMap);

	VaccinationMaster searchVaccinationMaster(SearchParameterMap searchParameterMap);

	VaccinationReported searchVaccinationReported(SearchParameterMap searchParameterMap);

	List<VaccinationReported> searchVaccinationReportedList(SearchParameterMap searchParameterMap);

	List<VaccinationMaster> searchVaccinationMasterGoldenList(SearchParameterMap searchParameterMap);

	List<VaccinationMaster> searchVaccinationListOperationEverything(String patientId);

	ObservationReported searchObservationReported(SearchParameterMap searchParameterMap);

	Organization searchOrganization(SearchParameterMap searchParameterMap);

	RelatedPerson searchRelatedPerson(SearchParameterMap searchParameterMap);

	ObservationMaster searchObservationMaster(SearchParameterMap searchParameterMap);

	List<ObservationReported> searchObservationReportedList(SearchParameterMap searchParameterMap);

	OrgLocation searchOrgLocation(SearchParameterMap searchParameterMap);

	List<OrgLocation> searchOrgLocationList(SearchParameterMap searchParameterMap);

	ModelPerson searchPerson(SearchParameterMap searchParameterMap);

	ModelPerson searchPractitioner(SearchParameterMap searchParameterMap);

	PatientReported savePatientReported(PatientReported patientReported);

	ModelPerson savePractitioner(ModelPerson modelPerson);

	ObservationReported saveObservationReported(ObservationReported observationReported);

	VaccinationReported saveVaccinationReported(VaccinationReported vaccinationReported);

	OrgLocation saveOrgLocation(OrgLocation orgLocation);

	Organization saveOrganization(Organization organization);

	PatientMaster readAsPatientMaster(String id);

	PatientReported readAsPatientReported(String id);

	ModelPerson readPractitionerAsPerson(String id);

	OrgLocation readAsOrgLocation(String id);

	VaccinationReported readAsVaccinationReported(String id);

	VaccinationMaster readAsVaccinationMaster(String id);

	IBundleProvider searchRegularRecord(Class<? extends IBaseResource> aClass, SearchParameterMap searchParameterMap);

	IBundleProvider searchGoldenRecord(Class<? extends IBaseResource> aClass, SearchParameterMap searchParameterMap);

	List<PatientReported> searchPatientReportedFromGoldenIdWithMdmLinks(String patientMasterId);

	PatientMaster readPatientMasterWithMdmLink(String patientReportedId);

	List<VaccinationReported> searchVaccinationReportedFromGoldenIdWithMdmLinks(String vaccinationMasterId);

	VaccinationMaster readVaccinationMasterWithMdmLink(String vaccinationReportedId);



}
