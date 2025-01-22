package org.immregistries.iis.kernal.InternalClient;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.immregistries.iis.kernal.fhir.annotations.OnR4Condition;
import org.immregistries.iis.kernal.model.*;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.immregistries.iis.kernal.logic.IncomingMessageHandler.MINIMAL_MATCHING_SCORE;


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

	public PatientMaster searchPatientMaster(SearchParameterMap searchParameterMap) {
		PatientMaster patientMaster = null;
		IBundleProvider bundleProvider = searchGoldenRecord(Patient.class, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			patientMaster = patientMapper.getMaster((Patient) bundleProvider.getResources(0, 1).get(0));
		}
		return patientMaster;
	}

	public PatientReported searchPatientReported(SearchParameterMap searchParameterMap) {
		PatientReported patientReported = null;
		IBundleProvider bundleProvider = searchRegularRecord(Patient.class, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			patientReported = patientMapper.getReportedWithMaster((Patient) bundleProvider.getResources(0, 1).get(0));
		}
		return patientReported;
	}

	public List<PatientReported> searchPatientReportedList(SearchParameterMap searchParameterMap) {
		List<PatientReported> patientReportedList = new ArrayList<>();
		IBundleProvider bundleProvider = searchRegularRecord(Patient.class, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			for (IBaseResource resource : bundleProvider.getAllResources()) {
				patientReportedList.add(patientMapper.getReportedWithMaster((Patient) resource));
			}
		}
		return patientReportedList;
	}

	public List<PatientMaster> searchPatientMasterGoldenList(SearchParameterMap searchParameterMap) {
		List<PatientMaster> patientList = new ArrayList<>();
		IBundleProvider bundleProvider = searchGoldenRecord(Patient.class, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			for (IBaseResource resource : bundleProvider.getAllResources()) {
				patientList.add(patientMapper.getMaster((Patient) resource));
			}
		}
		return patientList;
	}

	public VaccinationMaster searchVaccinationMaster(SearchParameterMap searchParameterMap) {
		VaccinationMaster vaccinationMaster = null;
		IBundleProvider bundleProvider = searchGoldenRecord(Immunization.class, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			vaccinationMaster = immunizationMapper.getMaster((Immunization) bundleProvider.getResources(0, 1).get(0));
		}
		return vaccinationMaster;
	}

	public VaccinationReported searchVaccinationReported(SearchParameterMap searchParameterMap) {
		VaccinationReported vaccinationReported = null;
		IBundleProvider bundleProvider = searchRegularRecord(Immunization.class, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			vaccinationReported = immunizationMapper.getReportedWithMaster((Immunization) bundleProvider.getResources(0, 1).get(0));
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
			vaccinationReportedList.add(immunizationMapper.getReportedWithMaster((Immunization) resource));
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
					vaccinationList.add(immunizationMapper.getMaster((Immunization) entry.getResource()));
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
			observationReported = observationMapper.getReportedWithMaster((Observation) bundleProvider.getResources(0, 1).get(0),  fhirClient);
		}
		return observationReported;
	}

	public ObservationMaster searchObservationMaster(SearchParameterMap searchParameterMap) {
		ObservationMaster observationMaster = null;
		IBundleProvider bundleProvider = searchGoldenRecord(Observation.class, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			observationMaster = observationMapper.getMaster((Observation) bundleProvider.getResources(0, 1).get(0));
		}
		return observationMaster;
	}

	public List<ObservationReported> searchObservationReportedList(SearchParameterMap searchParameterMap) {
		IGenericClient fhirClient = repositoryClientFactory.getFhirClient();
		List<ObservationReported> observationReportedList = new ArrayList<>();
		IBundleProvider bundleProvider = search(Observation.class, searchParameterMap);
		for (IBaseResource resource : bundleProvider.getAllResources()) {
			observationReportedList.add(observationMapper.getReportedWithMaster((Observation) resource, fhirClient));
		}
		return observationReportedList;
	}

	public OrgLocation searchOrgLocation(SearchParameterMap searchParameterMap) {
		OrgLocation orgLocation = null;
		IBundleProvider bundleProvider = search(Observation.class, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			orgLocation = locationMapper.orgLocationFromFhir((Location) bundleProvider.getResources(0, 1).get(0));
		}
		return orgLocation;
	}

	public List<OrgLocation> searchOrgLocationList(SearchParameterMap searchParameterMap) {
		List<OrgLocation> locationList = new ArrayList<>();
		IBundleProvider bundleProvider = search(Location.class, searchParameterMap);
		for (IBaseResource resource : bundleProvider.getAllResources()) {
			locationList.add(locationMapper.orgLocationFromFhir((Location) resource));
		}
		return locationList;
	}

	public ModelPerson searchPerson(SearchParameterMap searchParameterMap) {
		ModelPerson modelPerson = null;
		IBundleProvider bundleProvider = search(Person.class, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			modelPerson = personMapper.getModelPerson((Person) bundleProvider.getResources(0, 1).get(0));
		}
		return modelPerson;
	}

	public ModelPerson searchPractitioner(SearchParameterMap searchParameterMap) {
		ModelPerson modelPerson = null;
		IBundleProvider bundleProvider = search(Practitioner.class, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			modelPerson = practitionerMapper.getModelPerson((Practitioner) bundleProvider.getResources(0, 1).get(0));
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

	public MethodOutcome savePatientReportedMethodOutcome(PatientReported patientReported) {
		Patient patient = patientMapper.getFhirResource(patientReported);
		return save(patient,
				Patient.IDENTIFIER.exactly().systemAndIdentifier(patientReported.getMainPatientIdentifier().getSystem(), patientReported.getMainPatientIdentifier().getValue()));
	}

	public PatientReported savePatientReported(PatientReported patientReported) {
		MethodOutcome outcome = savePatientReportedMethodOutcome(patientReported);
		logger.info("created {} resource {}", outcome.getCreated(), outcome.getResource());
		if (!outcome.getResource().isEmpty()) {
			patientReported.setPatientId(outcome.getResource().getIdElement().getIdPart());
			PatientReported patientReported1 = patientMapper.getReported((Patient) outcome.getResource());
			logger.info("test {} {} {}", patientReported1.toString().compareTo(patientReported.getPatient().toString()), patientReported.getPatient(), patientReported1);
			return patientMapper.getReportedWithMaster((Patient) outcome.getResource());
		} else if (outcome.getCreated() != null && outcome.getCreated()) {
			patientReported.setPatientId(outcome.getId().getIdPart());
			PatientReported patientReported1 = patientMapper.getReported((Patient) outcome.getResource());
			logger.info("test {} {} {}", patientReported1.toString().compareTo(patientReported.getPatient().toString()), patientReported.getPatient(), patientReported1);
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
		Immunization immunization = immunizationMapper.getFhirResource(vaccinationReported);
		// TODO change conditional create to update ?
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
		Location location = locationMapper.getFhirResource(orgLocation);
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
		if (organization.getIdentifierFirstRep().getValue() != null) {
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
		if (goldenIdComponent.isPresent() && !goldenIdComponent.get().getValue().isEmpty()) {
			return readPatientMaster(String.valueOf(goldenIdComponent.get().getValue()));
		} else {
			return null;
		}
	}

	public PatientMaster readPatientMaster(String id) {
		return patientMapper.getMaster((Patient) read(Patient.class, id)); // TODO filter if golden record ?
	}

	public PatientReported readPatientReported(String id) {
		return patientMapper.getReportedWithMaster((Patient) read(Patient.class, id));
	}

	public ModelPerson readPractitionerPerson(String id) {
		return practitionerMapper.getModelPerson((Practitioner) read(Practitioner.class, id));
	}

	public OrgLocation readOrgLocation(String id) {
		return locationMapper.orgLocationFromFhir((Location) read(Location.class, id));
	}

	public VaccinationReported readVaccinationReported(String id) {
		return immunizationMapper.getReportedWithMaster((Immunization) read(Immunization.class, id));
	}

	public PatientMaster matchPatient(List<PatientReported> multipleMatches, PatientMaster patientMasterForMatchQuery, Date cutoff) {
		PatientMaster singleMatch = null;
		Bundle matches = repositoryClientFactory.getFhirClient()
			.operation().onType(Patient.class)
			.named("match")
			.withParameter(Parameters.class, "resource", patientMapper.getFhirResource(patientMasterForMatchQuery))
			.returnResourceType(Bundle.class).execute();
		for (Bundle.BundleEntryComponent entry : matches.getEntry()) {
			if (entry.getResource() instanceof Patient) {
				Patient patient = (Patient) entry.getResource();
				PatientMaster patientMaster = patientMapper.getMaster(patient);
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
				multipleMatches.add(patientMapper.getReported((Patient) entry.getResource()));
			}
		}
		return singleMatch;
	}
}
