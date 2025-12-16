package org.immregistries.iis.kernal.controllers.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.immregistries.iis.kernal.fhir.interceptors.PartitionTenantCreationInterceptor;
import org.immregistries.iis.kernal.fhir.security.TenantUtil;
import org.immregistries.iis.kernal.fhir.security.UserAccessUtil;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.persisted.model.UserAccess;
import org.immregistries.iis.kernal.persisted.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rest/tenant")
public class TenantRestController {

    @Autowired
    private PartitionTenantCreationInterceptor partitionTenantCreationInterceptor;

    @Autowired
    TenantRepository tenantRepository;
	@Autowired
	TenantUtil tenantUtil;
	@Autowired
	UserAccessUtil userAccessUtil;

    @GetMapping("/{tenantId}")
    public Tenant getTenant(@PathVariable("tenantId") int tenantId) {
		 UserAccess userAccess = userAccessUtil.getUserAccess();
		 return tenantRepository.findByOrgIdAndUserAccessId(tenantId, userAccess.getUserAccessId())
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
    }

    @GetMapping
    public List<Tenant> getTenants(HttpServletRequest req) {
		 return tenantRepository.findByUserAccessId(UserAccessUtil.get().getUserAccess().getUserAccessId());
    }

    @PostMapping
    public Tenant createTenant(@RequestBody Tenant tenant) {
		 UserAccess currentUser = UserAccessUtil.get().getUserAccess();
        if (tenant.getUserAccess() != null && !tenant.getUserAccess().equals(currentUser)) {
            throw new IllegalArgumentException("Tenant UserAccess must be null or match the current user");
        }
        // TODO prevent duplicate tenant creation
		 tenantUtil.authenticateTenant(currentUser, tenant.getOrganizationName());
        tenant.setUserAccess(currentUser);
        tenant = tenantRepository.save(tenant);
        return tenant;
    }

}
