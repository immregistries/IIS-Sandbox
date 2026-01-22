package org.immregistries.iis.kernal.fhir.interceptors;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.ResourceType;
import org.immregistries.iis.kernal.mapping.mappers.resources.ImmunizationMapper;
import org.immregistries.iis.kernal.model.BusinessIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static ca.uhn.fhir.interceptor.api.Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED;
import static org.immregistries.iis.kernal.mapping.requesters.FhirSaveRequester.GOLDEN_RECORD;
import static org.immregistries.iis.kernal.mapping.requesters.FhirSaveRequester.GOLDEN_SYSTEM_TAG;

/**
 * Interceptor solving patient business identifier references to the references id if found
 *
 * @param <Immunization> FHIR Immunization class
 * @param <Group>        FHIR Group class
 * @param <Observation>  FHIR Observation class
 */
public abstract class IdentifierSolverInterceptor<Patient extends IDomainResource, Immunization extends IDomainResource, Group extends IDomainResource, Observation extends IDomainResource> {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	public static final String PATIENT_SP_IDENTIFIER = "identifier";
	@Autowired
	private FhirContext fhirContext;
	@Autowired
	private ImmunizationMapper<Immunization> immunizationMapper;

	private IFhirResourceDao<Patient> patientDao;


	@Autowired
	public void setDaoRegistry(DaoRegistry daoRegistry) {
		this.patientDao = daoRegistry.getResourceDao(ResourceType.Patient.name());
	}

	/**
	 * Resolves patient business identifier references to the actual references id if known
	 * Supports Immunization, Group, Observation
	 * TODO support other resources
	 * TODO add flavours ?
	 */
	@Hook(SERVER_INCOMING_REQUEST_PRE_HANDLED)
	public void handle(RequestDetails requestDetails) throws InvalidRequestException {
		if (requestDetails.getResource() == null || requestDetails.getRestOperationType() == null) {
			return;
		}
		if (requestDetails.getRestOperationType().equals(RestOperationTypeEnum.UPDATE) || requestDetails.getRestOperationType().equals(RestOperationTypeEnum.CREATE)) {
			String resourceType = fhirContext.getResourceType(requestDetails.getResource());
			IAnyResource resource = (IAnyResource) requestDetails.getResource();
			if (Strings.CI.equals(ResourceType.Immunization.name(), resourceType)) {
				handleImmunization(requestDetails, (Immunization) resource);
			} else if (Strings.CI.equals(ResourceType.Group.name(), resourceType)) {
				handleGroup(requestDetails, (Group) resource);
			} else if (Strings.CI.equals(ResourceType.Observation.name(), resourceType)) {
				handleObservation(requestDetails, (Observation) resource);
			}
		}
	}

	/**
	 * Handle Immunization
	 *
	 * @param requestDetails requestDetails to extract RequestPartitionId From and set new resource in
	 * @param immunization   Resource
	 */
	abstract void handleImmunization(RequestDetails requestDetails, Immunization immunization);

	/**
	 * Handle Observation
	 *
	 * @param requestDetails requestDetails to extract RequestPartitionId From and set new resource in
	 * @param observation    Resource
	 */
	abstract void handleObservation(RequestDetails requestDetails, Observation observation);

	/**
	 * Handle Group and its members
	 *
	 * @param requestDetails requestDetails to extract RequestPartitionId From and set new resource in
	 * @param group          Resource
	 */
	abstract void handleGroup(RequestDetails requestDetails, Group group);

	/**
	 * Searches for PatientId matching Patient Identifier
	 *
	 * @param requestDetails RequestDetails to extract Partition Id from
	 * @param identifier     identifier
	 * @return Patient id or null
	 */
	public String solvePatientIdentifier(RequestDetails requestDetails, BusinessIdentifier identifier) {
		RequestPartitionId thePartitionId = RequestPartitionId.fromPartitionName(PartitionTenantCreationInterceptor.extractPartitionName(requestDetails));
		return solvePatientIdentifier(thePartitionId, identifier);
	}

	/**
	 * Searches for PatientId matching Patient Identifier
	 *
	 * @param thePartitionId PartitionId used to search
	 * @param identifier     identifier
	 * @return Patient id or null
	 */
	public String solvePatientIdentifier(RequestPartitionId thePartitionId, BusinessIdentifier identifier) {
		SystemRequestDetails systemRequestDetails = SystemRequestDetails.forRequestPartitionId(thePartitionId);
		String id = null;
		/*
		 * searching for matching patient golden record first
		 */
		SearchParameterMap goldenSearchParameterMap = new SearchParameterMap()
			.add("_tag", new TokenParam()
				.setSystem(GOLDEN_SYSTEM_TAG)
				.setValue(GOLDEN_RECORD));
		if (StringUtils.isNotBlank(identifier.getSystem())) {
			goldenSearchParameterMap.add(PATIENT_SP_IDENTIFIER, new TokenParam()
				.setSystem(identifier.getSystem())
				.setValue(identifier.getValue()));
		} else {
			goldenSearchParameterMap.add(PATIENT_SP_IDENTIFIER, new TokenParam()
				.setValue(identifier.getValue()));
		}

		// TODO get golden record, or merge and add identifiers to golden record
		IBundleProvider goldenBundleProvider = patientDao.search(goldenSearchParameterMap, systemRequestDetails);
		if (!goldenBundleProvider.isEmpty()) {
			id = goldenBundleProvider.getAllResourceIds().get(0);
		} else {
			/*
			 * If no golden record matched, regular records are checked
			 */
			// TODO set flavor
			SearchParameterMap searchParameterMap = new SearchParameterMap();
			if (StringUtils.isNotBlank(identifier.getSystem())) {
				searchParameterMap.add(PATIENT_SP_IDENTIFIER, new TokenParam()
					.setSystem(identifier.getSystem())
					.setValue(identifier.getValue()));
			} else {
				searchParameterMap.add(PATIENT_SP_IDENTIFIER, new TokenParam()
					.setValue(identifier.getValue()));
			}

			IBundleProvider bundleProvider = patientDao.search(searchParameterMap, systemRequestDetails);
			if (!bundleProvider.isEmpty()) {
				id = bundleProvider.getAllResourceIds().get(0);
			}
		}
		return id;
	}


}
