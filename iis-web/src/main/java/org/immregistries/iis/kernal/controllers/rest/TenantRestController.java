package org.immregistries.iis.kernal.controllers.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.immregistries.iis.kernal.controllers.IisPathVariable;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.immregistries.iis.kernal.persisted.repository.TenantRepository;
import org.immregistries.iis.kernal.security.TenantAuthService;
import org.immregistries.iis.kernal.security.UserAccessUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.immregistries.iis.kernal.controllers.servlet.TenantController.PARAM_TENANT_ID;

@RestController
@RequestMapping(IisRestPath.BasePath.REST_PATH + IisRestPath.BasePath.TENANT_PATH)
public class TenantRestController {

    @Autowired
	 private TenantRepository tenantRepository;
    @Autowired
	 private TenantAuthService tenantAuthService;
    @Autowired
	 private UserAccessUtil userAccessUtil;

    @GetMapping(IisPathVariable.PlaceHolder.TENANT_ID_PLACEHOLDER)
    public Tenant getTenant(@PathVariable(PARAM_TENANT_ID) int tenantId) {
        UserAccess userAccess = userAccessUtil.getUserAccess();
        return tenantRepository.findByOrgIdAndUserAccessId(tenantId, userAccess.getUserAccessId())
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
    }

    @GetMapping
    public List<Tenant> getTenants(HttpServletRequest req) {
		 return tenantRepository.findByUserAccessId(userAccessUtil.getUserAccess().getUserAccessId());
    }

    @PostMapping
    public Tenant createTenant(@RequestBody Tenant tenant) {
		 UserAccess currentUser = userAccessUtil.getUserAccess();
        if (tenant.getUserAccess() != null && !tenant.getUserAccess().equals(currentUser)) {
            throw new IllegalArgumentException("Tenant UserAccess must be null or match the current user");
        }
        // TODO prevent duplicate tenant creation
        tenantAuthService.authenticateTenant(currentUser, tenant.getOrganizationName());
        tenant.setUserAccess(currentUser);
        tenant = tenantRepository.save(tenant);
        return tenant;
    }

}
