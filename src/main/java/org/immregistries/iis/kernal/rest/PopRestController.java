package org.immregistries.iis.kernal.rest;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.Session;
import org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.fhir.security.TenantUtil;
import org.immregistries.iis.kernal.logic.messageHandling.V2IncomingMessageHandler;
import org.immregistries.iis.kernal.mapping.internalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.persisted.util.HibernateConfig;
import org.immregistries.iis.kernal.servlet.PopController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;

@RestController
@RequestMapping("/rest/tenant/{tenantId}/pop")
public class PopRestController {

    @Autowired
    private FhirContext fhirContext;
    @Autowired
    private RepositoryClientFactory repositoryClientFactory;
    @Autowired
    private V2IncomingMessageHandler handler;

    @PostMapping
    public String postPop(
            // @PathVariable int tenantId,
            @RequestBody PopRequest popRequest,
            HttpServletRequest req) {
        try (Session dataSession = HibernateConfig.getDataSession()) {
            Tenant tenant = CurrentTenantUtil.getTenantFromName(req, dataSession);
            if (tenant == null) {
                throw new RuntimeException("Access is not authorized");
            }
            String message = popRequest.getMessage();
            String facilityName = popRequest.getFacilityName();

            if (message == null) {
                return "";
            }

            String[] messages = message.split(PopController.MSH_HEADER_REGEX);
            if (messages.length > 2) {
                req.setAttribute("groupPatientIds", new ArrayList<String>());
            }

            StringBuilder ackBuilder = new StringBuilder();
            for (String msh : messages) {
                if (!msh.isBlank()) {
                    String ack = handler.process(PopController.MSH_HEADER + msh, tenant, facilityName);
                    ackBuilder.append(ack);
                }
            }

            /**
             * Saving a group if multiple patients were sent
             */
            @SuppressWarnings("unchecked")
            ArrayList<String> groupPatientIds = (ArrayList<String>) req.getAttribute("groupPatientIds");
            if (groupPatientIds != null) {
                if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
                    org.hl7.fhir.r5.model.Group group = new org.hl7.fhir.r5.model.Group();
                    for (String id : groupPatientIds) {
                        group.addMember()
                                .setEntity(new org.hl7.fhir.r5.model.Reference().setReference("Patient/" + id));
                    }
                    group.setDescription("Generated from Hl2v2 VXU Query on  time " + new Date());
                    repositoryClientFactory.newGenericClient(req).create().resource(group).execute();
                } else {
                    org.hl7.fhir.r4.model.Group group = new org.hl7.fhir.r4.model.Group();
                    for (String id : groupPatientIds) {
                        group.addMember()
                                .setEntity(new org.hl7.fhir.r4.model.Reference().setReference("Patient/" + id));
                    }
                    repositoryClientFactory.newGenericClient(req).create().resource(group).execute();
                }
            }

            return ackBuilder.toString();
        }
    }

    public static class PopRequest {
        private String message;
        private String facilityName;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getFacilityName() {
            return facilityName;
        }

        public void setFacilityName(String facilityName) {
            this.facilityName = facilityName;
        }
    }
}
