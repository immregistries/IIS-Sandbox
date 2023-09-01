package org.immregistries.iis.kernal.InternalClient;

import ca.uhn.fhir.rest.gclient.ICriterion;
import org.hl7.fhir.instance.model.api.IBaseBundle;
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

	public PatientMaster searchPatientMaster(ICriterion... where);

	public PatientReported searchPatientReported(ICriterion... where);

	public List<PatientReported> searchPatientReportedList(ICriterion... where);
	public List<PatientMaster> searchPatientMasterGoldenList(ICriterion... where);

	public VaccinationMaster searchVaccinationMaster(ICriterion... where);

	public VaccinationReported searchVaccinationReported(ICriterion... where);

	public List<VaccinationReported> searchVaccinationReportedList(ICriterion... where);
	public List<VaccinationMaster> searchVaccinationListOperationEverything(String patientId);

	public ObservationReported searchObservationReported(ICriterion... where);
	public Organization searchOrganization(ICriterion... where);
	public RelatedPerson searchRelatedPerson(ICriterion... where);


	public ObservationMaster searchObservationMaster(ICriterion... where);
	public List<ObservationReported> searchObservationReportedList(ICriterion... where);
	public OrgLocation searchOrgLocation(ICriterion... where);
	public List<OrgLocation> searchOrgLocationList(ICriterion... where);
	public ModelPerson searchPerson(ICriterion... where);
	public ModelPerson searchPractitioner(ICriterion... where);

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

	IBaseBundle searchRegularRecord(Class<? extends IBaseResource> aClass, ICriterion... where);

	IBaseBundle searchGoldenRecord(Class<? extends IBaseResource> aClass, ICriterion... where);

}
