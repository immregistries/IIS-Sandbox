package org.immregistries.iis.kernal.controllers.rest.shlink;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.fhir.security.TenantUtil;
import org.immregistries.iis.kernal.logic.shlink.ShLinkUtilService;
import org.immregistries.iis.kernal.persisted.model.ShLinkManifest;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.persisted.repository.ShlinkManifestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("rest/link")
public class ShLinkManifestRestController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    ShLinkUtilService shLinkUtilService;
    @Autowired
    private ShlinkManifestRepository shlinkManifestRepository;
    @Autowired
    TenantUtil tenantUtil;

    @GetMapping("/{id}")
    public ShLinkManifest getManifest(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("id") String manifestId) {
        // resp.setContentType("application/json");
        return shLinkUtilService.readShLinkManifest(manifestId);
    }

    @PostMapping("/{id}")
    protected ShLinkManifest readShLinkManifest(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("id") String manifestId,
            @RequestAttribute(CurrentTenantUtil.SESSION_REQUEST_TENANT) Tenant tenant,
            @RequestParam(value = "recipient", required = false) String recipient,
            @RequestParam(value = "passcode", required = false) String passcode,
            @RequestParam(value = "embeddedLengthMax", required = false) String embeddedLengthMax)
            throws IOException, ServletException {
        // resp.setContentType("application/json");
        if (StringUtils.isNoneBlank(passcode)) {
            // Re-authenticate logic if tenant is already present?
            // Original: tenant = tenantUtil.authenticateTenantNoUsername(passcode,
            // tenantName);
            // new: verify passcode against tenant?
            // Actually, authenticateTenantNoUsername finds tenant by name IF passcode
            // matches for that tenant?
            // If we already have the tenant, we might want to check if the passcode is
            // valid for THIS tenant.
            // But tenantUtil doesn't seem to expose checkPasscode directly without finding
            // it.
            // However, authenticateTenantNoUsername(passcode, tenant.getOrganizationName())
            // should work and return the same tenant (or null/fail).
            Tenant authenticatedTenant = null;
            {
                authenticatedTenant = tenantUtil.authenticateTenantNoUsername(passcode, tenant.getOrganizationName());
                if (authenticatedTenant == null) {
                    throw new AuthenticationCredentialsNotFoundException("Invalid passcode");
                }
                // Use authenticatedTenant or just confirm it matches 'tenant' injection?
                // Typically 'RequestAttribute' tenant comes from an authenticated context or
                // resolved context.
                // But here we are POSTing with a passcode, maybe to ACCESS a protected
                // manifest?
                // If the filter resolved the tenant by name in URL, but didn't check
                // passcode...
                // The original code used passcode AND tenantName to find/auth the tenant.
                // I will keep the authentication check.
                tenant = authenticatedTenant;
            }
        }
        ShLinkManifest shLinkManifest = shLinkUtilService.readShLinkManifest(manifestId);
        return shLinkManifest;
    }

    @GetMapping()
    public List<ShLinkManifest> getManifestAll(HttpServletRequest req, HttpServletResponse resp) {
        // resp.setContentType("application/json");
        return shlinkManifestRepository.findAll();
    }

    @GetMapping("/$generate")
    public ShLinkManifest genManifest(HttpServletRequest req, HttpServletResponse resp) {
        ShLinkManifest shLinkManifest = shLinkUtilService.generateManifest(CurrentTenantUtil.getTenant(req));
        return shLinkUtilService.saveManifest(shLinkManifest);
    }

}
