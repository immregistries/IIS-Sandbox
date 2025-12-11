package org.immregistries.iis.kernal.logic;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.immregistries.iis.kernal.fhir.interceptors.PartitionTenantCreationInterceptor;

import org.immregistries.iis.kernal.fhir.security.TenantUtil;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.persisted.util.HibernateConfig;
import org.immregistries.smm.cdc.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil.SESSION_REQUEST_TENANT;

public abstract class BaseIISSOAPServer extends CDCWSDLServer {

	private PartitionTenantCreationInterceptor partitionTenantCreationInterceptor;
	private String tenantName;

	protected BaseIISSOAPServer(PartitionTenantCreationInterceptor partitionTenantCreationInterceptor,
			String tenantNameParameter) {
		this.partitionTenantCreationInterceptor = partitionTenantCreationInterceptor;
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
		try (Session dataSession = HibernateConfig.getDataSession()) {
			if ("NPE".equals(userId) && "NPE".equals(password)) {
				throw new UnknownFault("Unknown Fault");
			}
			Tenant tenant;
			if (StringUtils.isNotBlank(tenantName)) {
				tenant = TenantUtil.authenticateTenant(userId, password, tenantName, dataSession,
						partitionTenantCreationInterceptor);
			} else {
				tenant = TenantUtil.authenticateTenant(userId, password, facilityId, dataSession,
						partitionTenantCreationInterceptor);
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
}
