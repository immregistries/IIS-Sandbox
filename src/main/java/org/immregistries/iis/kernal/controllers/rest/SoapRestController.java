package org.immregistries.iis.kernal.controllers.rest;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.immregistries.iis.kernal.controllers.filters.TenantRequestLoggingFilter;
import org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.fhir.security.TenantUtil;
import org.immregistries.iis.kernal.logic.BaseIISSOAPServer;
import org.immregistries.iis.kernal.logic.messageHandling.V2IncomingMessageHandler;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.smm.cdc.CDCWSDLServer;
import org.immregistries.smm.cdc.Fault;
import org.immregistries.smm.cdc.SubmitSingleMessage;
import org.immregistries.smm.cdc.UnknownFault;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;

@RestController
@RequestMapping({"/rest/soap", "/rest/tenant/{tenantId}/soap"})
public class SoapRestController {

	@Autowired
	private TenantUtil tenantUtil;
	@Autowired
	private V2IncomingMessageHandler handler;

	@PostMapping
	protected void doPost(HttpServletRequest req, HttpServletResponse resp,
								 @RequestAttribute(value = TenantRequestLoggingFilter.TENANT_REQUEST_ATTRIBUTE, required = false) Tenant tenant)
		throws ServletException, IOException {

		String tenantName;
		if (tenant == null) {
			tenantName = null;
		} else {
			tenantName = tenant.getOrganizationName();
		}
		String path = req.getPathInfo();
		final String processorName = path == null ? "" : (path.startsWith("/") ? path.substring(1) : path);
		CDCWSDLServer server = new BaseIISSOAPServer(tenantName, tenantUtil) {
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
