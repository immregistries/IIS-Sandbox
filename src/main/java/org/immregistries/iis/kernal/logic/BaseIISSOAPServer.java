package org.immregistries.iis.kernal.logic;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

import org.immregistries.iis.kernal.fhir.security.TenantUtil;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.smm.cdc.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil.SESSION_REQUEST_TENANT;

public abstract class BaseIISSOAPServer extends CDCWSDLServer {

	private String tenantName;

	protected BaseIISSOAPServer(String tenantNameParameter) {
		this.tenantName = tenantNameParameter;
	}

	@Override
	public String getEchoBackMessage(String message) {
		return "End-point is ready. Echoing: " + message;
	}

	@Override
	public void authorize(SubmitSingleMessage ssm) throws Fault {
		String userId = ssm.getUsername();
		String password = ssm.getPassword();
		String facilityId = ssm.getFacilityID();
		if ("NPE".equals(userId) && "NPE".equals(password)) {
			throw new UnknownFault("Unknown Fault");
		}
		Tenant tenant;
		if (StringUtils.isNotBlank(tenantName)) {
			tenant = TenantUtil.authenticateTenant(userId, password, tenantName);
		} else {
			tenant = TenantUtil.authenticateTenant(userId, password, facilityId);
		}
		if (tenant == null) {
			throw new SecurityFault("Username/password combination is unrecognized");
		} else {
			HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
					.currentRequestAttributes()).getRequest();
			request.setAttribute(SESSION_REQUEST_TENANT, tenant);
		}
	}
}
