package org.immregistries.iis.kernal.mapping.internalClient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.immregistries.iis.kernal.fhir.security.TenantUtil;
import org.immregistries.iis.kernal.mapping.AllMappingService;
import org.immregistries.iis.kernal.mapping.interfaces.*;
import org.immregistries.iis.kernal.model.AbstractMappedObject;
import org.immregistries.iis.kernal.model.ObservationMaster;
import org.immregistries.iis.kernal.model.ObservationReported;
import org.immregistries.iis.kernal.model.OrgLocation;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.model.VaccinationReported;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("rawtypes")
public abstract class AbstractFhirRequester<Patient extends IBaseResource, Immunization extends IBaseResource, Location extends IBaseResource, Practitioner extends IBaseResource, Observation extends IBaseResource, Person extends IBaseResource, Organization extends IBaseResource, RelatedPerson extends IBaseResource>
		implements
		IFhirRequester<Patient, Immunization, Location, Practitioner, Observation, Person, Organization, RelatedPerson> {
	// public static final String GOLDEN_SYSTEM_IDENTIFIER =
	// "\"http://hapifhir.io/fhir/NamingSystem/mdm-golden-resource-enterprise-id\"";
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
	AllMappingService allMappingService;

	@Autowired
	TenantUtil tenantUtil;

	@Autowired
	RepositoryClientFactory repositoryClientFactory;
	@Autowired
	DaoRegistry daoRegistry;
	@Autowired
	FhirContext fhirContext;
	// @Autowired
	// RestfulServer fhirServer;

	public AbstractMappedObject searchMappedObjectMaster(String resourceType, SearchParameterMap searchParameterMap) {
		AbstractMappedObject mappedObject = null;
		IBundleProvider bundleProvider = searchGoldenRecord(resourceType, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			mappedObject = allMappingService.localObject(bundleProvider.getResources(0, 1).get(0));
		}
		return mappedObject;
	}

	public AbstractMappedObject searchMappedObjectReportedWithMaster(String resourceType,
			SearchParameterMap searchParameterMap) {
		AbstractMappedObject mappedObject = null;
		IBundleProvider bundleProvider = searchRegularRecord(resourceType, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			mappedObject = allMappingService.localObjectReportedWithMaster(bundleProvider.getResources(0, 1).get(0));
		}
		return mappedObject;
	}

	public List<AbstractMappedObject> searchMappedObjectReportedList(String resourceType,
			SearchParameterMap searchParameterMap) {
		IBundleProvider bundleProvider = searchRegularRecord(resourceType, searchParameterMap);
		return bundleProvider.getAllResources().stream().map(allMappingService::localObjectReportedWithMaster)
				.collect(Collectors.toList());
	}

	public List<VaccinationMaster> searchVaccinationMasterGoldenList(SearchParameterMap searchParameterMap) {
		List<VaccinationMaster> vaccinationMasterList = new ArrayList<>();
		IBundleProvider bundleProvider = searchGoldenRecord(ImmunizationMapper.IMMUNIZATION, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			for (IBaseResource resource : bundleProvider.getAllResources()) {
				vaccinationMasterList.add((VaccinationMaster) allMappingService.localObject(resource));
			}
		}
		return vaccinationMasterList;
	}

	public List<VaccinationReported> searchVaccinationReportedList(SearchParameterMap searchParameterMap) {
		List<VaccinationReported> vaccinationReportedList = new ArrayList<>();
		IBundleProvider bundleProvider = searchRegularRecord(ImmunizationMapper.IMMUNIZATION, searchParameterMap);
		for (IBaseResource resource : bundleProvider.getAllResources()) {
			vaccinationReportedList
					.add((VaccinationReported) allMappingService.localObjectReportedWithMaster(resource));
		}
		return vaccinationReportedList;
	}

	public VaccinationReported searchVaccinationReported(SearchParameterMap searchParameterMap) {
		return (VaccinationReported) searchMappedObjectReportedWithMaster("Immunization", searchParameterMap);
	}

	public ObservationReported searchObservationReported(SearchParameterMap searchParameterMap) {
		return (ObservationReported) searchMappedObjectReportedWithMaster(ObservationMapper.OBSERVATION,
				searchParameterMap);
	}

	public ObservationMaster searchObservationMaster(SearchParameterMap searchParameterMap) {
		return (ObservationMaster) searchMappedObjectMaster(ObservationMapper.OBSERVATION, searchParameterMap);
	}

	public List<ObservationReported> searchObservationReportedList(SearchParameterMap searchParameterMap) {
		List<ObservationReported> observationReportedList = new ArrayList<>();
		IBundleProvider bundleProvider = search(ObservationMapper.OBSERVATION, searchParameterMap);
		for (IBaseResource resource : bundleProvider.getAllResources()) {
			observationReportedList
					.add((ObservationReported) allMappingService.localObjectReportedWithMaster(resource));
		}
		return observationReportedList;
	}

	public OrgLocation searchOrgLocation(SearchParameterMap searchParameterMap) {
		return (OrgLocation) searchMappedObjectMaster(LocationMapper.LOCATION, searchParameterMap);
	}

	public List<OrgLocation> searchOrgLocationList(SearchParameterMap searchParameterMap) {
		List<OrgLocation> locationList = new ArrayList<>();
		IBundleProvider bundleProvider = search(LocationMapper.LOCATION, searchParameterMap);
		for (IBaseResource resource : bundleProvider.getAllResources()) {
			locationList.add((OrgLocation) allMappingService.localObject(resource));
		}
		return locationList;
	}

	/**
	 * Helping method for saving, executes conditional update and create on HAPI
	 * DAO, adds parameter to avoid golden records
	 *
	 * @param createOnly
	 * @param resource   Resource to save
	 * @param where      HAPIFHIR Criteria list
	 * @return methodOutcome
	 */
	protected MethodOutcome save(boolean createOnly, IBaseResource resource, ICriterion... where) {
		IFhirResourceDao dao = daoRegistry.getResourceDao(resource);
		String params = FhirRequesterUtil.stringCriterionList(fhirContext, where);
		if (StringUtils.isNotBlank(params)) {
			// If not empty add &
			params += "&";
			params += NOT_GOLDEN_CRITERION;
		}
		DaoMethodOutcome outcome;
		if (createOnly) {
			return dao.create(resource, TenantUtil.get().requestDetailsWithPartitionName());
		} else
			try {
				// IUpdateTyped updateTyped =
				// repositoryClientFactory.getFhirClient().update().resource(resource);
				// if (where.length == 0) {
				// return updateTyped.execute();
				// }
				// IUpdateWithQueryTyped updateWithQueryTyped =
				// updateTyped.conditional().where(where[0]);
				// for (int i = 1; i < where.length; i++) {
				// updateWithQueryTyped = updateWithQueryTyped.and(where[i]);
				// }
				// return updateWithQueryTyped.execute();
				return dao.update(resource, params, TenantUtil.get().requestDetailsWithPartitionName());
			} catch (InvalidRequestException invalidRequestException) {
				return dao.create(resource, TenantUtil.get().requestDetailsWithPartitionName());
			}
		// catch (JdbcBatchUpdateException jdbcBatchUpdateException) {
		// return dao.create(resource,
		// TenantUtil.get().requestDetailsWithPartitionName());
		// }
	}

	@Autowired
	FhirReadRequester fhirReadRequester;

	/**
	 *
	 * @param fhirType FHIR Resource Class
	 * @param id       resource id
	 * @return resource found
	 */
	public IBaseResource read(String fhirType, String id) {
		return fhirReadRequester.read(fhirType, id);
	}

	/**
	 * Search only golden record by adding extra parameter
	 * 
	 * @param aClass             FHIR Resource Class
	 * @param searchParameterMap search parameters
	 * @return Bundle of search result with only Golden/Master records
	 */
	public IBundleProvider searchGoldenRecord(Class<? extends IBaseResource> aClass,
			SearchParameterMap searchParameterMap) {
		if (searchParameterMap == null) {
			searchParameterMap = new SearchParameterMap();
		}
		searchParameterMap.add("_tag", new TokenParam(GOLDEN_SYSTEM_TAG, GOLDEN_RECORD));
		return search(aClass, searchParameterMap);
	}

	/**
	 * Search only golden record by adding extra parameter
	 * 
	 * @param fhirType           FHIR Resource Class
	 * @param searchParameterMap search parameters
	 * @return Bundle of search result with only Golden/Master records
	 */
	public IBundleProvider searchGoldenRecord(String fhirType,
			SearchParameterMap searchParameterMap) {
		if (searchParameterMap == null) {
			searchParameterMap = new SearchParameterMap();
		}
		searchParameterMap.add("_tag", new TokenParam(GOLDEN_SYSTEM_TAG, GOLDEN_RECORD));
		return search(fhirType, searchParameterMap);
	}

	/**
	 * Search only regular record by adding extra parameter excluding golden record
	 * 
	 * @param aClass             FHIR Resource Class
	 * @param searchParameterMap Search parameters
	 * @return Bundle of search result excluding Golden/Master records
	 */
	public IBundleProvider searchRegularRecord(Class<? extends IBaseResource> aClass,
			SearchParameterMap searchParameterMap) {
		if (searchParameterMap == null) {
			searchParameterMap = new SearchParameterMap();
		}
		searchParameterMap.add("_tag",
				new TokenParam(GOLDEN_SYSTEM_TAG, GOLDEN_RECORD).setModifier(TokenParamModifier.NOT));
		return search(aClass, searchParameterMap);
	}

	/**
	 * Search only regular record by adding extra parameter excluding golden record
	 * 
	 * @param fhirType             FHIR Resource Class
	 * @param searchParameterMap Search parameters
	 * @return Bundle of search result excluding Golden/Master records
	 */
	public IBundleProvider searchRegularRecord(String fhirType,
			SearchParameterMap searchParameterMap) {
		if (searchParameterMap == null) {
			searchParameterMap = new SearchParameterMap();
		}
		searchParameterMap.add("_tag",
				new TokenParam(GOLDEN_SYSTEM_TAG, GOLDEN_RECORD).setModifier(TokenParamModifier.NOT));
		return search(fhirType, searchParameterMap);
	}

	/**
	 * Search Operation
	 * 
	 * @param aClass             FHIR Resource Class
	 * @param searchParameterMap Search parameters
	 * @return Bundle of search result
	 */
	IBundleProvider search(Class<? extends IBaseResource> aClass, SearchParameterMap searchParameterMap) {
		return daoRegistry.getResourceDao(aClass).search(searchParameterMap,
				tenantUtil.requestDetailsWithPartitionName());
	}

	/**
	 * Search Operation
	 *
	 * @param fhirType           FHIR Resource name
	 * @param searchParameterMap Search parameters
	 * @return Bundle of search result
	 */
	IBundleProvider search(String fhirType, SearchParameterMap searchParameterMap) {
		return daoRegistry.getResourceDao(fhirType).search(searchParameterMap,
				tenantUtil.requestDetailsWithPartitionName());
	}

	/**
	 * Fills multiple matched list and return Single Match
	 * Used for RSP
	 *
	 * @param multipleMatches            List to add multiple matches in
	 * @param patientMasterForMatchQuery patient Information to match
	 * @param cutoff                     cutoff date to ignore old records
	 * @return Single match result
	 */
	public abstract PatientMaster matchPatient(List<PatientReported> multipleMatches,
			PatientMaster patientMasterForMatchQuery, Date cutoff);

	/**
	 * Checks Meta and Tags
	 * 
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
