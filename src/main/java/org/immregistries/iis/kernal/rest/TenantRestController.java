package org.immregistries.iis.kernal.rest;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.immregistries.iis.kernal.fhir.interceptors.PartitionTenantCreationInterceptor;
import org.immregistries.iis.kernal.HibernateConfig;
import org.immregistries.iis.kernal.fhir.security.TenantUtil;
import org.immregistries.iis.kernal.fhir.security.UserAccessUtil;
import org.immregistries.iis.kernal.model.persisted.Tenant;
import org.immregistries.iis.kernal.model.persisted.UserAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/rest/tenant")
public class TenantRestController {

    @Autowired
    private PartitionTenantCreationInterceptor partitionTenantCreationInterceptor;

    @GetMapping("/{tenantId}")
    public Tenant getTenant(@PathVariable int tenantId) {
        try (Session dataSession = HibernateConfig.getDataSession()) {
            Query<Tenant> query = dataSession
                    .createQuery("from Tenant where orgId = :tenantId and userAccess = :userAccess", Tenant.class);
            query.setParameter("tenantId", tenantId);
            query.setParameter("userAccess", UserAccessUtil.getUserAccess());
            return query.uniqueResult();
        }
    }

    @GetMapping
    public List<Tenant> getTenants(HttpServletRequest req) {
        try (Session dataSession = HibernateConfig.getDataSession()) {
            Query<Tenant> query = dataSession.createQuery("from Tenant where userAccess = :userAccess", Tenant.class);
            query.setParameter("userAccess", UserAccessUtil.getUserAccess());
            return query.list();
        }
    }

    @PostMapping
    public Tenant createTenant(@RequestBody Tenant tenant) {
        UserAccess currentUser = UserAccessUtil.getUserAccess();
        if (tenant.getUserAccess() != null && !tenant.getUserAccess().equals(currentUser)) {
            throw new IllegalArgumentException("Tenant UserAccess must be null or match the current user");
        }
        try (Session dataSession = HibernateConfig.getDataSession()) {
            TenantUtil.authenticateTenant(currentUser, tenant.getOrganizationName(), dataSession,
                    partitionTenantCreationInterceptor);
            tenant.setUserAccess(currentUser);
            Transaction transaction = dataSession.beginTransaction();
            dataSession.persist(tenant);
            transaction.commit();
            return tenant;
        }
    }

}
