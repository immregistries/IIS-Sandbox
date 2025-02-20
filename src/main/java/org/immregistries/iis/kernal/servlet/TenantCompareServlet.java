package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.jpa.partition.IPartitionLookupSvc;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.hibernate.Session;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.model.Tenant;
import org.immregistries.iis.kernal.model.UserAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TenantCompareServlet extends HttpServlet {

	public static final String TENANT_IDS = "tenantIds";

	@Autowired
	private IPartitionLookupSvc partitionLookupSvc;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String[] tenantNames = req.getParameterValues(TENANT_IDS);

		try (Session dataSession = ServletHelper.getDataSession()) {
			UserAccess userAccess = ServletHelper.getUserAccess();
			if (userAccess == null) {
				throw new AuthenticationCredentialsNotFoundException("");
			}

			List<Tenant> tenantList = Arrays.stream(tenantNames).map(tenantName -> ServletHelper.authenticateTenant(userAccess, tenantName, dataSession)).collect(Collectors.toList());

			Stream<ServletRequestDetails> servletRequestDetailsStream = tenantList.stream().map(tenant -> {
				ServletRequestDetails servletRequestDetails = new ServletRequestDetails();
				servletRequestDetails.setTenantId(tenant.getOrganizationName());
				return servletRequestDetails;
			});
			resp.setContentType("text/html");
			PrintWriter out = new PrintWriter(resp.getOutputStream());
		}

	}


}
