package org.immregistries.iis.kernal.repository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.mapping.forR5.*;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.iis.kernal.model.ModelPerson;
import org.immregistries.iis.kernal.servlet.ServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.immregistries.iis.kernal.mapping.MappingHelper.OBSERVATION_REPORTED;
import static org.immregistries.iis.kernal.mapping.MappingHelper.MRN_SYSTEM;

@Component
public class FhirRequests {
	Logger logger = LoggerFactory.getLogger(FhirRequests.class);
	@Autowired
	IFhirResourceDao<Patient> patientDao;
	@Autowired
	FhirContext fhirContext;
	@Autowired
	RepositoryClientFactory repositoryClientFactory;
	@Autowired
	PatientMapper patientMapper;
	@Autowired
	ImmunizationMapper immunizationMapper;
	@Autowired
	LocationMapper locationMapper;
	@Autowired
	ObservationMapper observationMapper;
	@Autowired
	PersonMapper personMapper;

	public static final String GOLDEN_SYSTEM_TAG = "http://hapifhir.io/fhir/NamingSystem/mdm-record-status";
	public static final String GOLDEN_RECORD = "GOLDEN_RECORD";
	private static final TokenCriterion NOT_GOLDEN_CRITERION= new TokenCriterion("_tag:not",GOLDEN_SYSTEM_TAG,GOLDEN_RECORD);

	private MethodOutcome save(IBaseResource resource, ICriterion... where) {
		IGenericClient fhirClient = ServletHelper.getFhirClient(repositoryClientFactory);
		MethodOutcome outcome;
		try {
			IUpdateTyped query = fhirClient.update().resource(resource);
			int size = where.length;
			query = query.conditional().where(NOT_GOLDEN_CRITERION);
			int i = 0;
			while (i < size) {
				query = ((IUpdateWithQuery) query).and(where[i]);
				i++;
			}
			outcome = query
				.execute();
		} catch (ResourceNotFoundException e){
			outcome = fhirClient.create().resource(resource)
				.execute();
		}
		return outcome;
	}
	public IBaseResource read(Class<? extends IBaseResource> aClass,String id) {
		IGenericClient fhirClient = ServletHelper.getFhirClient(repositoryClientFactory);
		try {
			return fhirClient.read().resource(aClass).withId(id).execute();
		} catch (ResourceNotFoundException e){
			return null;
		}
	}
	private Bundle searchGoldenRecord(Class<? extends IBaseResource> aClass,
												 ICriterion... where) {
		IGenericClient fhirClient = ServletHelper.getFhirClient(repositoryClientFactory);
		try {
			IQuery<Bundle> query = fhirClient.search().forResource(aClass).returnBundle(Bundle.class);
			int size = where.length;
			if (size > 0) {
				query = query.where(where[0]).withTag(GOLDEN_SYSTEM_TAG, GOLDEN_RECORD);
			}
			int i = 1;
			while (i < size) {
				query = query.and(where[i]);
				i++;
			}
			return  query.execute();
		} catch (ResourceNotFoundException e) {
			return  new Bundle();
		}
	}

	private IBundleProvider searchGoldenRecord(IFhirResourceDao dao,
															 IQueryParameterType... where) {
		SearchParameterMap searchParameterMap = new SearchParameterMap();
		searchParameterMap.add("_tag", new TokenParam(GOLDEN_SYSTEM_TAG,GOLDEN_RECORD));
		try {
			int size = where.length;
			int i = 0;
			while (i < size) {
				searchParameterMap.add(where[i].getQueryParameterQualifier(),where[i])	;
				i++;
			}
			return dao.search(searchParameterMap, ServletHelper.requestDetailsWithPartitionName());

		} catch (ResourceNotFoundException e) {
			return null;
		}

	}
	private Bundle searchRegularRecord(Class<? extends IBaseResource> aClass,
												  ICriterion... where) {
		IGenericClient fhirClient = ServletHelper.getFhirClient(repositoryClientFactory);
		try {
			IQuery<Bundle> query = fhirClient.search().forResource(aClass).returnBundle(Bundle.class);
			int size = where.length;
			query = query.where(NOT_GOLDEN_CRITERION);
			int i = 0;
			while (i < size) {
				query = query.and(where[i]);
				i++;
			}
			return  query.execute();
		} catch (ResourceNotFoundException e) {
			return  new Bundle();
		}
	}
	private Bundle search(
		Class<? extends IBaseResource> aClass,
		ICriterion... where) {
		IGenericClient fhirClient = ServletHelper.getFhirClient(repositoryClientFactory);
		try {
			IQuery<Bundle> query = fhirClient.search().forResource(aClass).returnBundle(Bundle.class);
			int size = where.length;
			if (size > 0) {
				query = query.where(where[0]);
			}
			int i = 1;
			while (i < size) {
				query = query.and(where[i]);
				i++;
			}
			return  query.execute();
		} catch (ResourceNotFoundException e) {
			return  new Bundle();
		}
	}



