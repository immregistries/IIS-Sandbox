package org.immregistries.iis.kernal.mapping.internalClient;

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
import org.immregistries.iis.kernal.mapping.interfaces.*;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

public abstract class AbstractFhirRequester<
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
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

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
//	@Autowired
//	RelatedPersonMapper<RelatedPerson> relatedPersonMapper;

	@Autowired
	RepositoryClientFactory repositoryClientFactory;
	@Autowired
	DaoRegistry daoRegistry;
	@Autowired
	FhirContext fhirContext;
	@Autowired
	RestfulServer fhirServer;

	/**
	 * Converts HAPI ICriterion Object to HTTP URI  parameter substring
	 *
	 * @param iCriterion HAPIFHIR criterion
	 * @return HTTP parameter String equivalent
	 */
	private String stringCriterion(ICriterion iCriterion) {
		ICriterionInternal iCriterionInternal = (ICriterionInternal) iCriterion;
		return iCriterionInternal.getParameterName() + "=" + iCriterionInternal.getParameterValue(fhirContext);
	}

	/**
	 * Converts list HAPI ICriterion to a complete HTTP URI parameter suffix
	 * @param criteria HAPIFHIR criteria list
	 * @return Complete HTTP URI suffix
	 */
	private String stringCriterionList(ICriterion... criteria) {
		int size = criteria.length;
		StringBuilder params = new StringBuilder();
		if (size > 0) {
			params = new StringBuilder(stringCriterion(criteria[0]));
			int i = 1;
			while (i < size) {
				params.append("&").append(stringCriterion(criteria[i]));
				i++;
			}
		}
		return params.toString();
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

	/**
	 * Helping method for saving, executes conditional update and create on HAPI DAO, adds parameter to avoid golden records
	 *
	 * @param createOnly
	 * @param resource   Resource to save
	 * @param where      HAPIFHIR Criteria list
	 * @return methodOutcome
	 */
	protected MethodOutcome save(boolean createOnly, IBaseResource resource, ICriterion... where) {
		IFhirResourceDao dao = daoRegistry.getResourceDao(resource);
		String params = stringCriterionList(where);
		if (StringUtils.isNotBlank(params)) {
			// If not empty add &
			params += "&";
			params += NOT_GOLDEN_CRITERION;
		}
		DaoMethodOutcome outcome;
		if (createOnly) {
			return dao.create(resource, ServletHelper.requestDetailsWithPartitionName());
		}
		try {
			return dao.update(resource, params, ServletHelper.requestDetailsWithPartitionName());
		} catch (InvalidRequestException invalidRequestException) {
			return dao.create(resource, ServletHelper.requestDetailsWithPartitionName());
		}
	}

	/**
	 *
	 * @param aClass FHIR Resource Class
	 * @param id resource id
	 * @return resource found
	 */
	public IBaseResource read(Class<? extends IBaseResource> aClass, String id) {
		IFhirResourceDao dao = daoRegistry.getResourceDao(aClass);
		return dao.read(new IdType(id), ServletHelper.requestDetailsWithPartitionName());
	}

	/**
	 * Search only golden record by adding extra parameter
	 * @param aClass FHIR Resource Class
	 * @param searchParameterMap search parameters
	 * @return Bundle of search result with only Golden/Master records
	 */
	public IBundleProvider searchGoldenRecord(Class<? extends IBaseResource> aClass, SearchParameterMap searchParameterMap) {
		if (searchParameterMap == null) {
			searchParameterMap = new SearchParameterMap();
		}
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

	/**
	 * Search only regular record by adding extra parameter excluding golden record
	 * @param aClass FHIR Resource Class
	 * @param searchParameterMap Search parameters
	 * @return Bundle of search result excluding Golden/Master records
	 */
	public IBundleProvider searchRegularRecord(Class<? extends IBaseResource> aClass, SearchParameterMap searchParameterMap) {
		if (searchParameterMap == null) {
			searchParameterMap = new SearchParameterMap();
		}
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

	/**
	 * Search Operation
	 * @param aClass FHIR Resource Class
	 * @param searchParameterMap Search parameters
	 * @return Bundle of search result
	 */
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

	/**
	 * Fills multiple matched list and return Single Match
	 * Used for RSP
	 *
	 * @param multipleMatches List to add multiple matches in
	 * @param patientMasterForMatchQuery patient Information to match
	 * @param cutoff cutoff date to ignore old records
	 * @return Single match result
	 */
	public abstract PatientMaster matchPatient(List<PatientReported> multipleMatches, PatientMaster patientMasterForMatchQuery, Date cutoff);

	/**
	 * Checks Meta and Tags
	 * @param iBaseResource FHIR resource
	 * @return if resource is golden record
	 */
	public static boolean isGoldenRecord(IBaseResource iBaseResource) {
		if (iBaseResource != null && iBaseResource.getMeta() != null && !iBaseResource.getMeta().isEmpty()) {
			return iBaseResource.getMeta().getTag(GOLDEN_SYSTEM_TAG, GOLDEN_RECORD) != null;
		}
		return false;
	}

}
