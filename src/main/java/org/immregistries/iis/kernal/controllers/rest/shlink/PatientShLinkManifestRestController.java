package org.immregistries.iis.kernal.controllers.rest.shlink;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.controllers.rest.RestUrlUtil;
import org.immregistries.iis.kernal.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.security.TenantUtil;
import org.immregistries.iis.kernal.logic.shlink.ShLinkUtilService;
import org.immregistries.iis.kernal.mapping.interfaces.PatientMapper;
import org.immregistries.iis.kernal.mapping.internalClient.FhirSearchRequester;
import org.immregistries.iis.kernal.mapping.internalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.model.ShLinkManifestRequestBody;
import org.immregistries.iis.kernal.persisted.model.ShLinkManifest;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

import static org.immregistries.iis.kernal.controllers.servlet.TenantController.PARAM_TENANT_ID;
import static org.immregistries.iis.kernal.controllers.servlet.util.PatientServletUtil.fetchPatientFromParameters;

@RestController
@RequestMapping("rest/tenant/{tenantId}/manifest")
public class PatientShLinkManifestRestController {

	public static final String MANIFEST_PATH_SUFFIX = "/manifest";
	public static final String MANIFEST_FULL_PATH = RestUrlUtil.REST_TENANT_PATH + MANIFEST_PATH_SUFFIX;

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private ShLinkUtilService shLinkUtilService;
	@Autowired
	private RepositoryClientFactory repositoryClientFactory;
	@Autowired
	FhirSearchRequester fhirSearchRequester;
	@Autowired
	private FhirContext fhirContext;
	@Autowired
	private PatientMapper patientMapper;
	@Autowired
	private TenantUtil tenantUtil;

	@PostMapping({"/patient", "/patient/{id}"})
	protected ShLinkManifest postPatientShLinkManifest(HttpServletRequest req, HttpServletResponse resp,
																		@PathVariable(value = "id", required = false) String id,
																		@PathVariable(PARAM_TENANT_ID) String tenantId,
																		@RequestAttribute(CurrentTenantUtil.SESSION_REQUEST_TENANT) Tenant tenant,
																		@RequestBody ShLinkManifestRequestBody body) throws IOException, ServletException {
		String passcode = body.getPasscode();
		if (StringUtils.isNotBlank(passcode)) {
			tenant = tenantUtil.authenticateTenantNoUsername(tenantId, passcode);
		}
		if (tenant == null) {
			throw new AuthenticationCredentialsNotFoundException("No tenant found or invalid passcode");
		}
		return getShLinkManifest(req, id, tenant);
	}

	@GetMapping({"/patient", "/patient/{id}"})
	protected ShLinkManifest getPatientShLinkManifest(HttpServletRequest req, HttpServletResponse resp,
																	  @PathVariable(value = "id", required = false) String id,
																	  @RequestAttribute(CurrentTenantUtil.SESSION_REQUEST_TENANT) Tenant tenant,
																	  @RequestParam(value = "recipient", required = false) String recipient,
																	  @RequestParam(value = "passcode", required = false) String passcode,
																	  @RequestParam(value = "embeddedLengthMax", required = false) String embeddedLengthMax) {
		return getShLinkManifest(req, id, tenant);
	}

	private ShLinkManifest getShLinkManifest(HttpServletRequest req, String id, Tenant tenant) {
		IGenericClient fhirClient = repositoryClientFactory.newGenericClient(req);
		IBaseResource patientSelected = fetchPatientFromParameters(id, "", fhirClient, fhirSearchRequester);
		return shLinkUtilService.generateExamplePatientManifest(tenant, patientSelected.getIdElement());
	}
}
