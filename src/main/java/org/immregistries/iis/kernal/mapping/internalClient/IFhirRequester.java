package org.immregistries.iis.kernal.mapping.internalClient;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.model.*;

import java.util.List;

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
public interface IFhirRequester<Patient extends IBaseResource, Immunization extends IBaseResource, Location extends IBaseResource, Practitioner extends IBaseResource, Observation extends IBaseResource, Person extends IBaseResource, Organization extends IBaseResource, RelatedPerson extends IBaseResource> {


	PatientReported savePatientReported(PatientReported patientReported);

	ModelPerson savePractitioner(ModelPerson modelPerson);

	ObservationReported saveObservationReported(ObservationReported observationReported);

	VaccinationReported saveVaccinationReported(VaccinationReported vaccinationReported);

	OrgLocation saveOrgLocation(OrgLocation orgLocation);

	Organization saveOrganization(Organization organization);

}
