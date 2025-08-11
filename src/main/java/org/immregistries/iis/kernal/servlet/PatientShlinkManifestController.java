package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.fhir.interceptors.PartitionCreationInterceptor;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.fhir.shl.ShlUtilService;
import org.immregistries.iis.kernal.mapping.interfaces.PatientMapper;
import org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester;
import org.immregistries.iis.kernal.mapping.internalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.model.ShlinkManifestRequestBody;
import org.immregistries.iis.kernal.model.persisted.ShLinkManifest;
import org.immregistries.iis.kernal.model.persisted.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

import static org.immregistries.iis.kernal.servlet.PatientController.PATIENT_BASE_PATH;
import static org.immregistries.iis.kernal.servlet.PatientServletUtil.fetchPatientFromParameters;
import static org.immregistries.iis.kernal.servlet.PatientShlinkManifestController.PATIENT_MANIFEST_FULL_PATH;

@RestController
@RequestMapping({PATIENT_MANIFEST_FULL_PATH})
public class PatientShlinkManifestController {

	public static final String MANIFEST_PATH_SUFFIX = "/manifest";
	public static final String PATIENT_MANIFEST_FULL_PATH = TenantController.TENANT_PATH + MANIFEST_PATH_SUFFIX;


	@Autowired
	private ShlUtilService shlUtilService;
	@Autowired
	private RepositoryClientFactory repositoryClientFactory;
	@Autowired
	private AbstractFhirRequester fhirRequester;
	@Autowired
	private FhirContext fhirContext;
	@Autowired
	private PatientMapper patientMapper;
	@Autowired
	private PartitionCreationInterceptor partitionCreationInterceptor;

	@PostMapping({PATIENT_BASE_PATH, PATIENT_BASE_PATH + "/{id}"})
	protected ShLinkManifest postPatientShLinkManifest(HttpServletRequest req, HttpServletResponse resp,
																	  @PathVariable("id") String id,
																		@PathVariable("tenantName") String tenantName,
																		@RequestBody ShlinkManifestRequestBody body) throws IOException, ServletException {
		String passcode = body.getPasscode();
		resp.setContentType("application/json");
		try (Session dataSession = ServletHelper.getDataSession()) {
			Tenant tenant = null;
			if (StringUtils.isNotBlank(passcode)) {
				tenant = ServletHelper.authenticateTenantNoUsername(passcode, tenantName, dataSession, partitionCreationInterceptor);
			}
			if (tenant == null) {
//				if (ServletHelper.getUserAccess() != null) {
//					resp.sendRedirect("/iis/tenant");
//				}
				throw new AuthenticationCredentialsNotFoundException("No tenant found or invalid passcode");
			}
			return getShLinkManifest(req, id, tenant);
		}
	}

	@GetMapping({PATIENT_BASE_PATH, PATIENT_BASE_PATH + "/{id}"})
	protected ShLinkManifest getPatientShLinkManifest(HttpServletRequest req, HttpServletResponse resp,
																	  @PathVariable("id") String id,
																	  @PathVariable("tenantName") String tenantName,
																	  @RequestParam(value = "recipient", required = false) String recipient,
																	  @RequestParam(value = "passcode", required = false) String passcode,
																	  @RequestParam(value = "embeddedLengthMax", required = false) String embeddedLengthMax) throws IOException, ServletException {
		resp.setContentType("application/json");
		try (Session dataSession = ServletHelper.getDataSession()) {
			Tenant tenant;
				tenant = ServletHelper.getTenant(req, dataSession);
			if (tenant == null) {
				if (ServletHelper.getUserAccess() != null) {
					resp.sendRedirect("/iis/tenant");
				}
				throw new AuthenticationCredentialsNotFoundException("");
			}
			return getShLinkManifest(req, id, tenant);
		}
	}

	private ShLinkManifest getShLinkManifest(HttpServletRequest req, String id, Tenant tenant) {
		IGenericClient fhirClient = repositoryClientFactory.newGenericClient(req);
		IBaseResource patientSelected = fetchPatientFromParameters(id, "", fhirClient, fhirRequester);
		return shlUtilService.generateExamplePatientManifest(tenant, patientSelected.getIdElement());
	}
}
