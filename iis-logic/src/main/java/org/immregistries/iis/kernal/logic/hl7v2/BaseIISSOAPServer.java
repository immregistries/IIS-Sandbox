package org.immregistries.iis.kernal.logic.hl7v2;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.security.RequestTenantUtil;
import org.immregistries.iis.kernal.security.TenantAuthService;
import org.immregistries.smm.cdc.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public abstract class BaseIISSOAPServer extends CDCWSDLServer {

	private String tenantName;
	private TenantAuthService tenantAuthService;
	private RequestTenantUtil requestTenantUtil;

	protected BaseIISSOAPServer(String tenantNameParameter, TenantAuthService tenantAuthService, RequestTenantUtil requestTenantUtil) {
		this.tenantName = tenantNameParameter;
		this.tenantAuthService = tenantAuthService;
		this.requestTenantUtil = requestTenantUtil;
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
			requestTenantUtil.setTenantForRequest(request, tenant);
		}
	}
}
