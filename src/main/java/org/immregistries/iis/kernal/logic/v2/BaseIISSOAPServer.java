package org.immregistries.iis.kernal.logic.v2;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.security.TenantAuthService;
import org.immregistries.smm.cdc.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.immregistries.iis.kernal.security.CurrentTenantUtil.SESSION_REQUEST_TENANT;

public abstract class BaseIISSOAPServer extends CDCWSDLServer {

	private String tenantName;
	private TenantAuthService tenantAuthService;

	protected BaseIISSOAPServer(String tenantNameParameter, TenantAuthService tenantAuthService) {
		this.tenantName = tenantNameParameter;
		this.tenantAuthService = tenantAuthService;
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
			tenant = tenantAuthService.authenticateTenant(userId, password, tenantName);
		} else {
			tenant = tenantAuthService.authenticateTenant(userId, password, facilityId);
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
