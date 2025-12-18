package org.immregistries.iis.kernal.mapping.internalClient;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.mapping.interfaces.*;
import org.immregistries.iis.kernal.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.immregistries.iis.kernal.logic.IIncomingMessageHandler.MINIMAL_MATCHING_SCORE;
import static org.immregistries.iis.kernal.mapping.interfaces.ImmunizationMapper.IMMUNIZATION;

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
public class FhirRequesterR4 extends
		AbstractFhirRequester<Patient, Immunization, Location, Practitioner, Observation, Person, Organization, RelatedPerson> {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());


	public List<PatientReported> searchPatientReportedList(SearchParameterMap searchParameterMap) {
		List<PatientReported> patientReportedList = new ArrayList<>();
		IBundleProvider bundleProvider = searchRegularRecord(PatientMapper.PATIENT, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			for (IBaseResource resource : bundleProvider.getAllResources()) {
				patientReportedList
						.add((PatientReported) allMappingService.localObjectReportedWithMaster((Patient) resource));
			}
		}
		return patientReportedList;
	}

	public List<PatientMaster> searchPatientMasterGoldenList(SearchParameterMap searchParameterMap) {
		List<PatientMaster> patientList = new ArrayList<>();
		IBundleProvider bundleProvider = searchGoldenRecord(PatientMapper.Patient, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			for (IBaseResource resource : bundleProvider.getAllResources()) {
				patientList.add((PatientMaster) allMappingService.localObject(resource));
			}
		}
		return patientList;
	}

	public Organization searchOrganization(SearchParameterMap searchParameterMap) {
		IBundleProvider bundleProvider = search("Organization", searchParameterMap);
		return (Organization) bundleProvider.getAllResources().stream().findFirst().orElse(null);
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
			if (entry.getResource().fhirType().equals(IMMUNIZATION)) {
				if (AbstractFhirRequester.isGoldenRecord(entry.getResource())) {
					VaccinationMaster vaccinationMaster = (VaccinationMaster) allMappingService
							.localObject(entry.getResource());
					if (vaccinationMaster != null) {
						vaccinationList.add(vaccinationMaster);
					}
				}
			}
		}
		return vaccinationList;
	}

	// public List<ObservationMaster> searchObservationMasterList(SearchParameterMap
	// searchParameterMap) {
	// IGenericClient fhirClient = repositoryClientFactory.getFhirClient();
	// List<ObservationMaster> observationReportedList = new ArrayList<>();
	// IBundleProvider bundleProvider = searchGoldenRecord(Observation.class,
	// searchParameterMap);
	// for (IBaseResource resource : bundleProvider.getAllResources()) {
	// observationReportedList.add(observationMapper.localObjectReportedWithMaster((Observation)
	// resource));
	// }
	// return observationReportedList;
	// }

	// public ModelPerson searchPerson(SearchParameterMap searchParameterMap) {
	// ModelPerson modelPerson = null;
	// IBundleProvider bundleProvider = search(Person.class, searchParameterMap);
	// if (!bundleProvider.isEmpty()) {
	// modelPerson = personMapper.localObject((Person)
	// bundleProvider.getResources(0, 1).get(0));
	// }
	// return modelPerson;
	// }

	public ModelPerson searchPractitioner(SearchParameterMap searchParameterMap) {
		return (ModelPerson) searchMappedObjectMaster(PractitionerMapper.PRACTITIONER, searchParameterMap);
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
			return (PatientReported) allMappingService.localObjectReportedWithMaster(outcome.getResource());
		} else if (outcome.getCreated() != null && outcome.getCreated()) {
			patientReported.setPatientId(outcome.getId().getIdPart());
			return readAsPatientReported(outcome.getId().getIdPart());
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
		Observation observation = observationMapper.fhirResource(observationReported);
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

	public PatientMaster readAsPatientMaster(String id) {
		Patient patient = (Patient) read(PatientMapper.PATIENT, id);
		if (AbstractFhirRequester.isGoldenRecord(patient)) {
			return (PatientMaster) allMappingService.localObject(patient);
		}
		return null;
	}

	public PatientReported readAsPatientReported(String id) {
		return (PatientReported) allMappingService.localObjectReportedWithMaster(read(PatientMapper.PATIENT, id));
	}

	public ModelPerson readPractitionerAsPerson(String id) {
		return practitionerMapper.localObject((Practitioner) read(PractitionerMapper.PRACTITIONER, id));
	}

	public OrgLocation readAsOrgLocation(String id) {
		return locationMapper.localObject((Location) read(LocationMapper.LOCATION, id));
	}

	public VaccinationReported readAsVaccinationReported(String id) {
		return (VaccinationReported) allMappingService
				.localObjectReportedWithMaster((Immunization) read(ImmunizationMapper.IMMUNIZATION, id));
	}

	public VaccinationMaster readAsVaccinationMaster(String id) {
		Immunization immunization = (Immunization) read(ImmunizationMapper.IMMUNIZATION, id);
		if (AbstractFhirRequester.isGoldenRecord(immunization)) {
			return (VaccinationMaster) allMappingService.localObject(immunization);
		}
		return null;
	}

	public PatientMaster matchPatient(List<PatientReported> multipleMatches, PatientMaster patientMasterForMatchQuery,
			Date cutoff) {
		PatientMaster singleMatch = null;
		Bundle matches = repositoryClientFactory.getFhirClient()
				.operation().onType(Patient.class)
				.named("match")
				.withParameter(Parameters.class, "resource", allMappingService.fhirResource(patientMasterForMatchQuery))
				.returnResourceType(Bundle.class).execute();
		BigDecimal singleMatchScore = new BigDecimal(-1);
		for (Bundle.BundleEntryComponent entry : matches.getEntry()) {
			if (entry.getResource() instanceof Patient) {
				Patient patient = (Patient) entry.getResource();
				PatientMaster patientMaster = (PatientMaster) allMappingService.localObject(patient);
				/*
				 * Filter for flavours previously configured SNAIL
				 */
				if (cutoff != null && cutoff.before(patientMaster.getReportedDate())) {
					break;
				}

				// /**
				// * Filtering only Golden records
				// * TODO ask Nathan to assert workflow
				// */
				// if (entry.getResource().getMeta().getTag(GOLDEN_SYSTEM_TAG, GOLDEN_RECORD) ==
				// null) {
				// break;
				// }
				// if (entry.getSearch().hasScore() &&
				// entry.getSearch().getScoreElement().compareTo(new
				// DecimalType(MINIMAL_MATCHING_SCORE))) {
				// singleMatch = patientMaster;
				// }
				if (isGoldenRecord(entry.getResource())) {
					if (singleMatch == null) {
						if (!entry.getSearch().hasScore()) {
							singleMatch = patientMaster;
						} else if (entry.getSearch().getScoreElement().compareTo(new DecimalType(
								Math.max(MINIMAL_MATCHING_SCORE, singleMatchScore.toBigInteger().intValue()))) >= 0) {
							singleMatch = patientMaster;
							singleMatchScore = entry.getSearch().getScore();
						}
					}
				}
				multipleMatches.add((PatientReported) allMappingService.localObjectReported(entry.getResource()));
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
						.map(part -> ((StringType) part.getValue()).getValue()))
				.flatMap(Optional::stream);
	}

	public Optional<String> readGoldenResourceId(String reportId) {
		Parameters out = repositoryClientFactory.getFhirClient().operation().onServer().named("$mdm-query-links")
				.withParameters(new Parameters().addParameter("resourceId", reportId)).execute();
		List<Parameters.ParametersParameterComponent> part = out.getParameter().stream()
				.filter(parametersParameterComponent -> parametersParameterComponent.getName().equals("link"))
				.findFirst().orElse(new Parameters.ParametersParameterComponent()).getPart();
		Optional<Parameters.ParametersParameterComponent> goldenIdComponent = part.stream()
				.filter(parametersParameterComponent -> parametersParameterComponent.getName()
						.equals("goldenResourceId"))
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
