package org.immregistries.iis.kernal.rest;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.immregistries.iis.kernal.fhir.interceptors.PartitionTenantCreationInterceptor;
import org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.logic.BaseIISSOAPServer;
import org.immregistries.iis.kernal.logic.messageHandling.V2IncomingMessageHandler;
import org.immregistries.iis.kernal.model.persisted.Tenant;
import org.immregistries.smm.cdc.CDCWSDLServer;
import org.immregistries.smm.cdc.Fault;
import org.immregistries.smm.cdc.SubmitSingleMessage;
import org.immregistries.smm.cdc.UnknownFault;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;

@RestController
@RequestMapping({ "/rest/soap", "/rest/tenant/{tenantId}/soap" })
public class SoapRestController {

    @Autowired
    private V2IncomingMessageHandler handler;

    @Autowired
    private PartitionTenantCreationInterceptor partitionTenantCreationInterceptor;

    @PostMapping
    protected void doPost(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable(required = false) String tenantId)
            throws ServletException, IOException {
        Tenant tenant = CurrentTenantUtil.getTenant();

        String tenantName;
        if (tenant == null) {
            tenantName = null;
        } else {
            tenantName = tenant.getOrganizationName();
        }
        String path = req.getPathInfo();
        final String processorName = path == null ? "" : (path.startsWith("/") ? path.substring(1) : path);
        CDCWSDLServer server = new BaseIISSOAPServer(partitionTenantCreationInterceptor, tenantName) {
            @Override
            public void process(SubmitSingleMessage ssm, PrintWriter out) throws Fault {
                String message = ssm.getHl7Message();

                String ack = "";
                try {
                    /*
                     * Tenant is accessed through RequestContext, and was previously set through the
                     * authorize method of WSDL server
                     */
                    Tenant tenant = CurrentTenantUtil.getTenant();
                    if (tenant == null) {
                        throw new SecurityException("Username/password combination is unrecognized");
                    } else {
                        ack = handler.process(message, tenant, null);
                    }
                } catch (Exception e) {
                    throw new UnknownFault("Unable to process request: " + e.getMessage(), e);
                }
                out.print(ack);
            }
        };
        server.setProcessorName(processorName);
        server.process(req, resp);
    }
}
