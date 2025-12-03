package org.immregistries.iis.kernal.rest;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.mapping.interfaces.PatientMapper;
import org.immregistries.iis.kernal.mapping.internalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.persisted.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/tenant")
public class TenantRestController {

    @Autowired
    private RepositoryClientFactory repositoryClientFactory;

    @Autowired
    private PatientMapper patientMapper;

    @GetMapping("/{tenantId}")
    public Tenant getTenant(@PathVariable int tenantId) {
        try (Session dataSession = ServletHelper.getDataSession()) {
            Query<Tenant> query = dataSession.createQuery("from Tenant where orgId = :tenantId", Tenant.class);
            query.setParameter("tenantId", tenantId);
            return query.uniqueResult();
        }
    }

}
