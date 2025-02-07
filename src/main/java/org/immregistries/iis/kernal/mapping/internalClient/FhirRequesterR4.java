package org.immregistries.iis.kernal.mapping.internalClient;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
public class FhirRequesterR4 extends AbstractFhirRequester<Patient, Immunization, Location, Practitioner, Observation, Person, Organization, RelatedPerson> {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

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

	public List<VaccinationMaster> searchVaccinationMasterGoldenList(SearchParameterMap searchParameterMap) {
		List<VaccinationMaster> vaccinationMasterList = new ArrayList<>();
		IBundleProvider bundleProvider = searchGoldenRecord(Immunization.class, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			for (IBaseResource resource : bundleProvider.getAllResources()) {
				vaccinationMasterList.add(immunizationMapper.localObject((Immunization) resource));
			}
		}
		return vaccinationMasterList;
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

	public List<VaccinationMaster> searchVaccinationListOperationEverything(String patientId) {
		IGenericClient client = repositoryClientFactory.getFhirClient();
		Parameters in = new Parameters()
			.addParameter("_mdm", "true")
			.addParameter("_type", "Immunization");
		Bundle bundle = client.operation()
			.onInstance("Patient/" + patientId)
			.named("$everything")
			.withParameters(in)
			.prettyPrint()
			.useHttpGet()
			.returnResourceType(Bundle.class).execute();
		List<VaccinationMaster> vaccinationList = new ArrayList<>();
		for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
			if (entry.getResource() instanceof Immunization) {
				if (AbstractFhirRequester.isGoldenRecord(entry.getResource())) {
					VaccinationMaster vaccinationMaster = immunizationMapper.localObject((Immunization) entry.getResource());
					if (vaccinationMaster != null) {
						vaccinationList.add(vaccinationMaster);
					}
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
		IBundleProvider bundleProvider = search(Person.class, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			modelPerson = personMapper.localObject((Person) bundleProvider.getResources(0, 1).get(0));
		}
		return modelPerson;
	}

	public ModelPerson searchPractitioner(SearchParameterMap searchParameterMap) {
		ModelPerson modelPerson = null;
		IBundleProvider bundleProvider = search(Practitioner.class, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			modelPerson = practitionerMapper.localObject((Practitioner) bundleProvider.getResources(0, 1).get(0));
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
		Patient patient = patientMapper.fhirResource(patientReported);
		return save(patient,
			Patient.IDENTIFIER.exactly().systemAndIdentifier(patientReported.getMainBusinessIdentifier().getSystem(), patientReported.getMainBusinessIdentifier().getValue()));
	}

	public PatientReported savePatientReported(PatientReported patientReported) {
		MethodOutcome outcome = savePatientReportedMethodOutcome(patientReported);
//		logger.info("created {} resource {}", outcome.getCreated(), outcome.getResource());
		if (!outcome.getResource().isEmpty()) {
			patientReported.setPatientId(outcome.getResource().getIdElement().getIdPart());
			return patientMapper.localObjectReportedWithMaster((Patient) outcome.getResource());
		} else if (outcome.getCreated() != null && outcome.getCreated()) {
			patientReported.setPatientId(outcome.getId().getIdPart());
			return readAsPatientReported(outcome.getId().getIdPart());
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
			observationReported.setObservationId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			observationReported.setObservationId(outcome.getResource().getIdElement().getIdPart());
		}
		return observationReported;
	}

	public VaccinationReported saveVaccinationReported(VaccinationReported vaccinationReported) {
		Immunization immunization = immunizationMapper.fhirResource(vaccinationReported);
		// TODO change conditional create to update ?
		MethodOutcome outcome = save(immunization
//			, Immunization.IDENTIFIER.exactly()
//				.identifier(vaccinationReported.getExternalLink())
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

	public PatientMaster readAsPatientMaster(String id) {
		Patient patient = (Patient) read(Patient.class, id);
		if (AbstractFhirRequester.isGoldenRecord(patient)) {
			return patientMapper.localObject(patient);
		}
		return null;
	}

	public PatientReported readAsPatientReported(String id) {
		return patientMapper.localObjectReportedWithMaster((Patient) read(Patient.class, id));
	}

	public ModelPerson readPractitionerAsPerson(String id) {
		return practitionerMapper.localObject((Practitioner) read(Practitioner.class, id));
	}

	public OrgLocation readAsOrgLocation(String id) {
		return locationMapper.localObject((Location) read(Location.class, id));
	}

	public VaccinationReported readAsVaccinationReported(String id) {
		return immunizationMapper.localObjectReportedWithMaster((Immunization) read(Immunization.class, id));
	}

	public VaccinationMaster readAsVaccinationMaster(String id) {
		Immunization immunization = (Immunization) read(Immunization.class, id);
		if (AbstractFhirRequester.isGoldenRecord(immunization)) {
			return immunizationMapper.localObject(immunization);
		}
		return null;
	}

	public PatientMaster matchPatient(List<PatientReported> multipleMatches, PatientMaster patientMasterForMatchQuery, Date cutoff) {
		PatientMaster singleMatch = null;
		Bundle matches = repositoryClientFactory.getFhirClient()
			.operation().onType(Patient.class)
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

	public List<PatientReported> searchPatientReportedFromGoldenIdWithMdmLinks(String patientMasterId) {
		return readMdmlinksReportedIds(patientMasterId)
			.map(this::readAsPatientReported)
			.collect(Collectors.toList());
	}

	public List<VaccinationReported> searchVaccinationReportedFromGoldenIdWithMdmLinks(String vaccinationMasterId) {
		return readMdmlinksReportedIds(vaccinationMasterId)
			.map(this::readAsVaccinationReported)
			.collect(Collectors.toList());
	}

	public Stream<String> readMdmlinksReportedIds(String masterId) {
		Parameters out = repositoryClientFactory.getFhirClient().operation().onServer().named("$mdm-query-links")
			.withParameters(new Parameters().addParameter("goldenResourceId", masterId)).execute();
		Stream<Parameters.ParametersParameterComponent> links = out.getParameter().stream()
			.filter(parametersParameterComponent -> parametersParameterComponent.getName().equals("link"));
		return links
			.map(link -> link.getPart()
				.stream()
				.filter(part -> part.getName().equals("sourceResourceId"))
				.findFirst()
				.map(part -> ((StringType) part.getValue()).getValue())).flatMap(Optional::stream);
	}

	public Optional<String> readGoldenResourceId(String reportId) {
		Parameters out = repositoryClientFactory.getFhirClient().operation().onServer().named("$mdm-query-links")
			.withParameters(new Parameters().addParameter("resourceId", reportId)).execute();
		List<Parameters.ParametersParameterComponent> part = out.getParameter().stream()
			.filter(parametersParameterComponent -> parametersParameterComponent.getName().equals("link"))
			.findFirst().orElse(new Parameters.ParametersParameterComponent()).getPart();
		Optional<Parameters.ParametersParameterComponent> goldenIdComponent = part.stream()
			.filter(parametersParameterComponent -> parametersParameterComponent.getName().equals("goldenResourceId"))
			.findFirst();
		return goldenIdComponent
			.filter(component -> !component.getValue().isEmpty())
			.map(component -> String.valueOf(component.getValue()));
	}

	public PatientMaster readPatientMasterWithMdmLink(String patientId) {
		Optional<String> goldenId = readGoldenResourceId(patientId);
		return goldenId.map(this::readAsPatientMaster).orElse(null);
	}

	public VaccinationMaster readVaccinationMasterWithMdmLink(String vaccinationReportedId) {
		Optional<String> goldenId = readGoldenResourceId(vaccinationReportedId);
		return goldenId.map(this::readAsVaccinationMaster).orElse(null);
	}
}
