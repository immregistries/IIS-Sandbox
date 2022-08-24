package org.immregistries.iis.kernal.repository;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.mapping.*;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.iis.kernal.model.ModelPerson;
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
	@Autowired
	IFhirResourceDao<Patient> patientDao;
	@Autowired
	RepositoryClientFactory repositoryClientFactory;
	Logger logger = LoggerFactory.getLogger(FhirRequests.class);
	public static final String GOLDEN_SYSTEM_TAG = "http://hapifhir.io/fhir/NamingSystem/mdm-record-status";
	public static final String GOLDEN_RECORD = "GOLDEN_RECORD";
	private static final TokenCriterion NOT_GOLDEN_CRITERION= new TokenCriterion("_tag:not",GOLDEN_SYSTEM_TAG,GOLDEN_RECORD);


	public PatientMaster searchPatientMaster(IGenericClient fhirClient, ICriterion... where) {
		PatientMaster patientMaster = null;
		Bundle bundle = searchGoldenRecord(Patient.class,fhirClient, where);
		if (bundle.hasEntry()) {
			patientMaster = PatientMapper.getMaster((Patient) bundle.getEntryFirstRep().getResource());
		}
		return patientMaster;
	}
	public PatientReported searchPatientReported(IGenericClient fhirClient, ICriterion... where) {
		PatientReported patientReported = null;
		Bundle bundle = searchRegularRecord(Patient.class,fhirClient, where);
		if (bundle.hasEntry()) {
			patientReported = PatientMapper.getReportedWithMaster((Patient) bundle.getEntryFirstRep().getResource(),this,fhirClient);
		}
		return patientReported;
	}

	public List<PatientReported> searchPatientReportedList(IGenericClient fhirClient, ICriterion... where) {
		List<PatientReported> patientReportedList = new ArrayList<>();
		Bundle bundle = searchRegularRecord(Patient.class,fhirClient, where);
		for (Bundle.BundleEntryComponent entry: bundle.getEntry()) {
			patientReportedList.add(PatientMapper.getReportedWithMaster((Patient) entry.getResource(),this,fhirClient));
		}
		return patientReportedList;
	}
	public VaccinationMaster searchVaccinationMaster(IGenericClient fhirClient, ICriterion... where) {
		VaccinationMaster vaccinationMaster = null;
		Bundle bundle = searchGoldenRecord(Immunization.class,fhirClient, where);
		if (bundle.hasEntry()) {
			vaccinationMaster = ImmunizationMapper.getMaster((Immunization) bundle.getEntryFirstRep().getResource());
		}
		return vaccinationMaster;
	}
	public VaccinationReported searchVaccinationReported(IGenericClient fhirClient, ICriterion... where) {
		VaccinationReported vaccinationReported = null;
		Bundle bundle = searchRegularRecord(Immunization.class,fhirClient, where);
		if (bundle.hasEntry()) {
			vaccinationReported = ImmunizationMapper.getReported((Immunization) bundle.getEntryFirstRep().getResource());
		}
		return vaccinationReported;
	}
	public List<VaccinationReported> searchVaccinationReportedList(IGenericClient fhirClient, ICriterion... where) {
		List<VaccinationReported> vaccinationReportedList = new ArrayList<>();
		Bundle bundle = searchRegularRecord(Immunization.class,fhirClient, where);
		for (Bundle.BundleEntryComponent entry: bundle.getEntry()) {
			vaccinationReportedList.add(ImmunizationMapper.getReported((Immunization) entry.getResource()));
		}
		return vaccinationReportedList;
	}
	public ObservationReported searchObservationReported(IGenericClient fhirClient, ICriterion... where) {
		ObservationReported observationReported = null;
		Bundle bundle = search(Observation.class,fhirClient, where);
		if (bundle.hasEntry()) {
			observationReported = ObservationMapper.getReported((Observation) bundle.getEntryFirstRep().getResource());
		}
		return observationReported;
	}
	public List<ObservationReported> searchObservationReportedList(IGenericClient fhirClient, ICriterion... where) {
		List<ObservationReported> observationReportedList = new ArrayList<>();
		Bundle bundle = search(Observation.class,fhirClient, where);
		for (Bundle.BundleEntryComponent entry: bundle.getEntry()) {
			observationReportedList.add(ObservationMapper.getReported((Observation) entry.getResource()));
		}
		return observationReportedList;
	}
	public OrgLocation searchOrgLocation(IGenericClient fhirClient, ICriterion... where) {
		OrgLocation orgLocation = null;
		Bundle bundle = search(Observation.class,fhirClient, where);
		if (bundle.hasEntry()) {
			orgLocation = LocationMapper.orgLocationFromFhir((Location) bundle.getEntryFirstRep().getResource());
		}
		return orgLocation;
	}
	public List<OrgLocation> searchOrgLocationList(IGenericClient fhirClient, ICriterion... where) {
		List<OrgLocation> locationList = new ArrayList<>();
		Bundle bundle = search(Location.class,fhirClient, where);
		for (Bundle.BundleEntryComponent entry: bundle.getEntry()) {
			locationList.add(LocationMapper.orgLocationFromFhir((Location) entry.getResource()));
		}
		return locationList;
	}


	public ModelPerson searchPerson(IGenericClient fhirClient, ICriterion... where) {
		ModelPerson modelPerson = null;
		Bundle bundle = search(org.hl7.fhir.r5.model.Person.class,fhirClient, where);
		if (bundle.hasEntry()) {
			modelPerson = PersonMapper.getModelPerson((org.hl7.fhir.r5.model.Person) bundle.getEntryFirstRep().getResource());
		}
		return modelPerson;
	}
	public ModelPerson searchPractitioner(IGenericClient fhirClient, ICriterion... where) {
		ModelPerson modelPerson = null;
		Bundle bundle = search(org.hl7.fhir.r5.model.Practitioner.class,fhirClient, where);
		if (bundle.hasEntry()) {
			modelPerson = PersonMapper.getModelPerson((org.hl7.fhir.r5.model.Practitioner) bundle.getEntryFirstRep().getResource());
		}
		return modelPerson;
	}


	private Bundle searchGoldenRecord(Class<? extends org.hl7.fhir.instance.model.api.IBaseResource> aClass,
												 IGenericClient fhirClient,
												 ICriterion... where) {
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

	private Bundle searchRegularRecord(Class<? extends org.hl7.fhir.instance.model.api.IBaseResource> aClass,
												 IGenericClient fhirClient,
												 ICriterion... where) {
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
		Class<? extends org.hl7.fhir.instance.model.api.IBaseResource> aClass,
		IGenericClient fhirClient,
		ICriterion... where) {
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

	public PatientReported savePatientReported(IGenericClient fhirClient, PatientReported patientReported) {
		Patient patient =  PatientMapper.getFhirResource( null,patientReported);
		MethodOutcome outcome = saveRegular(fhirClient,patient,
			Patient.IDENTIFIER.exactly().systemAndIdentifier(MRN_SYSTEM,patientReported.getPatientReportedExternalLink()));
		if (outcome.getCreated() != null && outcome.getCreated()) {
			patientReported.setPatientReportedId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			patientReported.setPatientReportedId(outcome.getResource().getIdElement().getIdPart());
		}
		return patientReported;
	}
	public ModelPerson savePractitioner(IGenericClient fhirClient, ModelPerson modelPerson) {
		Practitioner practitioner = PersonMapper.getFhirPractitioner(modelPerson);
		MethodOutcome outcome = saveRegular(fhirClient,practitioner,
			Patient.IDENTIFIER.exactly().identifier(modelPerson.getPersonExternalLink()));
		if (outcome.getCreated() != null && outcome.getCreated()) {
			modelPerson.setPersonId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			modelPerson.setPersonId(outcome.getResource().getIdElement().getIdPart());
		}
		return modelPerson;
	}

	public ObservationReported saveObservationReported(IGenericClient fhirClient, ObservationReported observationReported) {
		Observation observation = ObservationMapper.getFhirResource(null,observationReported);
		MethodOutcome outcome = saveRegular(fhirClient,observation,
			Observation.IDENTIFIER.exactly().systemAndIdentifier(OBSERVATION_REPORTED,observationReported.getObservationReportedId()));
		if (outcome.getCreated() != null && outcome.getCreated()) {
			observationReported.setPatientReportedId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			observationReported.setPatientReportedId(outcome.getResource().getIdElement().getIdPart());
		}
		return observationReported;
	}

	public VaccinationReported saveVaccinationReported(IGenericClient fhirClient, VaccinationReported vaccinationReported) {
		Immunization immunization = ImmunizationMapper.getFhirResource( null, vaccinationReported);
		MethodOutcome outcome = saveRegular(fhirClient,immunization,
			Immunization.IDENTIFIER.exactly()
				.systemAndIdentifier(MappingHelper.VACCINATION_REPORTED, vaccinationReported.getVaccinationReportedExternalLink())
		);
		if (outcome.getCreated()){
			vaccinationReported.setVaccinationReportedId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			vaccinationReported.setVaccinationReportedId(outcome.getResource().getIdElement().getIdPart());
		}
		return vaccinationReported;
	}
	public VaccinationReported saveVaccinationReported(IGenericClient fhirClient, VaccinationMaster vaccinationMaster, VaccinationReported vaccinationReported) {
		Immunization immunization = ImmunizationMapper.getFhirResource( vaccinationMaster, vaccinationReported);
		MethodOutcome outcome = saveRegular(fhirClient,immunization,
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
	public OrgLocation saveOrgLocation(IGenericClient fhirClient, OrgLocation orgLocation) {
		Location location = LocationMapper.fhirLocation(orgLocation);
		MethodOutcome outcome = saveRegular(fhirClient,location,
			Location.IDENTIFIER.exactly().identifier(location.getIdentifierFirstRep().getValue())
			);
		if (outcome.getCreated() != null && outcome.getCreated()){
			orgLocation.setOrgLocationId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			orgLocation.setOrgLocationId(outcome.getResource().getIdElement().getIdPart());
		}
		return orgLocation;
	}

	private MethodOutcome saveRegular(IGenericClient fhirClient, IBaseResource resource, ICriterion... where) {
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
	private MethodOutcome save(IGenericClient fhirClient, IBaseResource resource, ICriterion... where) {
		MethodOutcome outcome;
		try {
			IUpdateTyped query = fhirClient.update().resource(resource);
			int size = where.length;
			if (size > 0) {
				query = query.conditional().where(where[0]);
			}
			int i = 1;
			while (i < size) {
				query = ((IUpdateWithQuery) query).and(where[i]);
				i++;
			}
			outcome = query.execute();
		} catch (ResourceNotFoundException e){
			outcome = fhirClient.create().resource(resource)
				.execute();
		}
		return outcome;
	}

}
