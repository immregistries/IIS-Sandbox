package org.immregistries.iis.kernal.repository;

import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import org.hl7.fhir.r4.model.*;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.iis.kernal.servlet.ServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


/**
 * DO NOT EDIT THE CONTENT OF THIS FILE
 *
 * This is a literal copy of FhirRequesterR4 except for the name and imported FHIR Model package
 *
 * Please paste any new content from R4 version here to preserve similarity in behavior.
 */
@Component
@Conditional(OnR4Condition.class)
public class FhirRequesterR4 extends FhirRequester<Patient,Immunization,Location,Practitioner,Observation,Person,Organization,RelatedPerson> {
	Logger logger = LoggerFactory.getLogger(FhirRequesterR4.class);

	public PatientMaster searchPatientMaster(ICriterion... where) {
		PatientMaster patientMaster = null;
		org.hl7.fhir.r4.model.Bundle bundle = (org.hl7.fhir.r4.model.Bundle) searchGoldenRecord(org.hl7.fhir.r4.model.Patient.class, where);
		if (bundle != null && bundle.hasEntry()) {
			patientMaster = patientMapper.getMaster((org.hl7.fhir.r4.model.Patient) bundle.getEntryFirstRep().getResource());
		}
		return patientMaster;
	}

	public PatientReported searchPatientReported(ICriterion... where) {
		PatientReported patientReported = null;
		org.hl7.fhir.r4.model.Bundle bundle = (org.hl7.fhir.r4.model.Bundle) searchRegularRecord(org.hl7.fhir.r4.model.Patient.class, where);
		if (bundle != null && bundle.hasEntry()) {
			patientReported = patientMapper.getReportedWithMaster((org.hl7.fhir.r4.model.Patient) bundle.getEntryFirstRep().getResource());
		}
		return patientReported;
	}

	public List<PatientReported> searchPatientReportedList(ICriterion... where) {
		List<PatientReported> patientReportedList = new ArrayList<>();
		org.hl7.fhir.r4.model.Bundle bundle = (org.hl7.fhir.r4.model.Bundle) searchRegularRecord(org.hl7.fhir.r4.model.Patient.class, where);
		if (bundle != null) {
			for (org.hl7.fhir.r4.model.Bundle.BundleEntryComponent entry: bundle.getEntry()) {
				patientReportedList.add(patientMapper.getReportedWithMaster((org.hl7.fhir.r4.model.Patient) entry.getResource()));
			}
		}
		return patientReportedList;
	}

	public VaccinationMaster searchVaccinationMaster(ICriterion... where) {
		VaccinationMaster vaccinationMaster = null;
		org.hl7.fhir.r4.model.Bundle bundle = (org.hl7.fhir.r4.model.Bundle) searchGoldenRecord(org.hl7.fhir.r4.model.Immunization.class, where);
		if (bundle != null && bundle.hasEntry()) {
			vaccinationMaster = immunizationMapper.getMaster((org.hl7.fhir.r4.model.Immunization) bundle.getEntryFirstRep().getResource());
		}
		return vaccinationMaster;
	}

	public VaccinationReported searchVaccinationReported(ICriterion... where) {
		VaccinationReported vaccinationReported = null;
		org.hl7.fhir.r4.model.Bundle bundle = (org.hl7.fhir.r4.model.Bundle) searchRegularRecord(org.hl7.fhir.r4.model.Immunization.class, where);
		if (bundle != null && bundle.hasEntry()) {
			vaccinationReported = immunizationMapper.getReportedWithMaster((org.hl7.fhir.r4.model.Immunization) bundle.getEntryFirstRep().getResource());
		}
		return vaccinationReported;
	}

	public org.hl7.fhir.r4.model.Organization searchOrganization(ICriterion... where) {
		org.hl7.fhir.r4.model.Organization organization = null;
		org.hl7.fhir.r4.model.Bundle bundle = (org.hl7.fhir.r4.model.Bundle) search(org.hl7.fhir.r4.model.Organization.class, where);
		if (bundle != null && bundle.hasEntry()) {
			organization = (org.hl7.fhir.r4.model.Organization) bundle.getEntryFirstRep().getResource();
		}
		return organization;
	}

	public List<VaccinationReported> searchVaccinationReportedList(ICriterion... where) {
		List<VaccinationReported> vaccinationReportedList = new ArrayList<>();
		org.hl7.fhir.r4.model.Bundle bundle = (org.hl7.fhir.r4.model.Bundle) searchRegularRecord(org.hl7.fhir.r4.model.Immunization.class, where);
		for (org.hl7.fhir.r4.model.Bundle.BundleEntryComponent entry: bundle.getEntry()) {
			vaccinationReportedList.add(immunizationMapper.getReportedWithMaster((org.hl7.fhir.r4.model.Immunization) entry.getResource()));
		}
		return vaccinationReportedList;
	}

