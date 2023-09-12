package org.immregistries.iis.kernal.InternalClient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.jpa.util.QueryParameterUtils;
import ca.uhn.fhir.jpa.util.SearchParameterMapCalculator;
import ca.uhn.fhir.model.api.IQueryParameterAnd;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.SearchParameter;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.immregistries.iis.kernal.mapping.Interfaces.*;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	Logger logger = LoggerFactory.getLogger(FhirRequester.class);

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
	@Autowired
	DaoRegistry daoRegistry;
	@Autowired
	FhirContext fhirContext;

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

	private static final String NOT_GOLDEN_CRITERION =
//		"_tag:not" +
			GOLDEN_SYSTEM_TAG + "|" + GOLDEN_RECORD;

	private String stringCriterion(ICriterion iCriterion) {
		ICriterionInternal iCriterionInternal = (ICriterionInternal) iCriterion;
		return iCriterionInternal.getParameterName() + "=" + iCriterionInternal.getParameterValue(fhirContext);
	}
	private String stringCriterionList(ICriterion... criteria) {
		int size = criteria.length;
		String params = "";
		if (size > 0){
			params = stringCriterion(criteria[0]);
			int i = 1;
			while (i < size) {
				params += "&" + stringCriterion(criteria[i]);
				i++;
			}
		}
		return params;
	}


	private SearchParameterMap searchParameterCriterionList(ICriterion... criteria) {
		SearchParameterMap map = new SearchParameterMap();
		int size = criteria.length;
		int i = 1;
		while (i < size) {
			ICriterionInternal iCriterionInternal = (ICriterionInternal) criteria[i];
//			new QueryParameterUtils()
			map.add(iCriterionInternal.getParameterName(),
				new StringParam(iCriterionInternal.getParameterValue(fhirContext)));
//			params += "&" + stringCriterion(criteria[i]);
			i++;
		}
		return map;
	}


	protected MethodOutcome save(IBaseResource resource, ICriterion... where) {
		IFhirResourceDao dao = daoRegistry.getResourceDao(resource);
		String params = stringCriterionList(where);
		if (StringUtils.isNotBlank(params)) {
			// If not empty add &
			params += "&";
		}
		params += NOT_GOLDEN_CRITERION;
		logger.info("TEST CRITERION \n {}", params);
		DaoMethodOutcome outcome = dao.update(resource,params,ServletHelper.requestDetailsWithPartitionName());
		logger.info("TEST outcome \n {}", outcome.getCreated());
		return outcome;
	}

	public IBaseResource read(Class<? extends IBaseResource> aClass,String id) {
		IFhirResourceDao dao = daoRegistry.getResourceDao(aClass);
		return dao.read(new IdType(id),ServletHelper.requestDetailsWithPartitionName());
	}

	public IBaseBundle searchGoldenRecord(Class<? extends IBaseResource> aClass, ICriterion... where) {
		IFhirResourceDao dao = daoRegistry.getResourceDao(aClass);

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

	public IBaseBundle searchRegularRecord(Class<? extends IBaseResource> aClass, ICriterion... where) {
		IFhirResourceDao dao = daoRegistry.getResourceDao(aClass);
		SearchParameterMap map = searchParameterCriterionList(where);
		map.add("_tag:not", new StringParam(NOT_GOLDEN_CRITERION));
		logger.info("TEST CRITERION \n {}", params);
		IBundleProvider bundleProvider = dao.search(map, ServletHelper.requestDetailsWithPartitionName());
		bundleProvider.
//		IGenericClient fhirClient = repositoryClientFactory.getFhirClient();
//		try {
//			IQuery<IBaseBundle> query = fhirClient.search().forResource(aClass);
//			int size = where.length;
//			query = query.where(NOT_GOLDEN_CRITERION);
//			int i = 0;
//			while (i < size) {
//				query = query.and(where[i]);
//				i++;
//			}
//			return query.execute();
//		} catch (ResourceNotFoundException e) {
//			return null;
//		}
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
