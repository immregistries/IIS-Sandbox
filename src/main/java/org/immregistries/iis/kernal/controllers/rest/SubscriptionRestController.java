package org.immregistries.iis.kernal.controllers.rest;

import ca.uhn.fhir.jpa.provider.SubscriptionTriggeringProvider;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Subscription;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.immregistries.iis.kernal.logic.SubscriptionService;
import org.immregistries.iis.kernal.mapping.internalClient.IisFhirClientFactory;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.*;

import static org.immregistries.iis.kernal.security.CurrentTenantUtil.SESSION_REQUEST_TENANT;

import java.util.ArrayList;
import java.util.List;

@Conditional(OnR5Condition.class)
@RestController
@RequestMapping("/rest/tenant/{tenantId}/subscription")
public class SubscriptionRestController {

    @Autowired
    IisFhirClientFactory iisFhirClientFactory;
    @Autowired
    SubscriptionService subscriptionService;
    @Autowired
    SubscriptionTriggeringProvider subscriptionTriggeringProvider;

    @GetMapping()
    public IBaseBundle getAllSubscriptions(
            HttpServletRequest req) {
        IGenericClient fhirClient = iisFhirClientFactory.newGenericClient(req);

        // Implementation for GET request
        org.hl7.fhir.r5.model.Bundle subcriptionBundle = fhirClient.search()
                .forResource(org.hl7.fhir.r5.model.Subscription.class)
                .returnBundle(org.hl7.fhir.r5.model.Bundle.class).execute();
        return subcriptionBundle;
    }

    @PostMapping("/trigger")
    public String triggerSubscription(
            @RequestAttribute(name = SESSION_REQUEST_TENANT) Tenant tenant,
            @RequestBody TriggerRequest triggerRequest,
            HttpServletRequest req) {

        // Authenticate tenant
        // Note: Original code uses CurrentTenantUtil.getTenantRedirectIfNone(req, resp)
        // which handles redirect.
        // For REST, we just throw exception if not authorized or return 401.
        // We use TenantUtil.getTenantByIdAuthenticated for path variable auth.
        // We also need to set the tenant in CurrentTenantUtil for downstream logic.

        // However, the original code uses `req.getParameter(PARAM_SUBSCRIPTION_ID)` and
        // other params.
        // We will accept a DTO.
        IGenericClient localClient = iisFhirClientFactory.newGenericClient(req);
        String subscriptionId = triggerRequest.getSubscriptionId();

        Bundle searchBundle = localClient.search().forResource(Subscription.class)
                .where(Subscription.IDENTIFIER.exactly().identifier(subscriptionId)).returnBundle(Bundle.class)
                .execute();

        if (searchBundle.hasEntry()) {
            List<String> messages = triggerRequest.getMessages();
            List<String> httpVerbs = triggerRequest.getHttpVerbs();

            if (messages != null && httpVerbs != null && messages.size() == httpVerbs.size()) {
                List<Pair<String, Bundle.HTTPVerb>> parsedResources = new ArrayList<>();
                for (int i = 0; i < messages.size(); i++) {
                    String message = messages.get(i);
                    if (message != null && !message.isBlank()) {
                        parsedResources.add(new MutablePair<>(message, Bundle.HTTPVerb.valueOf(httpVerbs.get(i))));
                    }
                }

                Subscription subscription = (Subscription) searchBundle.getEntryFirstRep().getResource();
                return subscriptionService.triggerWithResourceFullManual(subscription, parsedResources);
            } else {
                return "Incorrect parameters length";
            }
        } else {
            return "NO SUBSCRIPTION FOUND FOR THIS IDENTIFIER";
        }
    }

    public static class TriggerRequest {
        private String subscriptionId;
        private List<String> messages;
        private List<String> httpVerbs;

        public String getSubscriptionId() {
            return subscriptionId;
        }

        public void setSubscriptionId(String subscriptionId) {
            this.subscriptionId = subscriptionId;
        }

        public List<String> getMessages() {
            return messages;
        }

        public void setMessages(List<String> messages) {
            this.messages = messages;
        }

        public List<String> getHttpVerbs() {
            return httpVerbs;
        }

        public void setHttpVerbs(List<String> httpVerbs) {
            this.httpVerbs = httpVerbs;
        }
    }
}
