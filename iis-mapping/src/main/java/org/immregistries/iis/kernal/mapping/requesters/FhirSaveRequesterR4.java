package org.immregistries.iis.kernal.mapping.requesters;

import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.ICriterion;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.r4.model.*;
import org.immregistries.iis.kernal.mapping.MappingService;
import org.immregistries.iis.kernal.mapping.mappers.resources.*;
import org.immregistries.iis.kernal.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * DO NOT EDIT THE CONTENT OF THIS FILE
 *
 * This is a literal copy of FhirRequesterR4 except for the name and imported
 * FHIR Model package
 *
 * Please paste any new content from R4 version here to preserve similarity in
 * behavior.
 */
@Component
@Conditional(OnR4Condition.class)
public class FhirSaveRequesterR4 extends
	FhirSaveRequester<Patient, Immunization, Location, Practitioner, Observation, Person, Organization, RelatedPerson> {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	public FhirReadRequesterR4 fhirReadRequester;

	@Autowired
	private PatientMapper<Patient> patientMapper;
	@Autowired
	private ImmunizationMapper<Immunization> immunizationMapper;
	@Autowired
	private LocationMapper<Location> locationMapper;
	@Autowired
	private PractitionerMapper<Practitioner> practitionerMapper;
	@Autowired
	private ObservationMapper<Observation> observationMapper;
	@Autowired
	private MappingService mappingService;

	public PatientReported savePatientReported(PatientReported patientReported) {
		Patient patient = (Patient) mappingService.fhirResource(patientReported);
		boolean createOnly = false;
		List<ICriterion> criteria = new ArrayList<>(2);
		criteria.add(Patient.IDENTIFIER.exactly().systemAndIdentifier(
				patientReported.getMainBusinessIdentifier().getSystem(),
				patientReported.getMainBusinessIdentifier().getValue()));
		if (StringUtils.isNotBlank(patientReported.getManagingOrganizationId())) {
			criteria.add(Patient.ORGANIZATION.hasAnyOfIds(patientReported.getManagingOrganizationId()));
		} else {
			createOnly = true;
		}
		MethodOutcome outcome = save(createOnly, patient, criteria.toArray(new ICriterion[0]));
		if (!outcome.getResource().isEmpty()) {
			patientReported.setPatientId(outcome.getResource().getIdElement().getIdPart());
			return (PatientReported) mappingService.localObjectReportedWithMaster((IAnyResource) outcome.getResource());
		} else if (outcome.getCreated() != null && outcome.getCreated()) {
			patientReported.setPatientId(outcome.getId().getIdPart());
			return fhirReadRequester.readAsPatientReported(outcome.getId().getIdPart());
		} else {
			return patientReported;
		}
	}

	public ModelPerson savePractitioner(ModelPerson modelPerson) {
		Practitioner practitioner = practitionerMapper.fhirObject(modelPerson);
		MethodOutcome outcome = save(false, practitioner,
				Patient.IDENTIFIER.exactly().identifier(modelPerson.getPersonExternalLink()));
		if (outcome.getCreated() != null && outcome.getCreated()) {
			modelPerson.setPersonId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			modelPerson.setPersonId(outcome.getResource().getIdElement().getIdPart());
		}
		return modelPerson;
	}

	public ObservationReported saveObservationReported(ObservationReported observationReported) {
		Observation observation = (Observation) observationMapper.fhirObject(observationReported);
		MethodOutcome outcome = save(false, observation);
		if (outcome.getCreated() != null && outcome.getCreated()) {
			observationReported.setObservationId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			observationReported.setObservationId(outcome.getResource().getIdElement().getIdPart());
		}
		return observationReported;
	}

	public VaccinationReported saveVaccinationReported(VaccinationReported vaccinationReported) {
		Immunization immunization = (Immunization) mappingService.fhirResource(vaccinationReported);
		// TODO change conditional create to update ?
		MethodOutcome outcome = save(false, immunization
		// , Immunization.IDENTIFIER.exactly()
		// .identifier(vaccinationReported.getExternalLink())
		);
		if (outcome.getCreated() != null && outcome.getCreated()) {
			vaccinationReported.setVaccinationId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			vaccinationReported.setVaccinationId(outcome.getResource().getIdElement().getIdPart());
		}
		return vaccinationReported;
	}

	public OrgLocation saveOrgLocation(OrgLocation orgLocation) {
		Location location = locationMapper.fhirObject(orgLocation);
		MethodOutcome outcome = save(false, location,
				Location.IDENTIFIER.exactly().identifier(location.getIdentifierFirstRep().getValue()));
		if (outcome.getCreated() != null && outcome.getCreated()) {
			orgLocation.setOrgLocationId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			orgLocation.setOrgLocationId(outcome.getResource().getIdElement().getIdPart());
		}
		return orgLocation;
	}

	public Organization saveOrganization(Organization organization) {
		MethodOutcome outcome = null;
		if (organization.getIdentifierFirstRep().getValue() != null) {
			outcome = save(false, organization,
					Organization.IDENTIFIER.exactly().identifier(organization.getIdentifierFirstRep().getValue()));
		} else {
			outcome = save(false, organization);
		}

		if (!outcome.getResource().isEmpty()) {
			return (Organization) outcome.getResource();
		} else if (outcome.getCreated() != null && outcome.getCreated()) {
			organization.setId(outcome.getId().getIdPart());
			return organization;
		} else {
			return null;
		}
	}



}
