package org.immregistries.iis.kernal.security;

import ca.uhn.fhir.jpa.partition.IPartitionLookupSvc;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.immregistries.iis.kernal.persisted.repository.TenantRepository;
import org.immregistries.iis.kernal.services.PartitionCreationService;
import org.immregistries.iis.kernal.services.PartitionNameExtractorService;
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
public class TenantAuthService {

	public static final List<String> FORBIDDEN_NAMES = List.of("pop", "iis", "home", "patient", "vaccination", "fhir",
			"tenant", "facility", "tenant");

	@Autowired
	private TenantRepository tenantRepository;
	@Autowired
	private PartitionNameExtractorService partitionNameExtractorService;
	@Autowired
	private PartitionCreationService partitionCreationService;
	@Autowired
	private IPartitionLookupSvc partitionLookupSvc;
	@Autowired
	private UserAccessUtil userAccessUtil;

	public Tenant authenticateTenantNoUsername(int tenantId, String password) {
		Tenant tenant = tenantRepository.findById(tenantId)
			.orElseThrow(() -> new RuntimeException("Invalid tenant id"));

		UserAccess tenantUserAccess = tenant.getUserAccess();
		String username = tenantUserAccess.getAccessName();

		UserAccess userAccess = userAccessUtil.authenticateUserAccessUsernamePassword(username, password);
		return authenticateTenant(userAccess, tenantId);
	}

	public Tenant authenticateTenantNoUsername(String facilityName, String password) {
		Tenant tenant = tenantRepository.findByOrganizationName(facilityName)
			.orElseThrow(() -> new RuntimeException("Invalid tenant name"));

		UserAccess tenantUserAccess = tenant.getUserAccess();
		String username = tenantUserAccess.getAccessName();

		UserAccess userAccess = userAccessUtil.authenticateUserAccessUsernamePassword(username, password);
		return authenticateTenant(userAccess, facilityName);
	}

	public Tenant authenticateTenant(String username, String password, String facilityName) {
		UserAccess userAccess = userAccessUtil.authenticateUserAccessUsernamePassword(username, password);
		return authenticateTenant(userAccess, facilityName);
	}

	public Tenant authenticateTenant(OAuth2User oAuth2User, String facilityName) {
		/**
		 * First user authentication with OAUTH
		 */
		UserAccess userAccess = userAccessUtil.authenticateUserAccessOAuth(oAuth2User);
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
			if (partitionCreationService != null) {
				partitionCreationService.getOrCreatePartitionId(tenant.getOrganizationName());
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
		if (EnumUtils.isValidEnum(IisRestPath.RestKey.class, facilityName) || FORBIDDEN_NAMES.contains(facilityName) || NumberUtils.isCreatable(facilityName)) {
			throw new AuthenticationException("Tenant name: " + facilityName + " is forbidden");
		}
		tenant.setOrganizationName(facilityName);
		tenant.setUserAccess(userAccess);
		return tenantRepository.save(tenant);
	}

	public Tenant getTenantByIdAuthenticated(int tenantId) {
		UserAccess userAccess = userAccessUtil.getUserAccess();
		Tenant tenant = tenantRepository.findByOrgIdAndUserAccessId(tenantId, userAccess.getUserAccessId())
				.orElse(null);
		return tenant;
	}

}
