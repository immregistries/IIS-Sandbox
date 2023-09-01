package org.immregistries.iis.kernal.InternalClient;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.mapping.Interfaces.*;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class FhirRequester<
	Patient extends IBaseResource,
	Immunization extends IBaseResource,
	Location extends IBaseResource,
	Practitioner extends IBaseResource,
	Observation extends IBaseResource,
	Person extends IBaseResource,
	Organization extends IBaseResource,
	RelatedPerson extends IBaseResource >
	implements IFhirRequester<Patient,Immunization,Location,Practitioner,Observation,Person,Organization, RelatedPerson> {

	@Autowired
	PatientMapper<Patient> patientMapper;
	@Autowired
	ImmunizationMapper<Immunization> immunizationMapper;
	@Autowired
	LocationMapper<Location> locationMapper;
	@Autowired
	PractitionerMapper<Practitioner> practitionerMapper;
	@Autowired
	ObservationMapper<Observation> observationMapper;
	@Autowired
	PersonMapper<Person> personMapper;
	@Autowired
	RelatedPersonMapper<RelatedPerson> relatedPersonMapper;

	@Autowired
	RepositoryClientFactory repositoryClientFactory;

//	private final Class<Patient> patientClass;
//	private final Class<Immunization> immunizationClass;
//	private final Class<Location> locationClass;
//	private final Class<Practitioner> practitionerClass;
//	private final Class<Observation> observationClass;
//	private final Class<Person> personClass;
//	private final Class<Organization> organizationClass;
//
//	@SuppressWarnings("unchecked")
//	public FhirRequester() {
//		Class[] classes = GenericTypeResolver.resolveTypeArguments(getClass(), FhirRequester.class);
//		assert classes != null;
//		this.patientClass = (Class<Patient>) classes[0] ;
//		this.immunizationClass = (Class<Immunization>) classes[1];
//		this.locationClass = (Class<Location>) classes[2];
//		this.practitionerClass = (Class<Practitioner>) classes[3];
//		this.observationClass = (Class<Observation>) classes[4];
//		this.personClass = (Class<Person>) classes[5];
//		this.organizationClass = (Class<Organization>) classes[6];
//	}

	public static final String GOLDEN_SYSTEM_TAG = "http://hapifhir.io/fhir/NamingSystem/mdm-record-status";
	public static final String GOLDEN_SYSTEM_IDENTIFIER = "\"http://hapifhir.io/fhir/NamingSystem/mdm-golden-resource-enterprise-id\"";
	public static final String GOLDEN_RECORD = "GOLDEN_RECORD";

	private static final ICriterion NOT_GOLDEN_CRITERION = new TokenCriterion("_tag:not", GOLDEN_SYSTEM_TAG, GOLDEN_RECORD);

	MethodOutcome save(IBaseResource resource, ICriterion... where) {
		IGenericClient fhirClient = repositoryClientFactory.getFhirClient();
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
		IGenericClient fhirClient = repositoryClientFactory.getFhirClient();
		try {
			return fhirClient.read().resource(aClass).withId(id).execute();
		} catch (ResourceNotFoundException e){
			return null;
		}
	}

	public IBaseBundle searchGoldenRecord(Class<? extends IBaseResource> aClass, ICriterion... where) {
		IGenericClient fhirClient = repositoryClientFactory.getFhirClient();
		try {
			IQuery<IBaseBundle> query = fhirClient.search().forResource(aClass);
			int size = where.length;
			if (size > 0) {
				query = query.where(where[0]);
			}
			query = query.withTag(GOLDEN_SYSTEM_TAG, GOLDEN_RECORD);
			int i = 1;
			while (i < size) {
				query = query.and(where[i]);
				i++;
			}
			return  query.execute();
		} catch (ResourceNotFoundException e) {
			return  null;
		}
	}

	IBundleProvider searchGoldenRecord(IFhirResourceDao dao,  IQueryParameterType... where) {
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

	public IBaseBundle searchRegularRecord(Class<? extends IBaseResource> aClass, ICriterion... where) {
		IGenericClient fhirClient = repositoryClientFactory.getFhirClient();
		try {
			IQuery<IBaseBundle> query = fhirClient.search().forResource(aClass);
			int size = where.length;
			query = query.where(NOT_GOLDEN_CRITERION);
			int i = 0;
			while (i < size) {
				query = query.and(where[i]);
				i++;
			}
			return query.execute();
		} catch (ResourceNotFoundException e) {
			return null;
		}
	}

	IBaseBundle search(
		Class<? extends IBaseResource> aClass,
		ICriterion... where) {
		IGenericClient fhirClient = repositoryClientFactory.getFhirClient();
		try {
			IQuery<IBaseBundle> query = fhirClient.search().forResource(aClass);
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
			return null;
		}
	}
}
