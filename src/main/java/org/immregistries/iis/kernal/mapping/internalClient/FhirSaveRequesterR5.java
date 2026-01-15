package org.immregistries.iis.kernal.mapping.internalClient;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.gclient.ICriterion;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.immregistries.iis.kernal.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
@Conditional(OnR5Condition.class)
public class FhirSaveRequesterR5 extends
        FhirSaveRequester<Patient, Immunization, Location, Practitioner, Observation, Person, Organization, RelatedPerson> {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	public FhirReadRequester fhirReadSuper;

	@Autowired
	FhirSearchRequester fhirSearchRequester;


	public Organization searchOrganization(SearchParameterMap searchParameterMap) {
		IBundleProvider bundleProvider = fhirSearchRequester.search("Organization", searchParameterMap);
		return (Organization) bundleProvider.getAllResources().stream().findFirst().orElse(null);
	}


	public RelatedPerson searchRelatedPerson(SearchParameterMap searchParameterMap) {
		RelatedPerson relatedPerson = null;
		IBundleProvider bundleProvider = fhirSearchRequester.search(RelatedPerson.class, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			relatedPerson = (RelatedPerson) bundleProvider.getResources(0, 1).get(0);
		}
		return relatedPerson;
	}

	public PatientReported savePatientReported(PatientReported patientReported) {
		Patient patient = (Patient) allMappingService.fhirResource(patientReported);
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
			return (PatientReported) allMappingService.localObjectReportedWithMaster((IAnyResource) outcome.getResource());
		} else if (outcome.getCreated() != null && outcome.getCreated()) {
			patientReported.setPatientId(outcome.getId().getIdPart());
			return fhirReadSuper.readAsPatientReported(outcome.getId().getIdPart());
		} else {
			return patientReported;
		}
	}

	public ModelPerson savePractitioner(ModelPerson modelPerson) {
		Practitioner practitioner = practitionerMapper.fhirResource(modelPerson);
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
		Observation observation = (Observation) observationMapper.fhirResource(observationReported);
		MethodOutcome outcome = save(false, observation);
		if (outcome.getCreated() != null && outcome.getCreated()) {
			observationReported.setObservationId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			observationReported.setObservationId(outcome.getResource().getIdElement().getIdPart());
		}
		return observationReported;
	}

	public VaccinationReported saveVaccinationReported(VaccinationReported vaccinationReported) {
		Immunization immunization = (Immunization) allMappingService.fhirResource(vaccinationReported);
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
		Location location = locationMapper.fhirResource(orgLocation);
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
