package org.immregistries.iis.kernal.InternalClient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.ICriterionInternal;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.apache.commons.lang3.StringUtils;
 import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.mapping.Interfaces.*;
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
	RelatedPerson extends IBaseResource>
	implements IFhirRequester<Patient, Immunization, Location, Practitioner, Observation, Person, Organization, RelatedPerson> {
	//	public static final String GOLDEN_SYSTEM_IDENTIFIER = "\"http://hapifhir.io/fhir/NamingSystem/mdm-golden-resource-enterprise-id\"";
	public static final String GOLDEN_SYSTEM_TAG = "http://hapifhir.io/fhir/NamingSystem/mdm-record-status";
	public static final String GOLDEN_RECORD = "GOLDEN_RECORD";
	private static final String GOLDEN_CRITERION_PART = GOLDEN_SYSTEM_TAG + "|" + GOLDEN_RECORD;
	private static final String NOT_GOLDEN_CRITERION = "_tag:not=" + GOLDEN_CRITERION_PART;
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
	@Autowired
	RestfulServer fhirServer;

	private String stringCriterion(ICriterion iCriterion) {
		ICriterionInternal iCriterionInternal = (ICriterionInternal) iCriterion;
		return iCriterionInternal.getParameterName() + "=" + iCriterionInternal.getParameterValue(fhirContext);
	}

	private String stringCriterionList(ICriterion... criteria) {
		int size = criteria.length;
		String params = "";
		if (size > 0) {
			params = stringCriterion(criteria[0]);
			int i = 1;
			while (i < size) {
				params += "&" + stringCriterion(criteria[i]);
				i++;
			}
		}
		return params;
	}

//	private SearchParameterMap searchParameterCriterionList(ICriterion... criteria) {
//		SearchParameterMap map = new SearchParameterMap();
//		int size = criteria.length;
//		int i = 0;
//		while (i < size) {
//			ICriterionInternal iCriterionInternal = (ICriterionInternal) criteria[i];
//			if (criteria[i] instanceof TokenCriterion) {
//
//			}
//			map.add(iCriterionInternal.getParameterName(),
//				new StringParam(iCriterionInternal.getParameterValue(fhirContext)));
//			i++;
//		}
//		return map;
//	}


	protected MethodOutcome save(IBaseResource resource, ICriterion... where) {
		IFhirResourceDao dao = daoRegistry.getResourceDao(resource);
		String params = stringCriterionList(where);
		if (StringUtils.isNotBlank(params)) {
			// If not empty add &
			params += "&";
			params += NOT_GOLDEN_CRITERION;
		}
		DaoMethodOutcome outcome;
		try {
			outcome = dao.update(resource, params, ServletHelper.requestDetailsWithPartitionName());
		} catch (InvalidRequestException invalidRequestException) {
			outcome = dao.create(resource, ServletHelper.requestDetailsWithPartitionName());
		}
		return outcome;
	}

	public IBaseResource read(Class<? extends IBaseResource> aClass, String id) {
		IFhirResourceDao dao = daoRegistry.getResourceDao(aClass);
		return dao.read(new IdType(id), ServletHelper.requestDetailsWithPartitionName());
	}

	public IBundleProvider searchGoldenRecord(Class<? extends IBaseResource> aClass, SearchParameterMap searchParameterMap) {
		searchParameterMap.add("_tag", new TokenParam(GOLDEN_SYSTEM_TAG, GOLDEN_RECORD));
		return search(aClass, searchParameterMap);
//		IGenericClient fhirClient = repositoryClientFactory.getFhirClient();
//		try {
//			IQuery<IBaseBundle> query = fhirClient.search().forResource(aClass);
//			int size = where.length;
//			if (size > 0) {
//				query = query.where(where[0]);
//			}
//			query = query.withTag(GOLDEN_SYSTEM_TAG, GOLDEN_RECORD);
//			int i = 1;
//			while (i < size) {
//				query = query.and(where[i]);
//				i++;
//			}
//			return query.execute();
//		} catch (ResourceNotFoundException e) {
//			return null;
//		}
	}

	public IBundleProvider searchRegularRecord(Class<? extends IBaseResource> aClass, SearchParameterMap searchParameterMap) {
		searchParameterMap.add("_tag", new TokenParam(GOLDEN_SYSTEM_TAG, GOLDEN_RECORD).setModifier(TokenParamModifier.NOT));
		return search(aClass, searchParameterMap);
//		return dao.search(searchParameterMap, ServletHelper.requestDetailsWithPartitionName());
//		IGenericClient fhirClient = repositoryClientFactory.getFhirClient();
//		try {
//			IQuery<IBaseBundle> query = fhirClient.search().forResource(aClass);
//			int size = where.length;
////			query = query.where(NOT_GOLDEN_CRITERION);
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

	IBundleProvider search(Class<? extends IBaseResource> aClass, SearchParameterMap searchParameterMap) {
		return daoRegistry.getResourceDao(aClass).search(searchParameterMap, ServletHelper.requestDetailsWithPartitionName());
//		IGenericClient fhirClient = repositoryClientFactory.getFhirClient();
//		try {
//			IQuery<IBaseBundle> query = fhirClient.search().forResource(aClass);
//			int size = where.length;
//			if (size > 0) {
//				query = query.where(where[0]);
//			}
//			int i = 1;
//			while (i < size) {
//				query = query.and(where[i]);
//				i++;
//			}
//			return query.execute();
//		} catch (ResourceNotFoundException e) {
//			return null;
//		}
	}
}
