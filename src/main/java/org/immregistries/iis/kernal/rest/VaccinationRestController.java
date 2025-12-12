package org.immregistries.iis.kernal.rest;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.ReferenceParam;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.Session;
import org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.fhir.security.TenantUtil;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.persisted.util.HibernateConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil.SESSION_REQUEST_TENANT;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/rest/tenant/{tenantId}/VaccinationMaster")
public class VaccinationRestController extends BaseTenantTiedRest {

    @GetMapping("/{vaccinationId}")
    public VaccinationMaster getVaccination(
            @RequestAttribute(name = SESSION_REQUEST_TENANT) Tenant tenant,
            @PathVariable("vaccinationId") String vaccinationId,
            HttpServletRequest req) {
        // TODO make FHIr Requester tenant aware
        return fhirRequester.readAsVaccinationMaster(vaccinationId);
    }

    @GetMapping("")
    public List<VaccinationMaster> getVaccinations(
            @RequestAttribute(name = SESSION_REQUEST_TENANT) Tenant tenant,
            @RequestParam(required = false) String patientId,
            HttpServletRequest req) {

        SearchParameterMap parameters = new SearchParameterMap();
        if (patientId != null && !patientId.isEmpty()) {
            parameters.add("patient", new ReferenceParam(patientId));
        }

        @SuppressWarnings("unchecked")
        List<VaccinationMaster> result = fhirRequester.searchVaccinationMasterGoldenList(parameters);
        return result;
    }

}
