package org.immregistries.iis.kernal.logic;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;
import org.immregistries.iis.kernal.fhir.CrossTenantDiffProvider;
import org.immregistries.iis.kernal.security.TenantUtil;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.persisted.model.UserAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester.GOLDEN_RECORD;
import static org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester.GOLDEN_SYSTEM_TAG;

@Service
public class TenantCompareService {
	private static final List<String> RESOURCES_TO_COMPARE = Arrays.asList("Patient", "Immunization", "Observation",
		"Organization");

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	TenantUtil tenantUtil;
	@Autowired
	private CrossTenantDiffProvider diffProvider;
	@Autowired
	private DaoRegistry daoRegistry;
	@Autowired
	private FhirContext fhirContext;

	@SuppressWarnings("rawtypes")
	public List<IBaseParameters> compareTenants(String[] tenantNames, UserAccess userAccess,
															  boolean includeGolden) {
		List<Tenant> tenantList = Arrays.stream(tenantNames).distinct()
			.map(tenantName -> tenantUtil.authenticateTenant(userAccess, tenantName))
			.collect(Collectors.toList());

		List<SystemRequestDetails> systemRequestDetailsList = tenantList.stream().map(tenant -> {
			SystemRequestDetails systemRequestDetails = new SystemRequestDetails();
			systemRequestDetails.setTenantId(tenant.getOrganizationName());
			return systemRequestDetails;
		}).collect(Collectors.toList());

		List<IBaseParameters> diffs = new ArrayList<>(100);
		List<String> resourceTypes = RESOURCES_TO_COMPARE;
		for (String resourceType : resourceTypes) {
			IFhirResourceDao resourceDao = daoRegistry.getResourceDao(resourceType);
			checkResourceType(systemRequestDetailsList, resourceDao, diffs, includeGolden);
		}
		return diffs;
	}

	private void checkResourceType(List<SystemRequestDetails> systemRequestDetailsList, IFhirResourceDao resourceDao,
											 List<IBaseParameters> diffs, Boolean includeGolden) {
		SearchParameterMap searchParameterMap = new SearchParameterMap();
		if (!includeGolden) {
			searchParameterMap.add("_tag",
				new TokenParam(GOLDEN_SYSTEM_TAG, GOLDEN_RECORD).setModifier(TokenParamModifier.NOT));
		}
		List<IBundleProvider> bundleProviderStream = systemRequestDetailsList.stream().map(
			requestDetails -> resourceDao.search(searchParameterMap, requestDetails)).collect(Collectors.toList());
		String label = resourceDao.getResourceType().getName().toLowerCase();

		int previousSize = -1;
		/**
		 * Counting found patients
		 */
		for (IBundleProvider iBundleProvider : bundleProviderStream) {
			int bundleSize = iBundleProvider.size();
			if (previousSize >= 0) {
				if (bundleSize > previousSize) {
					logger.info("Missing {}s in first tenant, {} found instead of {}", label, previousSize, bundleSize);
				} else if (bundleSize < previousSize) {
					logger.info("Missing {}s in second tenant, {} found instead of {}", label, previousSize,
						bundleSize);
				}
				// return ;
			}
			previousSize = bundleSize;
		}

		/*
		 * Diff on each patient
		 */
		SystemRequestDetails diffRequestDetail = new SystemRequestDetails();
		diffRequestDetail.setRequestPartitionId(RequestPartitionId.allPartitions());
		logger.info("Testing {}s with $diff", label);

		for (int i = 0; i < previousSize; i++) {
			IBaseResource iBaseResource1 = bundleProviderStream.get(0).getAllResources().get(i);
			IBaseResource iBaseResource2 = bundleProviderStream.get(1).getAllResources().get(i);

			IBaseParameters diff = diffProvider.diff(iBaseResource1.getIdElement(), iBaseResource2.getIdElement(),
				new BooleanType(false), diffRequestDetail);
			diffs.add(diff);
		}
	}

}
