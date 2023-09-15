package org.immregistries.iis.kernal.InternalClient;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.gclient.ICriterion;
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

	public PatientMaster searchPatientMaster(SearchParameterMap searchParameterMap);

	public PatientReported searchPatientReported(SearchParameterMap searchParameterMap);

	public List<PatientReported> searchPatientReportedList(SearchParameterMap searchParameterMap);
	public List<PatientMaster> searchPatientMasterGoldenList(SearchParameterMap searchParameterMap);

	public VaccinationMaster searchVaccinationMaster(SearchParameterMap searchParameterMap);

	public VaccinationReported searchVaccinationReported(SearchParameterMap searchParameterMap);

	public List<VaccinationReported> searchVaccinationReportedList(SearchParameterMap searchParameterMap);
	public List<VaccinationMaster> searchVaccinationListOperationEverything(String patientId);

	public ObservationReported searchObservationReported(SearchParameterMap searchParameterMap);
	public Organization searchOrganization(SearchParameterMap searchParameterMap);
	public RelatedPerson searchRelatedPerson(SearchParameterMap searchParameterMap);


	public ObservationMaster searchObservationMaster(SearchParameterMap searchParameterMap);
	public List<ObservationReported> searchObservationReportedList(SearchParameterMap searchParameterMap);
	public OrgLocation searchOrgLocation(SearchParameterMap searchParameterMap);
	public List<OrgLocation> searchOrgLocationList(SearchParameterMap searchParameterMap);
	public ModelPerson searchPerson(SearchParameterMap searchParameterMap);
	public ModelPerson searchPractitioner(SearchParameterMap searchParameterMap);

	public PatientReported savePatientReported(PatientReported patientReported);
	public ModelPerson savePractitioner(ModelPerson modelPerson);
	public ObservationReported saveObservationReported(ObservationReported observationReported);

	public VaccinationReported saveVaccinationReported(VaccinationReported vaccinationReported);

	public OrgLocation saveOrgLocation(OrgLocation orgLocation);

	public Organization saveOrganization(Organization organization);

	public PatientReported saveRelatedPerson(PatientReported patientReported);

	/**
	 * Unsafe: doesn't verify golden record quality, read patient golden record and maps to PatientMaster
	 * @param id
	 * @return
	 */
	public PatientMaster readPatientMaster(String id);

	public PatientReported readPatientReported(String id);

	public ModelPerson readPractitionerPerson(String id);

	public OrgLocation readOrgLocation(String id);

	public VaccinationReported readVaccinationReported(String id);

	public PatientMaster readPatientMasterWithMdmLink(String patientId);

	IBundleProvider searchRegularRecord(Class<? extends IBaseResource> aClass, SearchParameterMap searchParameterMap);

	IBundleProvider searchGoldenRecord(Class<? extends IBaseResource> aClass, SearchParameterMap searchParameterMap);

}
