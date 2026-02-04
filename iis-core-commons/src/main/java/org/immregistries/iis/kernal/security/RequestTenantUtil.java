package org.immregistries.iis.kernal.security;

import ca.uhn.fhir.jpa.entity.PartitionEntity;
import ca.uhn.fhir.jpa.partition.IPartitionLookupSvc;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.GlobalConstants;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * static class providing tools related to the Tenant selected using request
 * context
 */
@Service
public class RequestTenantUtil {
	@Autowired
	private IPartitionLookupSvc partitionLookupSvc;
	@Autowired
	private TenantAuthService tenantAuthService;

	public Tenant getTenantFromContextRequest() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
				.getRequest();
		return getTenant(request);
	}

	public Tenant getTenant(RequestDetails theRequestDetails) {
		Object attribute = theRequestDetails.getAttribute(GlobalConstants.SESSION_REQUEST_TENANT);
		if (attribute != null) {
			return (Tenant) attribute;
		} else {
			String tenantName = theRequestDetails.getTenantId();
			if (StringUtils.isNotBlank(tenantName)) {
				Tenant tenant = getTenantFromName(tenantName);
				setTenantForRequestDetails(theRequestDetails, tenant);
				return tenant;
			}
		}
		return null;
	}

	public Tenant getTenant(HttpServletRequest request) {
		final Tenant tenant;
		/*
		 * Extracting variables from the Request
		 */
		/*
		 * If Tenant was already set as attribute return it
		 */
		Tenant requestTenant = (Tenant) request.getAttribute(GlobalConstants.SESSION_REQUEST_TENANT);
		if (requestTenant != null) {
			tenant = requestTenant;
		} else {
			String urlTenantName = (String) request.getAttribute(GlobalConstants.TENANT_NAME_URL);
			Object tenantIdUrlAttribute = request.getAttribute(GlobalConstants.TENANT_ID_URL);
			int urlTenantId = 0;
			if (tenantIdUrlAttribute != null) {
				urlTenantId = (int) tenantIdUrlAttribute;
			}

			/*
			 * if Tenant Id specified
			 * else check if name
			 */
			if (urlTenantId > 0) {
//				tenant = tenantUtil.getTenantByIdAuthenticated(urlTenantId);
//				request.setAttribute(SESSION_REQUEST_TENANT, tenant);
				tenant = null; //TODO change
			} else if (StringUtils.isNotBlank(urlTenantName)) {
				tenant = getTenantFromName(urlTenantName);
				setTenantForRequest(request, tenant);
			} else {
				tenant = null;
			}
		}
		return tenant;
	}

	private Tenant getTenantFromName(String pathVariable) {
		Tenant tenant = null;
		if (StringUtils.isNotBlank(pathVariable)) {
			UserAccess userAccess = null;
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (authentication instanceof UserAccess) {
				userAccess = (UserAccess) authentication;
			}
			tenant = tenantAuthService.authenticateTenant(userAccess, pathVariable);
		}
		return tenant;
	}


	public Tenant setTenantForRequestDetails(RequestDetails requestDetails, Tenant tenant) {
		requestDetails.setAttribute(GlobalConstants.SESSION_REQUEST_TENANT, tenant);
		return tenant;
	}

	public static Tenant setTenantForRequest(HttpServletRequest request, Tenant tenant) {
		request.setAttribute(GlobalConstants.SESSION_REQUEST_TENANT, tenant);
		return tenant;
	}

	public SystemRequestDetails requestDetailsWithPartitionName() {
		Tenant tenant = getTenantFromContextRequest();
		return requestDetailsWithPartitionName(tenant);
	}

	public @NotNull SystemRequestDetails requestDetailsWithPartitionName(Tenant tenant) {
		String organizationName = tenant.getOrganizationName();
		return requestDetailsWithPartitionName(organizationName);
	}

	public @NotNull SystemRequestDetails requestDetailsWithPartitionName(String organizationName) {
		PartitionEntity partitionEntity = partitionLookupSvc.getPartitionByName(organizationName);
		if (partitionEntity == null) {
			// return SystemRequestDetails.forAllPartitions();
			throw new RuntimeException("No partition found");
		}
		SystemRequestDetails requestDetails = SystemRequestDetails.forRequestPartitionId(partitionEntity.toRequestPartitionId());
		requestDetails.setTenantId(organizationName);
		return requestDetails;
	}


}
