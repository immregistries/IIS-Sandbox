package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.partition.IPartitionLookupSvc;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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

import static org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester.GOLDEN_RECORD;
import static org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester.GOLDEN_SYSTEM_TAG;


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

			List<SystemRequestDetails> systemRequestDetailsList = tenantList.stream().map(tenant -> {
				SystemRequestDetails systemRequestDetails = new SystemRequestDetails();
				systemRequestDetails.setTenantId(tenant.getOrganizationName());
				return systemRequestDetails;
			}).collect(Collectors.toList());

			IFhirResourceDao patientDao = daoRegistry.getResourceDao("Patient");
			checkResourceType(systemRequestDetailsList, patientDao, out);
			IFhirResourceDao immunizationDao = daoRegistry.getResourceDao("Immunization");
			checkResourceType(systemRequestDetailsList, immunizationDao, out);
			IFhirResourceDao observationDao = daoRegistry.getResourceDao("Observation");
			checkResourceType(systemRequestDetailsList, observationDao, out);
			IFhirResourceDao organizationDao = daoRegistry.getResourceDao("Organization");
			checkResourceType(systemRequestDetailsList, organizationDao, out);

			/*
			 * Check references, match resources
			 */
		} finally {
			out.flush();
			out.close();
		}
	}

	private void checkResourceType(List<SystemRequestDetails> systemRequestDetailsList, IFhirResourceDao resourceDao, PrintWriter out) {
		List<IBundleProvider> bundleProviderStream = systemRequestDetailsList.stream().map(
			requestDetails -> resourceDao.search(new SearchParameterMap("_tag", new TokenParam(GOLDEN_SYSTEM_TAG, GOLDEN_RECORD).setModifier(TokenParamModifier.NOT)), requestDetails)).collect(Collectors.toList());
		String label = resourceDao.getResourceType().getName().toLowerCase();

		int previousSize = -1;
		/**
		 * Counting found patients
		 */
		for (IBundleProvider iBundleProvider : bundleProviderStream) {
			int bundleSize = iBundleProvider.size();
			if (previousSize >= 0) {
				if (bundleSize > previousSize) {
					logger.info("Missing {}s in first tenant ,{} found instead of {}", label, previousSize, bundleSize);
				} else if (bundleSize < previousSize) {
					logger.info("Missing {}s in second tenant ,{} found instead of {}", label, previousSize, bundleSize);
				}
//					return ;
			}
			previousSize = bundleSize;
		}

		/*
		 * Diff on each patient
		 */
		SystemRequestDetails diffRequestDetail = new SystemRequestDetails();
		diffRequestDetail.setRequestPartitionId(RequestPartitionId.allPartitions());
		logger.info("Testing {}s with $diff", label);

		JsonObject jsonObject = new JsonObject();
		JsonArray diffs = new JsonArray();
		for (int i = 0; i < previousSize; i++) {
			IBaseResource iBaseResource1 = bundleProviderStream.get(0).getAllResources().get(i);
			IBaseResource iBaseResource2 = bundleProviderStream.get(1).getAllResources().get(i);

			IBaseParameters diff = diffProvider.diff(iBaseResource1.getIdElement(), iBaseResource2.getIdElement(), new BooleanType(false), diffRequestDetail);
//			patientsDiff.add(fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(diff));
			out.println("<pre>" + fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(diff) + "</pre>");
//				out.println("<pre>" + fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(diff) + "</pre>");

//				logger.info("Diff Patient {}, {}", i, fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(diff));
		}
//		jsonObject.add("patientsDiff", patientsDiff);
		return;
	}


}
