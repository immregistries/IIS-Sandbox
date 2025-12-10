package org.immregistries.iis.kernal.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.immregistries.iis.kernal.HibernateConfig;
import org.immregistries.iis.kernal.fhir.interceptors.PartitionTenantCreationInterceptor;
import org.immregistries.iis.kernal.fhir.security.TenantUtil;
import org.immregistries.iis.kernal.fhir.security.UserAccessUtil;
import org.immregistries.iis.kernal.model.persisted.Tenant;
import org.immregistries.iis.kernal.model.persisted.TenantRepository;
import org.immregistries.iis.kernal.model.persisted.UserAccess;
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
		 return tenantRepository.findByUserAccessId(UserAccessUtil.getUserAccess().getUserAccessId());
//        try (Session dataSession = HibernateConfig.getDataSession()) {
//            Query<Tenant> query = dataSession.createQuery("from Tenant where userAccess = :userAccess", Tenant.class);
//            query.setParameter("userAccess", UserAccessUtil.getUserAccess());
//            return query.list();
//        }
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
