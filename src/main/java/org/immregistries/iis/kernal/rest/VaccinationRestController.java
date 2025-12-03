package org.immregistries.iis.kernal.rest;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.ReferenceParam;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.Session;

import org.immregistries.iis.kernal.HibernateConfig;
import org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.fhir.security.TenantUtil;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.model.persisted.Tenant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/rest/tenant/{tenantId}/VaccinationMaster")
public class VaccinationRestController extends BaseTenantTiedRest {

    @GetMapping("/{vaccinationId}")
    public VaccinationMaster getVaccination(
            @PathVariable int tenantId,
            @PathVariable String vaccinationId,
            HttpServletRequest req) {
        try (Session dataSession = HibernateConfig.getDataSession()) {
            Tenant tenant = TenantUtil.getTenantByIdAuthenticated(tenantId, dataSession);
            if (tenant == null) {
                return null;
            }

            return fhirRequester.readAsVaccinationMaster(vaccinationId);
        }
    }

    @GetMapping("")
    public List<VaccinationMaster> getVaccinations(
            @PathVariable int tenantId,
            @RequestParam(required = false) String patientId,
            HttpServletRequest req) {
        try (Session dataSession = HibernateConfig.getDataSession()) {
            Tenant tenant = TenantUtil.getTenantByIdAuthenticated(tenantId, dataSession);
            if (tenant == null) {
                return new ArrayList<>();
            }
            CurrentTenantUtil.getTenant(tenant.getOrganizationName(), req, dataSession);

            SearchParameterMap parameters = new SearchParameterMap();
            if (patientId != null && !patientId.isEmpty()) {
                parameters.add("patient", new ReferenceParam(patientId));
            }

            @SuppressWarnings("unchecked")
            List<VaccinationMaster> result = fhirRequester.searchVaccinationMasterGoldenList(parameters);
            return result;
        }
    }

}
