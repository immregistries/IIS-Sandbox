package org.immregistries.iis.kernal.mapping.requesters;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.immregistries.iis.kernal.mapping.IisFhirClientFactory;
import org.immregistries.iis.kernal.mapping.MappingService;
import org.immregistries.iis.kernal.mapping.mappers.resources.*;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.iis.kernal.security.TenantUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.immregistries.iis.kernal.mapping.mappers.resources.ImmunizationMapper.IMMUNIZATION;

@Service
public class FhirSearchRequester {

	@Autowired
	IisFhirClientFactory iisFhirClientFactory;
	@Autowired
	DaoRegistry daoRegistry;
	@Autowired
	MappingService mappingService;
	@Autowired
	PatientMapper patientMapper;

	@Autowired
	TenantUtil tenantUtil;

	@Autowired
	FhirReadRequester fhirReadRequester;

	public IisMappedToFhirResource searchMappedObjectMaster(String resourceType, SearchParameterMap searchParameterMap) {
		IisMappedToFhirResource mappedObject = null;
		IBundleProvider bundleProvider = searchGoldenRecord(resourceType, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			mappedObject = mappingService.localObject((IAnyResource) bundleProvider.getResources(0, 1).get(0));
		}
		return mappedObject;
	}

	public IisMappedToFhirResource searchMappedObjectReportedWithMaster(String resourceType,
																							  SearchParameterMap searchParameterMap) {
		IisMappedToFhirResource mappedObject = null;
		IBundleProvider bundleProvider = searchRegularRecord(resourceType, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			mappedObject = mappingService.localObjectReportedWithMaster((IAnyResource) bundleProvider.getResources(0, 1).get(0));
		}
		return mappedObject;
	}

	public List<IisMappedToFhirResource> searchMappedObjectReportedList(String resourceType,
																							  SearchParameterMap searchParameterMap) {
		IBundleProvider bundleProvider = searchRegularRecord(resourceType, searchParameterMap);
		return bundleProvider.getAllResources().stream().map(IAnyResource.class::cast).map(mappingService::localObjectReportedWithMaster)
			.collect(Collectors.toList());
	}

	public List<VaccinationMaster> searchVaccinationMasterGoldenList(SearchParameterMap searchParameterMap) {
		List<VaccinationMaster> vaccinationMasterList = new ArrayList<VaccinationMaster>();
		IBundleProvider bundleProvider = searchGoldenRecord(ImmunizationMapper.IMMUNIZATION, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			for (IBaseResource resource : bundleProvider.getAllResources()) {
				vaccinationMasterList.add((VaccinationMaster) mappingService.localObjectMaster((IAnyResource) resource));
			}
		}
		return vaccinationMasterList;
	}

	public List<VaccinationReported> searchVaccinationReportedList(SearchParameterMap searchParameterMap) {
		List<VaccinationReported> vaccinationReportedList = new ArrayList<VaccinationReported>();
		IBundleProvider bundleProvider = searchRegularRecord(ImmunizationMapper.IMMUNIZATION, searchParameterMap);
		for (IBaseResource resource : bundleProvider.getAllResources()) {
			vaccinationReportedList
				.add((VaccinationReported) mappingService.localObjectReportedWithMaster((IAnyResource) resource));
		}
		return vaccinationReportedList;
	}

	public VaccinationReported searchVaccinationReported(SearchParameterMap searchParameterMap) {
		return (VaccinationReported) searchMappedObjectReportedWithMaster(ImmunizationMapper.IMMUNIZATION, searchParameterMap);
	}

