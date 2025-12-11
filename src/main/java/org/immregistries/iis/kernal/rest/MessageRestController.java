package org.immregistries.iis.kernal.rest;

import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.Session;
import org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.fhir.security.TenantUtil;
import org.immregistries.iis.kernal.persisted.model.MessageReceived;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.persisted.util.HibernateConfig;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rest/tenant/{tenantId}/message")
public class MessageRestController {

    @GetMapping
    public List<MessageReceived> getMessages(
            @PathVariable int tenantId,
            @RequestParam(required = false) String search,
            HttpServletRequest req) {

        try (Session dataSession = HibernateConfig.getDataSession()) {
            Tenant tenant = TenantUtil.getTenantByIdAuthenticated(tenantId, dataSession);
            if (tenant == null) {
                throw new RuntimeException("Access is not authorized");
            }
            CurrentTenantUtil.getTenant(tenant.getOrganizationName(), req, dataSession);

            Query query = dataSession.createQuery(
                    "from MessageReceived where tenant = :tenant order by reportedDate desc");
            query.setParameter("tenant", tenant);
            List<MessageReceived> messageReceivedList = query.getResultList();

            if (search != null && !search.isEmpty()) {
                // Filter in memory as per original controller logic
                // Or better, filter in query? Original logic filters in loop.
                // To keep consistency with original logic which limits to 10 *after* filtering?
                // Original logic:
                // Iterate list
                // If search matches, increment count.
                // If count > 10 break.
                // We should return the filtered list.

                // Note: The original controller logic is a bit inefficient (fetches all, then
                // filters).
                // We will replicate the logic but return the list.

                return messageReceivedList.stream()
                        .filter(m -> m.getMessageRequest().contains(search) || m.getMessageResponse().contains(search))
                        .limit(10)
                        .collect(java.util.stream.Collectors.toList());
            } else {
                return messageReceivedList.stream().limit(10).collect(java.util.stream.Collectors.toList());
            }
        }
    }
}
