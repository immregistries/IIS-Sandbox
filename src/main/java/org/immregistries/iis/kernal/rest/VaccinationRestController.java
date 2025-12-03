package org.immregistries.iis.kernal.rest;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.ReferenceParam;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.Session;

import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.mapping.internalClient.IFhirRequester;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.model.persisted.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/rest/tenant/{tenantId}/VaccinationMaster")
public class VaccinationRestController {

    @Autowired
    @SuppressWarnings("rawtypes")
    private IFhirRequester fhirRequester;

    @GetMapping("/{vaccinationId}")
    public VaccinationMaster getVaccination(
            @PathVariable int tenantId,
            @PathVariable String vaccinationId,
            HttpServletRequest req) {
        try (Session dataSession = ServletHelper.getDataSession()) {
            Tenant tenant = ServletHelper.getTenant(tenantId, dataSession);
            if (tenant == null) {
                return null;
            }
            ServletHelper.getTenant(tenant.getOrganizationName(), req, dataSession);

            return fhirRequester.readAsVaccinationMaster(vaccinationId);
        }
    }

    @GetMapping("")
    public List<VaccinationMaster> getVaccinations(
            @PathVariable int tenantId,
            @RequestParam(required = false) String patientId,
            HttpServletRequest req) {
        try (Session dataSession = ServletHelper.getDataSession()) {
            Tenant tenant = ServletHelper.getTenant(tenantId, dataSession);
            if (tenant == null) {
                return new ArrayList<>();
            }
            ServletHelper.getTenant(tenant.getOrganizationName(), req, dataSession);

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
