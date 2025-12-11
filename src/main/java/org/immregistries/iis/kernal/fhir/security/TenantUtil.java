package org.immregistries.iis.kernal.fhir.security;

import ca.uhn.fhir.jpa.entity.PartitionEntity;
import ca.uhn.fhir.jpa.partition.IPartitionLookupSvc;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import jakarta.persistence.TypedQuery;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.immregistries.iis.kernal.fhir.Application;
import org.immregistries.iis.kernal.fhir.interceptors.PartitionTenantCreationInterceptor;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.persisted.model.UserAccess;
import org.immregistries.iis.kernal.persisted.repository.TenantRepository;
import org.immregistries.iis.kernal.servlet.TenantController;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class TenantUtil implements ApplicationContextAware {

    /**
     * Singleton to access the application context for the repositories
     */
    private static ApplicationContext ac;

    private static TenantRepository tenantRepository;
    private static IPartitionLookupSvc partitionLookupSvc;
    private static PartitionTenantCreationInterceptor partitionTenantCreationInterceptor;

    @Override
    public void setApplicationContext(ApplicationContext ac) {
        TenantUtil.ac = ac;
    }

    public static IPartitionLookupSvc getPartitionLookupSvc() {
        if (partitionLookupSvc == null) {
            partitionLookupSvc = ac.getBean(IPartitionLookupSvc.class);
        }
        return partitionLookupSvc;
    }

    public static PartitionTenantCreationInterceptor getPartitionTenantCreationInterceptor() {
        if (partitionTenantCreationInterceptor == null) {
            partitionTenantCreationInterceptor = ac.getBean(PartitionTenantCreationInterceptor.class);
        }
        return partitionTenantCreationInterceptor;
    }

    public static TenantRepository getTenantRepository() {
        if (tenantRepository == null) {
            tenantRepository = ac.getBean(TenantRepository.class);
        }
        return tenantRepository;
    }

    public static final List<String> FORBIDDEN_NAMES = List.of("pop", "iis", "home", "patient", "vaccination", "fhir",
            "tenant", "facility", "tenant");

    /**
     * Adds tenant prefix to urlSuffix if tenant is not null
     * Used for links in the UI with href
     *
     * @param tenant    tenant
     * @param urlSuffix
     * @return {tenantBasePath}/{tenantName}/ + urlSuffix
     */
    public static String tenantifyPathWithContextPath(Tenant tenant, String urlSuffix) {
        if (tenant == null || tenant.getOrgId() < 0) {
            return urlSuffix;
        }
        String organizationName = tenant.getOrganizationName();
        return Application.IIS_PATH_BASE + tenantifyPathSuffix(organizationName, urlSuffix);
    }

    /**
     * Standardized converting url Suffix with tenant Name and variable,
     * Automatically adds / character if needed
     *
     * can also be used for security config with * as tenantName
     *
     * @param tenantName organisation name
     * @param urlSuffix
     * @return {tenantBasePath}/{tenantName}/ + urlSuffix
     */
    public static @NotNull String tenantifyPathSuffix(String tenantName, String urlSuffix) {
        if (!StringUtils.startsWith(urlSuffix, "/")) {
            urlSuffix = "/" + urlSuffix;
        }
        return TenantController.TENANT_BASE_PATH + "/" + tenantName + urlSuffix;
    }

    /**
     * Deals with tenantName path variable for Authorization config
     *
     * @param urlSuffix
     * @return
     */
    public static @NotNull String securityConfigUrl(String urlSuffix) {
        return tenantifyPathSuffix("*", urlSuffix);
    }

    public static Tenant authenticateTenantNoUsername(String password, String facilityName) {
        Tenant tenant = getTenantRepository().findByOrganizationName(facilityName).orElse(null);

        if (tenant == null) {
            throw new RuntimeException("Invalid tenantName");
        }
        UserAccess tenantUserAccess = tenant.getUserAccess();
        String username = tenantUserAccess.getAccessName();

        UserAccess userAccess = UserAccessUtil.authenticateUserAccessUsernamePassword(username, password);
        return authenticateTenant(userAccess, facilityName);
    }

    public static Tenant authenticateTenant(String username, String password, String facilityName) {
        UserAccess userAccess = UserAccessUtil.authenticateUserAccessUsernamePassword(username, password);
        return authenticateTenant(userAccess, facilityName);
    }

    public static Tenant authenticateTenant(OAuth2User oAuth2User, String facilityName) {
        /**
         * First user authentication with OAUTH
         */
        UserAccess userAccess = UserAccessUtil.authenticateUserAccessOAuth(oAuth2User);
        return authenticateTenant(userAccess, facilityName);
    }

    public static Tenant authenticateTenant(UserAccess userAccess, String facilityName) {
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

        Optional<Tenant> optional = getTenantRepository().findByOrganizationName(facilityName);
        if (optional.isEmpty()) {
            tenant = registerTenant(facilityName, userAccess);
            if (getPartitionTenantCreationInterceptor() != null) {
                getPartitionTenantCreationInterceptor().getOrCreatePartitionId(tenant.getOrganizationName());
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

    public static Tenant registerTenant(String facilityName, UserAccess userAccess) {
        Tenant tenant = new Tenant();
        if (FORBIDDEN_NAMES.contains(facilityName)) {
            throw new RuntimeException("Tenant name: " + facilityName + " is forbidden");
        }
        tenant.setOrganizationName(facilityName);
        tenant.setUserAccess(userAccess);
        return getTenantRepository().save(tenant);
    }

    public static RequestDetails requestDetailsWithPartitionName() {
        PartitionEntity partitionEntity = getPartitionLookupSvc()
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

    public static Tenant getTenantByIdAuthenticated(int tenantId) {
        UserAccess userAccess = UserAccessUtil.getUserAccess();
        Tenant tenant = getTenantRepository().findByIdAndUserAccessId(tenantId, userAccess.getUserAccessId())
                .orElse(null);
        return tenant;
    }

}
