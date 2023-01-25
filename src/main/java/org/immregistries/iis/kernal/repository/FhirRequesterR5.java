package org.immregistries.iis.kernal.repository;

import ca.uhn.fhir.jpa.starter.annotations.OnR5Condition;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.iis.kernal.model.ModelPerson;
import org.immregistries.iis.kernal.servlet.ServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Conditional(OnR5Condition.class)
public class FhirRequesterR5 extends FhirRequester<Patient,Immunization,Location,Practitioner,Observation,Person, Organization, RelatedPerson> {
	Logger logger = LoggerFactory.getLogger(FhirRequesterR5.class);

	public PatientMaster searchPatientMaster(ICriterion... where) {
		PatientMaster patientMaster = null;
		Bundle bundle = (Bundle) searchGoldenRecord(Patient.class, where);
		if (bundle != null && bundle.hasEntry()) {
			patientMaster = patientMapper.getMaster((Patient) bundle.getEntryFirstRep().getResource());
		}
		return patientMaster;
	}

	public PatientReported searchPatientReported(ICriterion... where) {
		PatientReported patientReported = null;
		Bundle bundle = (Bundle) searchRegularRecord(Patient.class, where);
		if (bundle != null && bundle.hasEntry()) {
			patientReported = patientMapper.getReportedWithMaster((Patient) bundle.getEntryFirstRep().getResource());
		}
		return patientReported;
	}

	public List<PatientReported> searchPatientReportedList(ICriterion... where) {
		List<PatientReported> patientReportedList = new ArrayList<>();
		Bundle bundle = (Bundle) searchRegularRecord(Patient.class, where);
		if (bundle != null) {
			for (Bundle.BundleEntryComponent entry: bundle.getEntry()) {
				patientReportedList.add(patientMapper.getReportedWithMaster((Patient) entry.getResource()));
			}
		}
		return patientReportedList;
	}

	public VaccinationMaster searchVaccinationMaster(ICriterion... where) {
		VaccinationMaster vaccinationMaster = null;
		Bundle bundle = (Bundle) searchGoldenRecord(Immunization.class, where);
		if (bundle != null && bundle.hasEntry()) {
			vaccinationMaster = immunizationMapper.getMaster((Immunization) bundle.getEntryFirstRep().getResource());
		}
		return vaccinationMaster;
	}

	public VaccinationReported searchVaccinationReported(ICriterion... where) {
		VaccinationReported vaccinationReported = null;
		Bundle bundle = (Bundle) searchRegularRecord(Immunization.class, where);
		if (bundle != null && bundle.hasEntry()) {
			vaccinationReported = immunizationMapper.getReportedWithMaster((Immunization) bundle.getEntryFirstRep().getResource());
		}
		return vaccinationReported;
	}

	public Organization searchOrganization(ICriterion... where) {
		Organization organization = null;
		Bundle bundle = (Bundle) search(Organization.class, where);
		if (bundle != null && bundle.hasEntry()) {
			organization = (Organization) bundle.getEntryFirstRep().getResource();
		}
		return organization;
	}

	public List<VaccinationReported> searchVaccinationReportedList(ICriterion... where) {
		List<VaccinationReported> vaccinationReportedList = new ArrayList<>();
		Bundle bundle = (Bundle) searchRegularRecord(Immunization.class, where);
		for (Bundle.BundleEntryComponent entry: bundle.getEntry()) {
			vaccinationReportedList.add(immunizationMapper.getReportedWithMaster((Immunization) entry.getResource()));
		}
		return vaccinationReportedList;
	}

	public ObservationReported searchObservationReported(ICriterion... where) {
		IGenericClient fhirClient = repositoryClientFactory.getFhirClientFromSession();
		ObservationReported observationReported = null;
		Bundle bundle = (Bundle) searchRegularRecord(Observation.class, where);
		if (bundle != null && bundle.hasEntry()) {
			observationReported = observationMapper.getReportedWithMaster((Observation) bundle.getEntryFirstRep().getResource(),this,fhirClient);
		}
		return observationReported;
	}

