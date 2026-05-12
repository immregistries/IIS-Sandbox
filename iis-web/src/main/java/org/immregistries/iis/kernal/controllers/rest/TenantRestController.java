package org.immregistries.iis.kernal.controllers.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.immregistries.iis.kernal.controllers.IisPathVariable;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.immregistries.iis.kernal.persisted.repository.TenantRepository;
import org.immregistries.iis.kernal.security.TenantAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @GetMapping(IisPathVariable.PlaceHolder.TENANT_ID_PLACEHOLDER)
	 public Tenant getTenant(@AuthenticationPrincipal UserAccess userAccess, @PathVariable(PARAM_TENANT_ID) int tenantId) {
        return tenantRepository.findByOrgIdAndUserAccessId(tenantId, userAccess.getUserAccessId())
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
    }

    @GetMapping
	 public List<Tenant> getTenants(@AuthenticationPrincipal UserAccess userAccess, HttpServletRequest req) {
		 return tenantRepository.findByUserAccessId(userAccess.getUserAccessId());
    }

    @PostMapping
	 public Tenant createTenant(@AuthenticationPrincipal UserAccess userAccess, @RequestBody Tenant tenant) {
		 if (tenant.getUserAccess() != null && !tenant.getUserAccess().equals(userAccess)) {
            throw new IllegalArgumentException("Tenant UserAccess must be null or match the current user");
        }
        // TODO prevent duplicate tenant creation
		 tenantAuthService.authenticateTenant(userAccess, tenant.getOrganizationName());
		 tenant.setUserAccess(userAccess);
        tenant = tenantRepository.save(tenant);
        return tenant;
    }

}