	public ObservationReported searchObservationReported(ICriterion... where) {
		IGenericClient fhirClient = ServletHelper.getFhirClient(repositoryClientFactory);
		ObservationReported observationReported = null;
		org.hl7.fhir.r4.model.Bundle bundle = (org.hl7.fhir.r4.model.Bundle) searchRegularRecord(org.hl7.fhir.r4.model.Observation.class, where);
		if (bundle != null && bundle.hasEntry()) {
			observationReported = observationMapper.getReportedWithMaster((org.hl7.fhir.r4.model.Observation) bundle.getEntryFirstRep().getResource(),this,fhirClient);
		}
		return observationReported;
	}

	public ObservationMaster searchObservationMaster(ICriterion... where) {
		ObservationMaster observationMaster = null;
		org.hl7.fhir.r4.model.Bundle bundle = (org.hl7.fhir.r4.model.Bundle) searchGoldenRecord(org.hl7.fhir.r4.model.Observation.class, where);
		if (bundle != null && bundle.hasEntry()) {
			observationMaster = observationMapper.getMaster((org.hl7.fhir.r4.model.Observation) bundle.getEntryFirstRep().getResource());
		}
		return observationMaster;
	}

	public List<ObservationReported> searchObservationReportedList(ICriterion... where) {
		IGenericClient fhirClient = ServletHelper.getFhirClient(repositoryClientFactory);
		List<ObservationReported> observationReportedList = new ArrayList<>();
		org.hl7.fhir.r4.model.Bundle bundle = (org.hl7.fhir.r4.model.Bundle) search(org.hl7.fhir.r4.model.Observation.class, where);
		for (org.hl7.fhir.r4.model.Bundle.BundleEntryComponent entry: bundle.getEntry()) {
			observationReportedList.add(observationMapper.getReportedWithMaster((org.hl7.fhir.r4.model.Observation) entry.getResource(),this,fhirClient));
		}
		return observationReportedList;
	}

	public OrgLocation searchOrgLocation(ICriterion... where) {
		OrgLocation orgLocation = null;
		org.hl7.fhir.r4.model.Bundle bundle = (org.hl7.fhir.r4.model.Bundle) search(org.hl7.fhir.r4.model.Observation.class, where);
		if (bundle != null && bundle.hasEntry()) {
			orgLocation = locationMapper.orgLocationFromFhir((org.hl7.fhir.r4.model.Location) bundle.getEntryFirstRep().getResource());
		}
		return orgLocation;
	}

	public List<OrgLocation> searchOrgLocationList(ICriterion... where) {
		List<OrgLocation> locationList = new ArrayList<>();
		org.hl7.fhir.r4.model.Bundle bundle = (org.hl7.fhir.r4.model.Bundle) search(org.hl7.fhir.r4.model.Location.class, where);
		for (org.hl7.fhir.r4.model.Bundle.BundleEntryComponent entry: bundle.getEntry()) {
			locationList.add(locationMapper.orgLocationFromFhir((org.hl7.fhir.r4.model.Location) entry.getResource()));
		}
		return locationList;
	}

	public ModelPerson searchPerson(ICriterion... where) {
		ModelPerson modelPerson = null;
		org.hl7.fhir.r4.model.Bundle bundle = (org.hl7.fhir.r4.model.Bundle) search(org.hl7.fhir.r4.model.Person.class, where);
		if (bundle != null && bundle.hasEntry()) {
			modelPerson = personMapper.getModelPerson((org.hl7.fhir.r4.model.Person) bundle.getEntryFirstRep().getResource());
		}
		return modelPerson;
	}

	public ModelPerson searchPractitioner(ICriterion... where) {
		ModelPerson modelPerson = null;
		org.hl7.fhir.r4.model.Bundle bundle = (org.hl7.fhir.r4.model.Bundle) search(org.hl7.fhir.r4.model.Practitioner.class, where);
		if (bundle != null && bundle.hasEntry()) {
			modelPerson = practitionerMapper.getModelPerson((org.hl7.fhir.r4.model.Practitioner) bundle.getEntryFirstRep().getResource());
		}
		return modelPerson;
	}

	public org.hl7.fhir.r4.model.RelatedPerson searchRelatedPerson(ICriterion... where) {
		org.hl7.fhir.r4.model.RelatedPerson relatedPerson = null;
		org.hl7.fhir.r4.model.Bundle bundle = (org.hl7.fhir.r4.model.Bundle) search(org.hl7.fhir.r4.model.RelatedPerson.class, where);
		if (bundle != null && bundle.hasEntry()) {
			relatedPerson = (org.hl7.fhir.r4.model.RelatedPerson) bundle.getEntryFirstRep().getResource();
		}
		return relatedPerson;
	}

