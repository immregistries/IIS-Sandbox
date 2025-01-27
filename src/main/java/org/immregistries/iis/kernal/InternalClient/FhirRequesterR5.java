package org.immregistries.iis.kernal.InternalClient;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.fhir.annotations.OnR5Condition;
import org.immregistries.iis.kernal.model.*;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.immregistries.iis.kernal.logic.IncomingMessageHandlerR5.MINIMAL_MATCHING_SCORE;

@Component
@Conditional(OnR5Condition.class)
public class FhirRequesterR5 extends FhirRequester<Patient, Immunization, Location, Practitioner, Observation, Person, Organization, RelatedPerson> {

	public PatientMaster searchPatientMaster(SearchParameterMap searchParameterMap) {
		PatientMaster patientMaster = null;
		IBundleProvider bundleProvider = searchGoldenRecord(Patient.class, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			patientMaster = patientMapper.localObject((Patient) bundleProvider.getResources(0, 1).get(0));
		}
		return patientMaster;
	}

	public PatientReported searchPatientReported(SearchParameterMap searchParameterMap) {
		PatientReported patientReported = null;
		IBundleProvider bundleProvider = searchRegularRecord(Patient.class, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			patientReported = patientMapper.localObjectReportedWithMaster((Patient) bundleProvider.getResources(0, 1).get(0));
		}
		return patientReported;
	}

	public List<PatientReported> searchPatientReportedList(SearchParameterMap searchParameterMap) {
		List<PatientReported> patientReportedList = new ArrayList<>();
		IBundleProvider bundleProvider = searchRegularRecord(Patient.class, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			for (IBaseResource resource : bundleProvider.getAllResources()) {
				patientReportedList.add(patientMapper.localObjectReportedWithMaster((Patient) resource));
			}
		}
		return patientReportedList;
	}

	public List<PatientMaster> searchPatientMasterGoldenList(SearchParameterMap searchParameterMap) {
		List<PatientMaster> patientList = new ArrayList<>();
		IBundleProvider bundleProvider = searchGoldenRecord(Patient.class, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			for (IBaseResource resource : bundleProvider.getAllResources()) {
				patientList.add(patientMapper.localObject((Patient) resource));
			}
		}
		return patientList;
	}

	public VaccinationMaster searchVaccinationMaster(SearchParameterMap searchParameterMap) {
		VaccinationMaster vaccinationMaster = null;
		IBundleProvider bundleProvider = searchGoldenRecord(Immunization.class, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			vaccinationMaster = immunizationMapper.localObject((Immunization) bundleProvider.getResources(0, 1).get(0));
		}
		return vaccinationMaster;
	}

	public VaccinationReported searchVaccinationReported(SearchParameterMap searchParameterMap) {
		VaccinationReported vaccinationReported = null;
		IBundleProvider bundleProvider = searchRegularRecord(Immunization.class, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			vaccinationReported = immunizationMapper.localObjectReportedWithMaster((Immunization) bundleProvider.getResources(0, 1).get(0));
		}
		return vaccinationReported;
	}

	public Organization searchOrganization(SearchParameterMap searchParameterMap) {
		Organization organization = null;
		IBundleProvider bundleProvider = search(Organization.class, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			organization = (Organization) bundleProvider.getResources(0, 1).get(0);
		}
		return organization;
	}

	public List<VaccinationReported> searchVaccinationReportedList(SearchParameterMap searchParameterMap) {
		List<VaccinationReported> vaccinationReportedList = new ArrayList<>();
		IBundleProvider bundleProvider = searchRegularRecord(Immunization.class, searchParameterMap);
		for (IBaseResource resource : bundleProvider.getAllResources()) {
			vaccinationReportedList.add(immunizationMapper.localObjectReportedWithMaster((Immunization) resource));
		}
		return vaccinationReportedList;
	}

