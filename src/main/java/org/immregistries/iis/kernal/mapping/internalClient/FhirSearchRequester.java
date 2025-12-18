package org.immregistries.iis.kernal.mapping.internalClient;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.fhir.security.TenantUtil;
import org.immregistries.iis.kernal.mapping.AllMappingService;
import org.immregistries.iis.kernal.mapping.interfaces.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.interfaces.LocationMapper;
import org.immregistries.iis.kernal.mapping.interfaces.ObservationMapper;
import org.immregistries.iis.kernal.mapping.interfaces.PatientMapper;
import org.immregistries.iis.kernal.model.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FhirSearchRequester {
	@Autowired
	DaoRegistry daoRegistry;
	@Autowired
	AllMappingService allMappingService;

	@Autowired
	TenantUtil tenantUtil;

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
		List<VaccinationMaster> vaccinationMasterList = new ArrayList<VaccinationMaster>();
		IBundleProvider bundleProvider = searchGoldenRecord(ImmunizationMapper.IMMUNIZATION, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			for (IBaseResource resource : bundleProvider.getAllResources()) {
				vaccinationMasterList.add((VaccinationMaster) allMappingService.localObject(resource));
			}
		}
		return vaccinationMasterList;
	}

	public List<VaccinationReported> searchVaccinationReportedList(SearchParameterMap searchParameterMap) {
		List<VaccinationReported> vaccinationReportedList = new ArrayList<VaccinationReported>();
		IBundleProvider bundleProvider = searchRegularRecord(ImmunizationMapper.IMMUNIZATION, searchParameterMap);
		for (IBaseResource resource : bundleProvider.getAllResources()) {
			vaccinationReportedList
				.add((VaccinationReported) allMappingService.localObjectReportedWithMaster(resource));
		}
		return vaccinationReportedList;
	}

	public VaccinationReported searchVaccinationReported(SearchParameterMap searchParameterMap) {
		return (VaccinationReported) searchMappedObjectReportedWithMaster(ImmunizationMapper.IMMUNIZATION, searchParameterMap);
	}

	public ObservationReported searchObservationReported(SearchParameterMap searchParameterMap) {
		return (ObservationReported) searchMappedObjectReportedWithMaster(ObservationMapper.OBSERVATION,
			searchParameterMap);
	}

	public ObservationMaster searchObservationMaster(SearchParameterMap searchParameterMap) {
		return (ObservationMaster) searchMappedObjectMaster(ObservationMapper.OBSERVATION, searchParameterMap);
	}

	public List<ObservationReported> searchObservationReportedList(SearchParameterMap searchParameterMap) {
		List<ObservationReported> observationReportedList = new ArrayList<ObservationReported>();
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
		List<OrgLocation> locationList = new ArrayList<OrgLocation>();
		IBundleProvider bundleProvider = search(LocationMapper.LOCATION, searchParameterMap);
		for (IBaseResource resource : bundleProvider.getAllResources()) {
			locationList.add((OrgLocation) allMappingService.localObject(resource));
		}
		return locationList;
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
		searchParameterMap.add("_tag", new TokenParam(AbstractFhirRequester.GOLDEN_SYSTEM_TAG, AbstractFhirRequester.GOLDEN_RECORD));
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
		searchParameterMap.add("_tag", new TokenParam(AbstractFhirRequester.GOLDEN_SYSTEM_TAG, AbstractFhirRequester.GOLDEN_RECORD));
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
			new TokenParam(AbstractFhirRequester.GOLDEN_SYSTEM_TAG, AbstractFhirRequester.GOLDEN_RECORD).setModifier(TokenParamModifier.NOT));
		return search(aClass, searchParameterMap);
	}

	/**
	 * Search only regular record by adding extra parameter excluding golden record
	 *
	 * @param fhirType           FHIR Resource Class
	 * @param searchParameterMap Search parameters
	 * @return Bundle of search result excluding Golden/Master records
	 */
	public IBundleProvider searchRegularRecord(String fhirType,
															 SearchParameterMap searchParameterMap) {
		if (searchParameterMap == null) {
			searchParameterMap = new SearchParameterMap();
		}
		searchParameterMap.add("_tag",
			new TokenParam(AbstractFhirRequester.GOLDEN_SYSTEM_TAG, AbstractFhirRequester.GOLDEN_RECORD).setModifier(TokenParamModifier.NOT));
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


	public List<PatientReported> searchPatientReportedList(SearchParameterMap searchParameterMap) {
		List<PatientReported> patientReportedList = new ArrayList<>();
		IBundleProvider bundleProvider = searchRegularRecord(PatientMapper.PATIENT, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			for (IBaseResource resource : bundleProvider.getAllResources()) {
				patientReportedList
					.add((PatientReported) allMappingService.localObjectReportedWithMaster((org.hl7.fhir.r4.model.Patient) resource));
			}
		}
		return patientReportedList;
	}

	public List<PatientMaster> searchPatientMasterGoldenList(SearchParameterMap searchParameterMap) {
		List<PatientMaster> patientList = new ArrayList<>();
		IBundleProvider bundleProvider = searchGoldenRecord(PatientMapper.PATIENT, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			for (IBaseResource resource : bundleProvider.getAllResources()) {
				patientList.add((PatientMaster) allMappingService.localObject(resource));
			}
		}
		return patientList;
	}
}