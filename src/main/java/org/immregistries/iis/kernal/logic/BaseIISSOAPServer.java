package org.immregistries.iis.kernal.logic;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.immregistries.iis.kernal.fhir.interceptors.PartitionCreationInterceptor;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.model.persisted.Tenant;
import org.immregistries.smm.cdc.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.immregistries.iis.kernal.fhir.security.ServletHelper.SESSION_REQUEST_TENANT;

public abstract class BaseIISSOAPServer extends CDCWSDLServer {

	private PartitionCreationInterceptor partitionCreationInterceptor;
	private String tenantName;

	protected BaseIISSOAPServer(PartitionCreationInterceptor partitionCreationInterceptor, String tenantNameParameter) {
		this.partitionCreationInterceptor = partitionCreationInterceptor;
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
		try (Session dataSession = ServletHelper.getDataSession()) {
			if ("NPE".equals(userId) && "NPE".equals(password)) {
				throw new UnknownFault("Unknown Fault");
			}
			Tenant tenant;
			if (StringUtils.isNotBlank(tenantName)) {
				tenant = ServletHelper.authenticateTenant(userId, password, tenantName, dataSession, partitionCreationInterceptor);
			} else {
				tenant = ServletHelper.authenticateTenant(userId, password, facilityId, dataSession, partitionCreationInterceptor);
			}
			if (tenant == null) {
				throw new SecurityFault("Username/password combination is unrecognized");
			} else {
				HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
				request.setAttribute(SESSION_REQUEST_TENANT, tenant);
			}
		}
	}
}
