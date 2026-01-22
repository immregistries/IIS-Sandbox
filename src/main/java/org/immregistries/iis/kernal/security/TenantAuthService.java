package org.immregistries.iis.kernal.security;

import ca.uhn.fhir.jpa.entity.PartitionEntity;
import ca.uhn.fhir.jpa.partition.IPartitionLookupSvc;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.immregistries.iis.kernal.fhir.multitenancy.PartitionTenantCreationInterceptor;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.immregistries.iis.kernal.persisted.repository.TenantRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Service
/**
 * Manages Tenant Authorization and Authentication
 *
 */
public class TenantAuthService implements InitializingBean {

	public static final List<String> FORBIDDEN_NAMES = List.of("pop", "iis", "home", "patient", "vaccination", "fhir",
			"tenant", "facility", "tenant");
	/**
	 * Needs to be statically accessible in Tenant Context
	 */
	private static TenantAuthService instance;
	@Autowired
	private TenantRepository tenantRepository;
	@Autowired
	private PartitionTenantCreationInterceptor partitionTenantCreationInterceptor;
	@Autowired
	private IPartitionLookupSvc partitionLookupSvc;

	public static TenantAuthService get() {
		return instance;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		instance = this;
	}

	public Tenant authenticateTenantNoUsername(int tenantId, String password) {
		Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
		if (tenant == null) {
			throw new RuntimeException("Invalid tenantId");
		}
		UserAccess tenantUserAccess = tenant.getUserAccess();
		String username = tenantUserAccess.getAccessName();

		UserAccess userAccess = UserAccessUtil.get().authenticateUserAccessUsernamePassword(username, password);
		return authenticateTenant(userAccess, tenantId);
	}

	public Tenant authenticateTenantNoUsername(String facilityName, String password) {
		Tenant tenant = tenantRepository.findByOrganizationName(facilityName).orElse(null);

		if (tenant == null) {
			throw new RuntimeException("Invalid tenantName");
		}
		UserAccess tenantUserAccess = tenant.getUserAccess();
		String username = tenantUserAccess.getAccessName();

		UserAccess userAccess = UserAccessUtil.get().authenticateUserAccessUsernamePassword(username, password);
		return authenticateTenant(userAccess, facilityName);
	}

	public Tenant authenticateTenant(String username, String password, String facilityName) {
		UserAccess userAccess = UserAccessUtil.get().authenticateUserAccessUsernamePassword(username, password);
		return authenticateTenant(userAccess, facilityName);
	}

	public Tenant authenticateTenant(OAuth2User oAuth2User, String facilityName) {
		/**
		 * First user authentication with OAUTH
		 */
		UserAccess userAccess = UserAccessUtil.get().authenticateUserAccessOAuth(oAuth2User);
		return authenticateTenant(userAccess, facilityName);
	}

	public Tenant authenticateTenant(UserAccess userAccess, String facilityName) {
		/**
		 * Users starting with the prefix can create a user with the same name, any
		 * other use of prefix are rejected
		 */
		if (StringUtils.isBlank(facilityName)) {
			throw new AuthenticationException();
		}
		facilityName = URLEncoder.encode(facilityName, StandardCharsets.UTF_8);
		if (facilityName.startsWith(UserAccessUtil.GITHUB_PREFIX)) { // TODO rethink
			if (!userAccess.getAccessName().startsWith(UserAccessUtil.GITHUB_PREFIX)) {
				throw new AuthenticationException();
			} else if (!facilityName.equals(userAccess.getAccessName())) {
				throw new AuthenticationException();
			}
		}

		Tenant tenant = null;

		Optional<Tenant> optional = tenantRepository.findByOrganizationName(facilityName);
		if (optional.isEmpty()) {
			tenant = registerTenant(facilityName, userAccess);
			if (partitionTenantCreationInterceptor != null) {
				partitionTenantCreationInterceptor.getOrCreatePartitionId(tenant.getOrganizationName());
			}
		} else {
			/*
			 * Important step verifying authorisation
			 */
			if (optional.get().getUserAccess().getUserAccessId() == userAccess.getUserAccessId()) {
				tenant = optional.get();
			}
		}
		return tenant;
	}


	public Tenant authenticateTenant(UserAccess userAccess, int tenantId) {
		Tenant tenant = null;
		Optional<Tenant> optional = tenantRepository.findById(tenantId);
		if (optional.get().getUserAccess().getUserAccessId() == userAccess.getUserAccessId()) {
			tenant = optional.get();
		}
		return tenant;
	}

	private Tenant registerTenant(String facilityName, UserAccess userAccess) {
		Tenant tenant = new Tenant();
		if (FORBIDDEN_NAMES.contains(facilityName) || NumberUtils.isCreatable(facilityName)) {
			throw new AuthenticationException("Tenant name: " + facilityName + " is forbidden");
		}
		tenant.setOrganizationName(facilityName);
		tenant.setUserAccess(userAccess);
		return tenantRepository.save(tenant);
	}

	public RequestDetails requestDetailsWithPartitionName() {
		PartitionEntity partitionEntity = partitionLookupSvc
				.getPartitionByName(CurrentTenantUtil.getTenant().getOrganizationName());
		if (partitionEntity == null) {
			// return SystemRequestDetails.forAllPartitions();
			throw new RuntimeException("No partition found");
		}
		RequestDetails requestDetails = SystemRequestDetails
				.forRequestPartitionId(partitionEntity.toRequestPartitionId());
		requestDetails.setTenantId(CurrentTenantUtil.getTenant().getOrganizationName());
		return requestDetails;
	}

	public Tenant getTenantByIdAuthenticated(int tenantId) {
		UserAccess userAccess = UserAccessUtil.get().getUserAccess();
		Tenant tenant = tenantRepository.findByOrgIdAndUserAccessId(tenantId, userAccess.getUserAccessId())
				.orElse(null);
		return tenant;
	}

}