	public PatientMaster searchPatientMaster(ICriterion... where) {
		PatientMaster patientMaster = null;
		Bundle bundle = searchGoldenRecord(Patient.class, where);
		if (bundle.hasEntry()) {
			patientMaster = patientMapper.getMaster((Patient) bundle.getEntryFirstRep().getResource());
		}
		return patientMaster;
	}

	public PatientReported searchPatientReported(ICriterion... where) {
		PatientReported patientReported = null;
		Bundle bundle = searchRegularRecord(Patient.class, where);

		if (bundle.hasEntry()) {
			patientReported = patientMapper.getReportedWithMaster((Patient) bundle.getEntryFirstRep().getResource());
		}
		return patientReported;
	}

	public List<PatientReported> searchPatientReportedList(ICriterion... where) {
		List<PatientReported> patientReportedList = new ArrayList<>();
		Bundle bundle = searchRegularRecord(Patient.class, where);
		for (Bundle.BundleEntryComponent entry: bundle.getEntry()) {
			patientReportedList.add(patientMapper.getReportedWithMaster((Patient) entry.getResource()));
		}
//		IBundleProvider bundleProvider = searchGoldenRecord(patientDao,where);
//		for (IBaseResource resource: bundleProvider.getAllResources()) {
//			patientReportedList.add(patientMapper.getReportedWithMaster((Patient) resource));
//		}
		return patientReportedList;
	}
	public VaccinationMaster searchVaccinationMaster(ICriterion... where) {
		VaccinationMaster vaccinationMaster = null;
		Bundle bundle = searchGoldenRecord(Immunization.class, where);
		if (bundle.hasEntry()) {
			vaccinationMaster = immunizationMapper.getMaster((Immunization) bundle.getEntryFirstRep().getResource());
		}
		return vaccinationMaster;
	}
	public VaccinationReported searchVaccinationReported(ICriterion... where) {
		VaccinationReported vaccinationReported = null;
		Bundle bundle = searchRegularRecord(Immunization.class, where);
		if (bundle.hasEntry()) {
			vaccinationReported = immunizationMapper.getReportedWithMaster((Immunization) bundle.getEntryFirstRep().getResource());
		}
		return vaccinationReported;
	}
	public Organization searchOrganization(ICriterion... where) {
		Organization organization = null;
		Bundle bundle = search(Organization.class, where);
		if (bundle.hasEntry()) {
			organization = (Organization) bundle.getEntryFirstRep().getResource();
		}
		return organization;
	}
	public List<VaccinationReported> searchVaccinationReportedList(ICriterion... where) {
		List<VaccinationReported> vaccinationReportedList = new ArrayList<>();
		Bundle bundle = searchRegularRecord(Immunization.class, where);
		for (Bundle.BundleEntryComponent entry: bundle.getEntry()) {
			vaccinationReportedList.add(immunizationMapper.getReportedWithMaster((Immunization) entry.getResource()));
		}
		return vaccinationReportedList;
	}
	public ObservationReported searchObservationReported(ICriterion... where) {
		IGenericClient fhirClient = ServletHelper.getFhirClient(repositoryClientFactory);
		ObservationReported observationReported = null;
		Bundle bundle = searchRegularRecord(Observation.class, where);
		if (bundle.hasEntry()) {
			observationReported = observationMapper.getReportedWithMaster((Observation) bundle.getEntryFirstRep().getResource(),this,fhirClient);
		}
		return observationReported;
	}
	public ObservationMaster searchObservationMaster(ICriterion... where) {
		ObservationMaster observationMaster = null;
		Bundle bundle = searchGoldenRecord(Observation.class, where);
		if (bundle.hasEntry()) {
			observationMaster = observationMapper.getMaster((Observation) bundle.getEntryFirstRep().getResource());
		}
		return observationMaster;
	}
	public List<ObservationReported> searchObservationReportedList(ICriterion... where) {
		IGenericClient fhirClient = ServletHelper.getFhirClient(repositoryClientFactory);
		List<ObservationReported> observationReportedList = new ArrayList<>();
		Bundle bundle = search(Observation.class, where);
		for (Bundle.BundleEntryComponent entry: bundle.getEntry()) {
			observationReportedList.add(observationMapper.getReportedWithMaster((Observation) entry.getResource(),this,fhirClient));
		}
		return observationReportedList;
	}
	public OrgLocation searchOrgLocation(ICriterion... where) {
		OrgLocation orgLocation = null;
		Bundle bundle = search(Observation.class, where);
		if (bundle.hasEntry()) {
			orgLocation = locationMapper.orgLocationFromFhir((Location) bundle.getEntryFirstRep().getResource());
		}
		return orgLocation;
	}
	public List<OrgLocation> searchOrgLocationList(ICriterion... where) {
		List<OrgLocation> locationList = new ArrayList<>();
		Bundle bundle = search(Location.class, where);
		for (Bundle.BundleEntryComponent entry: bundle.getEntry()) {
			locationList.add(locationMapper.orgLocationFromFhir((Location) entry.getResource()));
		}
		return locationList;
	}


