package org.immregistries.iis.kernal.servlet.shlink;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.fhir.interceptors.PartitionTenantCreationInterceptor;
import org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.fhir.security.TenantUtil;
import org.immregistries.iis.kernal.logic.shlink.ShLinkUtilService;
import org.immregistries.iis.kernal.mapping.interfaces.PatientMapper;
import org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester;
import org.immregistries.iis.kernal.mapping.internalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.model.ShLinkManifestRequestBody;
import org.immregistries.iis.kernal.persisted.model.ShLinkManifest;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.servlet.TenantController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

import static org.immregistries.iis.kernal.servlet.PatientController.PATIENT_BASE_PATH;
import static org.immregistries.iis.kernal.servlet.shlink.PatientShLinkManifestController.PATIENT_MANIFEST_FULL_PATH;
import static org.immregistries.iis.kernal.servlet.util.PatientServletUtil.fetchPatientFromParameters;

@RestController
@RequestMapping({ PATIENT_MANIFEST_FULL_PATH })
public class PatientShLinkManifestController {

	public static final String MANIFEST_PATH_SUFFIX = "/manifest";
	public static final String PATIENT_MANIFEST_FULL_PATH = TenantController.TENANT_PATH + MANIFEST_PATH_SUFFIX;
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private ShLinkUtilService shLinkUtilService;
	@Autowired
	private RepositoryClientFactory repositoryClientFactory;
	@Autowired
	private AbstractFhirRequester fhirRequester;
	@Autowired
	private FhirContext fhirContext;
	@Autowired
	private PatientMapper patientMapper;
	@Autowired
	private PartitionTenantCreationInterceptor partitionTenantCreationInterceptor;
	@Autowired
	private TenantUtil tenantUtil;


	@PostMapping({ PATIENT_BASE_PATH, PATIENT_BASE_PATH + "/{id}" })
	protected ShLinkManifest postPatientShLinkManifest(HttpServletRequest req, HttpServletResponse resp,
			@PathVariable("id") String id,
			@PathVariable("tenantName") String tenantName,
			@RequestBody ShLinkManifestRequestBody body) throws IOException, ServletException {
		String passcode = body.getPasscode();
		resp.setContentType("application/json");
		Tenant tenant = null;
		if (StringUtils.isNotBlank(passcode)) {
			tenant = tenantUtil.authenticateTenantNoUsername(passcode, tenantName);
		}
		if (tenant == null) {
			throw new AuthenticationCredentialsNotFoundException("No tenant found or invalid passcode");
		}
		return getShLinkManifest(req, id, tenant);
	}

	@GetMapping({ PATIENT_BASE_PATH, PATIENT_BASE_PATH + "/{id}" })
	protected ShLinkManifest getPatientShLinkManifest(HttpServletRequest req, HttpServletResponse resp,
			@PathVariable("id") String id,
			@PathVariable("tenantName") String tenantName,
			@RequestParam(value = "recipient", required = false) String recipient,
			@RequestParam(value = "passcode", required = false) String passcode,
			@RequestParam(value = "embeddedLengthMax", required = false) String embeddedLengthMax)
			throws IOException, ServletException {
		resp.setContentType("application/json");
		Tenant tenant = CurrentTenantUtil.getTenantRedirectIfNone(req, resp);
		return getShLinkManifest(req, id, tenant);
	}

	private ShLinkManifest getShLinkManifest(HttpServletRequest req, String id, Tenant tenant) {
		IGenericClient fhirClient = repositoryClientFactory.newGenericClient(req);
		IBaseResource patientSelected = fetchPatientFromParameters(id, "", fhirClient, fhirRequester);
		return shLinkUtilService.generateExamplePatientManifest(tenant, patientSelected.getIdElement());
	}
}
