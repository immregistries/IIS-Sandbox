package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.partition.IPartitionLookupSvc;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import org.hibernate.Session;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;
import org.immregistries.iis.kernal.fhir.CrossTenantDiffProvider;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.model.Tenant;
import org.immregistries.iis.kernal.model.UserAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public static final String TENANT_IDS = "tenantIds";

	@Autowired
	private IPartitionLookupSvc partitionLookupSvc;

	@Autowired
	private CrossTenantDiffProvider diffProvider;
	@Autowired
	private DaoRegistry daoRegistry;
	@Autowired
	private FhirContext fhirContext;
	@Autowired
	private RestfulServer restfulServer;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String[] tenantNames = req.getParameter(TENANT_IDS).split(",");

		logger.info("Testing Tenant comparison");
		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		HomeServlet.doHeader(out, "Tenant Comparison", ServletHelper.getTenant(req));
		try (Session dataSession = ServletHelper.getDataSession()) {
			UserAccess userAccess = ServletHelper.getUserAccess();
			if (userAccess == null) {
				throw new AuthenticationCredentialsNotFoundException("");
			}

			List<Tenant> tenantList = Arrays.stream(tenantNames).distinct().map(tenantName -> ServletHelper.authenticateTenant(userAccess, tenantName, dataSession)).collect(Collectors.toList());

			Stream<SystemRequestDetails> systemRequestDetailsStream = tenantList.stream().map(tenant -> {
				SystemRequestDetails systemRequestDetails = new SystemRequestDetails();
				systemRequestDetails.setTenantId(tenant.getOrganizationName());
				return systemRequestDetails;
			});

			IFhirResourceDao patientDao = daoRegistry.getResourceDao("Patient");


			List<IBundleProvider> bundleProviderStream = systemRequestDetailsStream.map(
				requestDetails -> patientDao.search(new SearchParameterMap(), requestDetails)).collect(Collectors.toList());

			int previousSize = -1;
			/**
			 * Counting found patients
			 */
			for (IBundleProvider iBundleProvider : bundleProviderStream) {
				int bundleSize = iBundleProvider.size();
				if (previousSize >= 0) {
					if (bundleSize > previousSize) {
						logger.info("Missing patients in first tenant ,{} found instead of {}", previousSize, bundleSize);
					} else if (bundleSize < previousSize) {
						logger.info("Missing patients in second tenant ,{} found instead of {}", previousSize, bundleSize);
					}
//					return ;
				}
				previousSize = bundleSize;
			}

			/*
			 * Diff on each patient
			 */
			SystemRequestDetails diffRequestDetail = new SystemRequestDetails();
//			diffRequestDetail.setTenantId("");
			logger.info("Testing patients with $diff");

			diffRequestDetail.setRequestPartitionId(RequestPartitionId.allPartitions());
			for (int i = 0; i < previousSize; i++) {
				IBaseResource iBaseResource1 = bundleProviderStream.get(0).getAllResources().get(i);
				IBaseResource iBaseResource2 = bundleProviderStream.get(1).getAllResources().get(i);

				IBaseParameters diff = diffProvider.diff(iBaseResource1.getIdElement(), iBaseResource2.getIdElement(), new BooleanType(false), diffRequestDetail);
//				logger.info("Diff Patient {}, {}", i, fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(diff));
				out.println("<pre>" + fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(diff) + "</pre>");
			}
//			for(IBundleProvider iBundleProvider: bundleProviderStream) {
//				iBundleProvider.getAllResources()
//			}
		} finally {
			out.flush();
			out.close();
		}
	}


}