	public List<VaccinationMaster> searchVaccinationListOperationEverything(String id) {
		IGenericClient client = repositoryClientFactory.getFhirClient();
		Parameters in = new Parameters()
			.addParameter("_mdm", "true")
			.addParameter("_type", "Immunization");
		Bundle bundle = client.operation()
			.onInstance("Patient/" + id)
			.named("$everything")
			.withParameters(in)
			.prettyPrint()
			.useHttpGet()
			.returnResourceType(Bundle.class).execute();
		List<VaccinationMaster> vaccinationList = new ArrayList<>();
		for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
			if (entry.getResource() instanceof Immunization) {
				if (entry.getResource().getMeta().getTag(GOLDEN_SYSTEM_TAG, GOLDEN_RECORD) != null) {
					vaccinationList.add(immunizationMapper.localObject((Immunization) entry.getResource()));
				}
			}
		}
		return vaccinationList;
	}

	public ObservationReported searchObservationReported(SearchParameterMap searchParameterMap) {
		IGenericClient fhirClient = repositoryClientFactory.getFhirClient();
		ObservationReported observationReported = null;
		IBundleProvider bundleProvider = searchRegularRecord(Observation.class, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			observationReported = observationMapper.localObjectReportedWithMaster((Observation) bundleProvider.getResources(0, 1).get(0));
		}
		return observationReported;
	}

	public ObservationMaster searchObservationMaster(SearchParameterMap searchParameterMap) {
		ObservationMaster observationMaster = null;
		IBundleProvider bundleProvider = searchGoldenRecord(Observation.class, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			observationMaster = observationMapper.localObject((Observation) bundleProvider.getResources(0, 1).get(0));
		}
		return observationMaster;
	}

	public List<ObservationReported> searchObservationReportedList(SearchParameterMap searchParameterMap) {
		IGenericClient fhirClient = repositoryClientFactory.getFhirClient();
		List<ObservationReported> observationReportedList = new ArrayList<>();
		IBundleProvider bundleProvider = search(Observation.class, searchParameterMap);
		for (IBaseResource resource : bundleProvider.getAllResources()) {
			observationReportedList.add(observationMapper.localObjectReportedWithMaster((Observation) resource));
		}
		return observationReportedList;
	}

	public OrgLocation searchOrgLocation(SearchParameterMap searchParameterMap) {
		OrgLocation orgLocation = null;
		IBundleProvider bundleProvider = search(Observation.class, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			orgLocation = locationMapper.localObject((Location) bundleProvider.getResources(0, 1).get(0));
		}
		return orgLocation;
	}

	public List<OrgLocation> searchOrgLocationList(SearchParameterMap searchParameterMap) {
		List<OrgLocation> locationList = new ArrayList<>();
		IBundleProvider bundleProvider = search(Location.class, searchParameterMap);
		for (IBaseResource resource : bundleProvider.getAllResources()) {
			locationList.add(locationMapper.localObject((Location) resource));
		}
		return locationList;
	}

	public ModelPerson searchPerson(SearchParameterMap searchParameterMap) {
		ModelPerson modelPerson = null;
		IBundleProvider bundleProvider = search(org.hl7.fhir.r5.model.Person.class, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			modelPerson = personMapper.localObject((org.hl7.fhir.r5.model.Person) bundleProvider.getResources(0, 1).get(0));
		}
		return modelPerson;
	}

	public ModelPerson searchPractitioner(SearchParameterMap searchParameterMap) {
		ModelPerson modelPerson = null;
		IBundleProvider bundleProvider = search(org.hl7.fhir.r5.model.Practitioner.class, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			modelPerson = practitionerMapper.localObject((org.hl7.fhir.r5.model.Practitioner) bundleProvider.getResources(0, 1).get(0));
		}
		return modelPerson;
	}

	public RelatedPerson searchRelatedPerson(SearchParameterMap searchParameterMap) {
		RelatedPerson relatedPerson = null;
		IBundleProvider bundleProvider = search(RelatedPerson.class, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			relatedPerson = (RelatedPerson) bundleProvider.getResources(0, 1).get(0);
		}
		return relatedPerson;
	}

	public PatientReported savePatientReported(PatientReported patientReported) {
		Patient patient = patientMapper.fhirResource(patientReported);
		MethodOutcome outcome = save(patient,
			Patient.ORGANIZATION.hasId(patientReported.getManagingOrganizationId()),
				Patient.IDENTIFIER.exactly().systemAndIdentifier(patientReported.getMainPatientIdentifier().getSystem(), patientReported.getMainPatientIdentifier().getValue()));
		if (!outcome.getResource().isEmpty()) {
			patientReported.setPatientId(outcome.getResource().getIdElement().getIdPart());
			return patientMapper.localObjectReportedWithMaster((Patient) outcome.getResource());
		} else if (outcome.getCreated() != null && outcome.getCreated()) {
			patientReported.setPatientId(outcome.getId().getIdPart());
			return readPatientReported(outcome.getId().getIdPart());
		} else {
			return patientReported;
//			return searchPatientReported(Patient.IDENTIFIER.exactly().systemAndIdentifier(patientReported.getPatientReportedAuthority(),patientReported.getPatientReportedExternalLink()));
		}
	}

	public ModelPerson savePractitioner(ModelPerson modelPerson) {
		Practitioner practitioner = practitionerMapper.fhirResource(modelPerson);
		MethodOutcome outcome = save(practitioner,
			Patient.IDENTIFIER.exactly().identifier(modelPerson.getPersonExternalLink()));
		if (outcome.getCreated() != null && outcome.getCreated()) {
			modelPerson.setPersonId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			modelPerson.setPersonId(outcome.getResource().getIdElement().getIdPart());
		}
		return modelPerson;
	}

