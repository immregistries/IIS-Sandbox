package org.immregistries.iis.kernal.mapping.internalClient;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.model.*;

/**
 * Helping service to execute Queries alongside fhir mapping
 * 
 * @param <Patient>
 * @param <Immunization>
 * @param <Location>
 * @param <Practitioner>
 * @param <Observation>
 * @param <Person>
 * @param <Organization>
 * @param <RelatedPerson>
 */
public interface IFhirRequester<Patient extends IAnyResource, Immunization extends IAnyResource, Location extends IAnyResource, Practitioner extends IAnyResource, Observation extends IAnyResource, Person extends IAnyResource, Organization extends IAnyResource, RelatedPerson extends IAnyResource> {


	PatientReported savePatientReported(PatientReported patientReported);

	ModelPerson savePractitioner(ModelPerson modelPerson);

	ObservationReported saveObservationReported(ObservationReported observationReported);

	VaccinationReported saveVaccinationReported(VaccinationReported vaccinationReported);

	OrgLocation saveOrgLocation(OrgLocation orgLocation);

	Organization saveOrganization(Organization organization);

}