	public PatientReported savePatientReported(PatientReported patientReported) {
		org.hl7.fhir.r4.model.Patient patient = (org.hl7.fhir.r4.model.Patient) patientMapper.getFhirResource(patientReported);
		MethodOutcome outcome = save(patient,
			org.hl7.fhir.r4.model.Patient.IDENTIFIER.exactly().systemAndIdentifier(patientReported.getPatientReportedAuthority(),patientReported.getPatientReportedExternalLink()));
		if (!outcome.getResource().isEmpty()) {
			patientReported.setId(outcome.getResource().getIdElement().getIdPart());
			return patientMapper.getReportedWithMaster((org.hl7.fhir.r4.model.Patient) outcome.getResource());
		} else if (outcome.getCreated() != null && outcome.getCreated()) {
			patientReported.setId(outcome.getId().getIdPart());
		}
//		return patientReported;
		return searchPatientReported(org.hl7.fhir.r4.model.Patient.IDENTIFIER.exactly().systemAndIdentifier(patientReported.getPatientReportedAuthority(),patientReported.getPatientReportedExternalLink()));
	}

	public ModelPerson savePractitioner(ModelPerson modelPerson) {
		org.hl7.fhir.r4.model.Practitioner practitioner = practitionerMapper.getFhirResource(modelPerson);
		MethodOutcome outcome = save(practitioner,
			org.hl7.fhir.r4.model.Patient.IDENTIFIER.exactly().identifier(modelPerson.getPersonExternalLink()));
		if (outcome.getCreated() != null && outcome.getCreated()) {
			modelPerson.setPersonId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			modelPerson.setPersonId(outcome.getResource().getIdElement().getIdPart());
		}
		return modelPerson;
	}

	public PatientReported saveRelatedPerson(PatientReported patientReported) {
		org.hl7.fhir.r4.model.RelatedPerson relatedPerson = (org.hl7.fhir.r4.model.RelatedPerson) relatedPersonMapper.getFhirRelatedPersonFromPatient(patientReported);
		MethodOutcome outcome = save(relatedPerson,
			org.hl7.fhir.r4.model.RelatedPerson.PATIENT.hasId(patientReported.getId()));
		if (outcome.getResource() != null)  {
			patientReported = relatedPersonMapper.fillGuardianInformation(patientReported, (org.hl7.fhir.r4.model.RelatedPerson) outcome.getResource());
		}
		return patientReported;
	}

	public ObservationReported saveObservationReported(ObservationReported observationReported) {
		org.hl7.fhir.r4.model.Observation observation = observationMapper.getFhirResource(observationReported);
		MethodOutcome outcome = save(observation);
		if (outcome.getCreated() != null && outcome.getCreated()) {
			observationReported.setPatientReportedId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			observationReported.setPatientReportedId(outcome.getResource().getIdElement().getIdPart());
		}
		return observationReported;
	}

	public VaccinationReported saveVaccinationReported(VaccinationReported vaccinationReported) {
		org.hl7.fhir.r4.model.Immunization immunization =  (org.hl7.fhir.r4.model.Immunization) immunizationMapper.getFhirResource(vaccinationReported);
		MethodOutcome outcome = save(immunization,
			org.hl7.fhir.r4.model.Immunization.IDENTIFIER.exactly()
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
		org.hl7.fhir.r4.model.Location location = locationMapper.getFhirResource(orgLocation);
		MethodOutcome outcome = save(location,
			org.hl7.fhir.r4.model.Location.IDENTIFIER.exactly().identifier(location.getIdentifierFirstRep().getValue())
		);
		if (outcome.getCreated() != null && outcome.getCreated()){
			orgLocation.setOrgLocationId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			orgLocation.setOrgLocationId(outcome.getResource().getIdElement().getIdPart());
		}
		return orgLocation;
	}

	public org.hl7.fhir.r4.model.Organization saveOrganization(org.hl7.fhir.r4.model.Organization organization) {
		MethodOutcome outcome = save(organization,
			org.hl7.fhir.r4.model.Organization.IDENTIFIER.exactly().identifier(organization.getIdentifierFirstRep().getValue())
		);
		if (!outcome.getResource().isEmpty()) {
			return (org.hl7.fhir.r4.model.Organization) outcome.getResource();
		} else if (outcome.getCreated() != null && outcome.getCreated()){
			organization.setId(outcome.getId().getIdPart());
			return organization;
		} else {
			return null;
		}
	}

	public PatientMaster readPatientMaster(String id) {
		return patientMapper.getMaster((org.hl7.fhir.r4.model.Patient) read(org.hl7.fhir.r4.model.Patient.class,id));
	}

	public PatientReported readPatientReported(String id) {
		return patientMapper.getReported((org.hl7.fhir.r4.model.Patient) read(org.hl7.fhir.r4.model.Patient.class,id));
	}

	public ModelPerson readPractitionerPerson(String id) {
		return practitionerMapper.getModelPerson((org.hl7.fhir.r4.model.Practitioner) read(org.hl7.fhir.r4.model.Practitioner.class,id));
	}

	public OrgLocation readOrgLocation(String id) {
		return locationMapper.orgLocationFromFhir((org.hl7.fhir.r4.model.Location) read(org.hl7.fhir.r4.model.Location.class,id));
	}

	public VaccinationReported readVaccinationReported(String id) {
		return immunizationMapper.getReported((org.hl7.fhir.r4.model.Immunization) read(org.hl7.fhir.r4.model.Immunization.class,id));
	}
}