	public ModelPerson searchPerson(ICriterion... where) {
		ModelPerson modelPerson = null;
		Bundle bundle = search(org.hl7.fhir.r5.model.Person.class, where);
		if (bundle.hasEntry()) {
			modelPerson = personMapper.getModelPerson((org.hl7.fhir.r5.model.Person) bundle.getEntryFirstRep().getResource());
		}
		return modelPerson;
	}
	public ModelPerson searchPractitioner(ICriterion... where) {
		ModelPerson modelPerson = null;
		Bundle bundle = search(org.hl7.fhir.r5.model.Practitioner.class, where);
		if (bundle.hasEntry()) {
			modelPerson = personMapper.getModelPerson((org.hl7.fhir.r5.model.Practitioner) bundle.getEntryFirstRep().getResource());
		}
		return modelPerson;
	}

	public PatientReported savePatientReported(PatientReported patientReported) {
		Patient patient =  patientMapper.getFhirResource(patientReported);
		MethodOutcome outcome = save(patient,
			Patient.IDENTIFIER.exactly().systemAndIdentifier(MRN_SYSTEM,patientReported.getPatientReportedExternalLink()));
		if (!outcome.getResource().isEmpty()) {
			patientReported.setPatientReportedId(outcome.getResource().getIdElement().getIdPart());
			return patientMapper.getReportedWithMaster((Patient) outcome.getResource());
		} else if (outcome.getCreated() != null && outcome.getCreated()) {
			patientReported.setPatientReportedId(outcome.getId().getIdPart());
		}
//		return patientReported;
		return searchPatientReported(Patient.IDENTIFIER.exactly().systemAndIdentifier(MRN_SYSTEM,patientReported.getPatientReportedExternalLink()));
	}
	public ModelPerson savePractitioner(ModelPerson modelPerson) {
		Practitioner practitioner = personMapper.getFhirPractitioner(modelPerson);
		MethodOutcome outcome = save(practitioner,
			Patient.IDENTIFIER.exactly().identifier(modelPerson.getPersonExternalLink()));
		if (outcome.getCreated() != null && outcome.getCreated()) {
			modelPerson.setPersonId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			modelPerson.setPersonId(outcome.getResource().getIdElement().getIdPart());
		}
		return modelPerson;
	}

	public ObservationReported saveObservationReported(ObservationReported observationReported) {
		Observation observation = observationMapper.getFhirResource(observationReported);
		MethodOutcome outcome = save(observation,
			Observation.IDENTIFIER.exactly().systemAndIdentifier(OBSERVATION_REPORTED,observationReported.getObservationReportedId()));
		if (outcome.getCreated() != null && outcome.getCreated()) {
			observationReported.setPatientReportedId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			observationReported.setPatientReportedId(outcome.getResource().getIdElement().getIdPart());
		}
		return observationReported;
	}

	public VaccinationReported saveVaccinationReported(VaccinationReported vaccinationReported) {
		Immunization immunization = immunizationMapper.getFhirResource(vaccinationReported);
		MethodOutcome outcome = save(immunization,
			Immunization.IDENTIFIER.exactly()
				.systemAndIdentifier(MappingHelper.VACCINATION_REPORTED, vaccinationReported.getVaccinationReportedExternalLink())
		);
		if (outcome.getCreated() != null && outcome.getCreated()){
			vaccinationReported.setVaccinationReportedId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			vaccinationReported.setVaccinationReportedId(outcome.getResource().getIdElement().getIdPart());
		}
		return vaccinationReported;
	}

	public OrgLocation saveOrgLocation(OrgLocation orgLocation) {
		Location location = locationMapper.fhirLocation(orgLocation);
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
		return patientMapper.getReported((Patient) read(Patient.class,id));
	}
	public ModelPerson readPractitionerPerson(String id) {
		return personMapper.getModelPerson((Practitioner) read(Practitioner.class,id));
	}
	public OrgLocation readOrgLocation(String id) {
		return locationMapper.orgLocationFromFhir((Location) read(Location.class,id));
	}
}