	public ObservationMaster searchObservationMaster(ICriterion... where) {
		ObservationMaster observationMaster = null;
		Bundle bundle = (Bundle) searchGoldenRecord(Observation.class, where);
		if (bundle != null && bundle.hasEntry()) {
			observationMaster = observationMapper.getMaster((Observation) bundle.getEntryFirstRep().getResource());
		}
		return observationMaster;
	}

	public List<ObservationReported> searchObservationReportedList(ICriterion... where) {
		IGenericClient fhirClient = repositoryClientFactory.getFhirClientFromSession();
		List<ObservationReported> observationReportedList = new ArrayList<>();
		Bundle bundle = (Bundle) search(Observation.class, where);
		for (Bundle.BundleEntryComponent entry: bundle.getEntry()) {
			observationReportedList.add(observationMapper.getReportedWithMaster((Observation) entry.getResource(),this,fhirClient));
		}
		return observationReportedList;
	}

	public OrgLocation searchOrgLocation(ICriterion... where) {
		OrgLocation orgLocation = null;
		Bundle bundle = (Bundle) search(Observation.class, where);
		if (bundle != null && bundle.hasEntry()) {
			orgLocation = locationMapper.orgLocationFromFhir((Location) bundle.getEntryFirstRep().getResource());
		}
		return orgLocation;
	}

	public List<OrgLocation> searchOrgLocationList(ICriterion... where) {
		List<OrgLocation> locationList = new ArrayList<>();
		Bundle bundle = (Bundle) search(Location.class, where);
		for (Bundle.BundleEntryComponent entry: bundle.getEntry()) {
			locationList.add(locationMapper.orgLocationFromFhir((Location) entry.getResource()));
		}
		return locationList;
	}

	public ModelPerson searchPerson(ICriterion... where) {
		ModelPerson modelPerson = null;
		Bundle bundle = (Bundle) search(org.hl7.fhir.r5.model.Person.class, where);
		if (bundle != null && bundle.hasEntry()) {
			modelPerson = personMapper.getModelPerson((org.hl7.fhir.r5.model.Person) bundle.getEntryFirstRep().getResource());
		}
		return modelPerson;
	}

	public ModelPerson searchPractitioner(ICriterion... where) {
		ModelPerson modelPerson = null;
		Bundle bundle = (Bundle) search(org.hl7.fhir.r5.model.Practitioner.class, where);
		if (bundle != null && bundle.hasEntry()) {
			modelPerson = practitionerMapper.getModelPerson((org.hl7.fhir.r5.model.Practitioner) bundle.getEntryFirstRep().getResource());
		}
		return modelPerson;
	}

	public RelatedPerson searchRelatedPerson(ICriterion... where) {
		RelatedPerson relatedPerson = null;
		Bundle bundle = (Bundle) search(RelatedPerson.class, where);
		if (bundle != null && bundle.hasEntry()) {
			relatedPerson = (RelatedPerson) bundle.getEntryFirstRep().getResource();
		}
		return relatedPerson;
	}

	public PatientReported savePatientReported(PatientReported patientReported) {
		Patient patient = (Patient) patientMapper.getFhirResource(patientReported);
		MethodOutcome outcome = save(patient,
			Patient.IDENTIFIER.exactly().systemAndIdentifier(patientReported.getPatientReportedAuthority(),patientReported.getPatientReportedExternalLink()));
		if (!outcome.getResource().isEmpty()) {
			patientReported.setId(outcome.getResource().getIdElement().getIdPart());
			return patientMapper.getReportedWithMaster((Patient) outcome.getResource());
		} else if (outcome.getCreated() != null && outcome.getCreated()) {
			patientReported.setId(outcome.getId().getIdPart());
			return readPatientReported(outcome.getId().getIdPart());
		} else {
			return patientReported;
//			return searchPatientReported(Patient.IDENTIFIER.exactly().systemAndIdentifier(patientReported.getPatientReportedAuthority(),patientReported.getPatientReportedExternalLink()));
		}
	}