	public VaccinationMaster searchVaccinationMaster(SearchParameterMap searchParameterMap) {
		return (VaccinationMaster) searchMappedObjectMaster(ImmunizationMapper.IMMUNIZATION, searchParameterMap);
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
				.add((ObservationReported) mappingService.localObjectReportedWithMaster((IAnyResource) resource));
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
			locationList.add((OrgLocation) mappingService.localObject((IAnyResource) resource));
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
		searchParameterMap.add("_tag", new TokenParam(FhirSaveRequester.GOLDEN_SYSTEM_TAG, FhirSaveRequester.GOLDEN_RECORD));
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
		searchParameterMap.add("_tag", new TokenParam(FhirSaveRequester.GOLDEN_SYSTEM_TAG, FhirSaveRequester.GOLDEN_RECORD));
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
			new TokenParam(FhirSaveRequester.GOLDEN_SYSTEM_TAG, FhirSaveRequester.GOLDEN_RECORD).setModifier(TokenParamModifier.NOT));
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
			new TokenParam(FhirSaveRequester.GOLDEN_SYSTEM_TAG, FhirSaveRequester.GOLDEN_RECORD).setModifier(TokenParamModifier.NOT));
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
					.add((PatientReported) mappingService.localObjectReportedWithMaster((org.hl7.fhir.r4.model.Patient) resource));
			}
		}
		return patientReportedList;
	}

	public List<PatientMaster> searchPatientMasterGoldenList(SearchParameterMap searchParameterMap) {
		List<PatientMaster> patientList = new ArrayList<>();
		IBundleProvider bundleProvider = searchGoldenRecord(PatientMapper.PATIENT, searchParameterMap);
		if (!bundleProvider.isEmpty()) {
			for (IBaseResource resource : bundleProvider.getAllResources()) {
				patientList.add((PatientMaster) mappingService.localObject((IAnyResource) resource));
			}
		}
		return patientList;
	}

	public List<PatientReported> searchPatientReportedFromGoldenIdWithMdmLinks(String patientMasterId) {
		return fhirReadRequester.readMdmlinksReportedIds(patientMasterId)
			.map(fhirReadRequester::readAsPatientReported)
			.collect(Collectors.toList());
	}

	public List<VaccinationReported> searchVaccinationReportedFromGoldenIdWithMdmLinks(String vaccinationMasterId) {
		return fhirReadRequester.readMdmlinksReportedIds(vaccinationMasterId)
			.map(fhirReadRequester::readAsVaccinationReported)
			.collect(Collectors.toList());
	}

	public List<VaccinationMaster> searchVaccinationMasterFromGoldenIdWithMdmLinks(String vaccinationMasterId) {
		return fhirReadRequester.readMdmlinksReportedIds(vaccinationMasterId)
			.map(fhirReadRequester::readAsVaccinationMaster)
			.collect(Collectors.toList());
	}

	public ModelPerson searchPractitioner(SearchParameterMap searchParameterMap) {
		return (ModelPerson) searchMappedObjectMaster(PractitionerMapper.PRACTITIONER, searchParameterMap);
	}

	public List<VaccinationMaster> searchVaccinationListOperationEverything(String patientId) {
		IGenericClient client = iisFhirClientFactory.getOrCreateFhirClientFromContext();
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
				if (FhirRequesterUtil.isGoldenRecord(entry.getResource())) {
					VaccinationMaster vaccinationMaster = (VaccinationMaster) mappingService
						.localObject(entry.getResource());
					if (vaccinationMaster != null) {
						vaccinationList.add(vaccinationMaster);
					}
				}
			}
		}
		return vaccinationList;
	}

	public org.hl7.fhir.r5.model.Organization searchOrganizationR5(SearchParameterMap searchParameterMap) {
		IBundleProvider bundleProvider = search(OrganizationMapper.ORGANIZATION, searchParameterMap);
		return (org.hl7.fhir.r5.model.Organization) bundleProvider.getAllResources().stream().findFirst().orElse(null);
	}
	public org.hl7.fhir.r4.model.Organization searchOrganizationR4(SearchParameterMap searchParameterMap) {
		IBundleProvider bundleProvider = search(OrganizationMapper.ORGANIZATION, searchParameterMap);
		return (org.hl7.fhir.r4.model.Organization) bundleProvider.getAllResources().stream().findFirst().orElse(null);
	}
	//	public RelatedPerson searchRelatedPerson(SearchParameterMap searchParameterMap) {
//		RelatedPerson relatedPerson = null;
//		IBundleProvider bundleProvider = fhirSearchRequester.search(RelatedPerson.class, searchParameterMap);
//		if (!bundleProvider.isEmpty()) {
//			relatedPerson = (RelatedPerson) bundleProvider.getResources(0, 1).get(0);
//		}
//		return relatedPerson;
//	}

}