//	public PatientReported saveRelatedPerson(PatientReported patientReported) {
//		RelatedPerson relatedPerson = relatedPersonMapper.getFhirRelatedPersonFromPatient(patientReported);
//		MethodOutcome outcome = save(relatedPerson,
//			RelatedPerson.PATIENT.hasId(patientReported.getPatientId()));
//		if (outcome.getResource() != null) {
//			relatedPersonMapper.fillGuardianInformation(patientReported, (RelatedPerson) outcome.getResource());
//		}
//		return patientReported;
//	}

	public ObservationReported saveObservationReported(ObservationReported observationReported) {
		Observation observation = observationMapper.fhirResource(observationReported);
		MethodOutcome outcome = save(observation);
		if (outcome.getCreated() != null && outcome.getCreated()) {
			observationReported.setPatientReportedId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			observationReported.setPatientReportedId(outcome.getResource().getIdElement().getIdPart());
		}
		return observationReported;
	}

	public VaccinationReported saveVaccinationReported(VaccinationReported vaccinationReported) {
		Immunization immunization = immunizationMapper.fhirResource(vaccinationReported);
		MethodOutcome outcome = save(immunization,
			Immunization.IDENTIFIER.exactly()
				.identifier(vaccinationReported.getExternalLink())
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
		MethodOutcome outcome = save(location,
			Location.IDENTIFIER.exactly().identifier(location.getIdentifierFirstRep().getValue())
		);
		if (outcome.getCreated() != null && outcome.getCreated()) {
			orgLocation.setOrgLocationId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			orgLocation.setOrgLocationId(outcome.getResource().getIdElement().getIdPart());
		}
		return orgLocation;
	}

	public Organization saveOrganization(Organization organization) {
		MethodOutcome outcome = null;
		if (StringUtils.isNotBlank(organization.getIdentifierFirstRep().getValue())) {
			outcome = save(organization,
				Organization.IDENTIFIER.exactly().identifier(organization.getIdentifierFirstRep().getValue())
			);
		} else {
			outcome = save(organization);
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

	public PatientMaster readPatientMasterWithMdmLink(String patientId) {
		Parameters out = repositoryClientFactory.getFhirClient().operation().onServer().named("$mdm-query-links")
			.withParameters(new Parameters().addParameter("resourceId", patientId)).execute();
		List<Parameters.ParametersParameterComponent> part = out.getParameter().stream()
			.filter(parametersParameterComponent -> parametersParameterComponent.getName().equals("link"))
			.findFirst().orElse(new Parameters.ParametersParameterComponent()).getPart();
		Optional<Parameters.ParametersParameterComponent> goldenIdComponent = part.stream()
			.filter(parametersParameterComponent -> parametersParameterComponent.getName().equals("goldenResourceId"))
			.findFirst();
		if (goldenIdComponent.isPresent() && !goldenIdComponent.get().getValueStringType().isEmpty()) {
			return readPatientMaster(goldenIdComponent.get().getValueStringType().getValue());
		} else {
			return null;
		}
	}

	public PatientMaster readPatientMaster(String id) {
		Patient patient = (Patient) read(Patient.class, id);
		if (FhirRequester.isGoldenRecord(patient)) {
			return patientMapper.localObject(patient);
		}
		return null;
	}

	public PatientReported readPatientReported(String id) {
		return patientMapper.localObjectReportedWithMaster((Patient) read(Patient.class, id));
	}

	public ModelPerson readPractitionerPerson(String id) {
		return practitionerMapper.localObject((Practitioner) read(Practitioner.class, id));
	}

	public OrgLocation readOrgLocation(String id) {
		return locationMapper.localObject((Location) read(Location.class, id));
	}

	public VaccinationReported readVaccinationReported(String id) {
		return immunizationMapper.localObjectReportedWithMaster((Immunization) read(Immunization.class, id));
	}

	public PatientMaster matchPatient(List<PatientReported> multipleMatches, PatientMaster patientMasterForMatchQuery, Date cutoff) {
		PatientMaster singleMatch = null;
		Bundle matches = repositoryClientFactory.getFhirClient()
			.operation()
			.onType(Patient.class)
			.named("match")
			.withParameter(Parameters.class, "resource", patientMapper.fhirResource(patientMasterForMatchQuery))
			.returnResourceType(Bundle.class).execute();
		for (Bundle.BundleEntryComponent entry : matches.getEntry()) {
			if (entry.getResource() instanceof Patient) {
				Patient patient = (Patient) entry.getResource();
				PatientMaster patientMaster = patientMapper.localObject(patient);
				/**
				 * Filter for flavours previously configured
				 */
				if (cutoff != null && cutoff.before(patientMaster.getReportedDate())) {
					break;
				}

//				/**
//				 * Filtering only Golden records
//				 * TODO ask Nathan to assert workflow
//				 */
//				if (entry.getResource().getMeta().getTag(GOLDEN_SYSTEM_TAG, GOLDEN_RECORD) == null) {
//					break;
//				}
				if (entry.getSearch().hasScore() && entry.getSearch().getScoreElement().compareTo(new DecimalType(MINIMAL_MATCHING_SCORE)) > 0) {
					singleMatch = patientMaster;
				}
				multipleMatches.add(patientMapper.localObjectReported((Patient) entry.getResource()));
			}
		}
		return singleMatch;
	}
}
