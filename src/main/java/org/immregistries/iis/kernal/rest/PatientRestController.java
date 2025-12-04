package org.immregistries.iis.kernal.rest;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.Session;

import org.immregistries.iis.kernal.HibernateConfig;
import org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.persisted.Tenant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/rest/tenant/{tenantId}/patientMaster")
public class PatientRestController extends BaseTenantTiedRest {

    @GetMapping("/{patientId}")
    public PatientMaster getPatient(
            @PathVariable String patientId,
            HttpServletRequest req) {
        try (Session dataSession = HibernateConfig.getDataSession()) {
            return fhirRequester.readAsPatientMaster(patientId);
        }
    }

    @GetMapping("")
    public List<PatientMaster> getAllPatients(
            HttpServletRequest req) {
        try (Session dataSession = HibernateConfig.getDataSession()) {
            Tenant tenant = CurrentTenantUtil.getTenant(req, dataSession);
            if (tenant == null) {
                return new ArrayList<>();
            }

            @SuppressWarnings("unchecked")
            List<PatientMaster> result = fhirRequester.searchPatientMasterGoldenList(new SearchParameterMap());
            return result;
        }
    }
}
