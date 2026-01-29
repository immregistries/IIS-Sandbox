package org.immregistries.iis.kernal.mapping.requesters;

import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.ResourceType;
import org.immregistries.iis.kernal.mapping.mappers.fields.BusinessIdentifierMapper;
import org.immregistries.iis.kernal.model.BusinessIdentifier;
import org.immregistries.iis.kernal.services.PartitionNameExtractorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.immregistries.iis.kernal.mapping.requesters.FhirSaveRequester.GOLDEN_RECORD;
import static org.immregistries.iis.kernal.mapping.requesters.FhirSaveRequester.GOLDEN_SYSTEM_TAG;

@Service
public class FhirIdentifierSolver<Identifier extends IBaseDatatype, Patient extends IDomainResource> {
	public static final String PATIENT_SP_IDENTIFIER = "identifier";

	@Autowired
	private BusinessIdentifierMapper<Identifier> businessIdentifierMapper;

	private IFhirResourceDao<Patient> patientDao;

	@Autowired
	private PartitionNameExtractorService partitionNameExtractorService;

	@Autowired
	public void setDaoRegistry(DaoRegistry daoRegistry) {
		this.patientDao = daoRegistry.getResourceDao(ResourceType.Patient.name());
	}


	public String solvePatientIdentifier(RequestDetails requestDetails, Identifier identifier) {
		return solvePatientIdentifier(requestDetails, businessIdentifierMapper.localObject(identifier));
	}

	/**
	 * Searches for PatientId matching Patient Identifier
	 *
	 * @param requestDetails RequestDetails to extract Partition Id from
	 * @param identifier     identifier
	 * @return Patient id or null
	 */
	public String solvePatientIdentifier(RequestDetails requestDetails, BusinessIdentifier identifier) {
		RequestPartitionId thePartitionId = RequestPartitionId.fromPartitionName(partitionNameExtractorService.extractPartitionName(requestDetails));
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