	public ModelPerson savePractitioner(ModelPerson modelPerson) {
		Practitioner practitioner = practitionerMapper.getFhirResource(modelPerson);
		MethodOutcome outcome = save(practitioner,
			Patient.IDENTIFIER.exactly().identifier(modelPerson.getPersonExternalLink()));
		if (outcome.getCreated() != null && outcome.getCreated()) {
			modelPerson.setPersonId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			modelPerson.setPersonId(outcome.getResource().getIdElement().getIdPart());
		}
		return modelPerson;
	}

	public PatientReported saveRelatedPerson(PatientReported patientReported) {
		RelatedPerson relatedPerson = (RelatedPerson) relatedPersonMapper.getFhirRelatedPersonFromPatient(patientReported);
		MethodOutcome outcome = save(relatedPerson,
			RelatedPerson.PATIENT.hasId(patientReported.getId()));
		if (outcome.getResource() != null)  {
			patientReported = relatedPersonMapper.fillGuardianInformation(patientReported, (RelatedPerson) outcome.getResource());
		}
		return patientReported;
	}

	public ObservationReported saveObservationReported(ObservationReported observationReported) {
		Observation observation = observationMapper.getFhirResource(observationReported);
		MethodOutcome outcome = save(observation);
		if (outcome.getCreated() != null && outcome.getCreated()) {
			observationReported.setPatientReportedId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			observationReported.setPatientReportedId(outcome.getResource().getIdElement().getIdPart());
		}
		return observationReported;
	}

	public VaccinationReported saveVaccinationReported(VaccinationReported vaccinationReported) {
		Immunization immunization =  (Immunization) immunizationMapper.getFhirResource(vaccinationReported);
		MethodOutcome outcome = save(immunization,
			Immunization.IDENTIFIER.exactly()
				.identifier(vaccinationReported.getVaccinationReportedExternalLink())
		);
		if (outcome.getCreated() != null && outcome.getCreated()){
			vaccinationReported.setVaccinationReportedId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			vaccinationReported.setVaccinationReportedId(outcome.getResource().getIdElement().getIdPart());
		}
		return vaccinationReported;
	}

	public OrgLocation saveOrgLocation(OrgLocation orgLocation) {
		Location location = locationMapper.getFhirResource(orgLocation);
		MethodOutcome outcome = save(location,
			Location.IDENTIFIER.exactly().identifier(location.getIdentifierFirstRep().getValue())
			);
		if (outcome.getCreated() != null && outcome.getCreated()){
			orgLocation.setOrgLocationId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			orgLocation.setOrgLocationId(outcome.getResource().getIdElement().getIdPart());
		}
		return orgLocation;
	}

	public Organization saveOrganization(Organization organization) {
		MethodOutcome outcome = save(organization,
			Organization.IDENTIFIER.exactly().identifier(organization.getIdentifierFirstRep().getValue())
			);
		if (!outcome.getResource().isEmpty()) {
			return (Organization) outcome.getResource();
		} else if (outcome.getCreated() != null && outcome.getCreated()){
			organization.setId(outcome.getId().getIdPart());
			return organization;
		} else {
			return null;
		}
	}

	public PatientMaster readPatientMaster(String id) {
		return patientMapper.getMaster((Patient) read(Patient.class,id));
	}

	public PatientReported readPatientReported(String id) {
		return patientMapper.getReportedWithMaster((Patient) read(Patient.class, id));
	}

	public ModelPerson readPractitionerPerson(String id) {
		return practitionerMapper.getModelPerson((Practitioner) read(Practitioner.class,id));
	}

	public OrgLocation readOrgLocation(String id) {
		return locationMapper.orgLocationFromFhir((Location) read(Location.class,id));
	}

	public VaccinationReported readVaccinationReported(String id) {
		return immunizationMapper.getReportedWithMaster((Immunization) read(Immunization.class, id));
	}
}
