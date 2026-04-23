package org.immregistries.iis.kernal.controllers.rest.shlink;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.IisRequestAttribute;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.controllers.servlet.util.PatientServletUtil;
import org.immregistries.iis.kernal.logic.shlink.ShLinkManifestGenerator;
import org.immregistries.iis.kernal.logic.shlink.generation.ShLinkGenerator;
import org.immregistries.iis.kernal.mapping.IisFhirClientFactory;
import org.immregistries.iis.kernal.mapping.mappers.resources.PatientMapper;
import org.immregistries.iis.kernal.mapping.requesters.FhirSearchRequester;
import org.immregistries.iis.kernal.model.shlink.ShLinkManifestRequestBody;
import org.immregistries.iis.kernal.persisted.entities.ShLinkManifest;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.security.RequestTenantUtil;
import org.immregistries.iis.kernal.security.TenantAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

import static org.immregistries.iis.kernal.controllers.servlet.TenantController.PARAM_TENANT_NAME;

@RestController
@RequestMapping(IisRestPath.MANIFEST_FULL_PATH)
public class PatientShLinkManifestRestController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private ShLinkGenerator shLinkGenerator;
	@Autowired
	private ShLinkManifestGenerator shLinkManifestGenerator;
	@Autowired
	private IisFhirClientFactory iisFhirClientFactory;
	@Autowired
	private FhirSearchRequester fhirSearchRequester;
	@Autowired
	private FhirContext fhirContext;
	@Autowired
	private PatientMapper patientMapper;
	@Autowired
	private TenantAuthService tenantAuthService;
	@Autowired
	private PatientServletUtil patientServletUtil;
	@Autowired
	private RequestTenantUtil requestTenantUtil;

	@PostMapping({ "/patient", "/patient/{id}" })
	protected ShLinkManifest postPatientShLinkManifest(HttpServletRequest req, HttpServletResponse resp,
			@PathVariable(value = "id", required = false) String id,
																		@PathVariable(PARAM_TENANT_NAME) String tenantName,
																		@RequestAttribute(value = IisRequestAttribute.TENANT_REQUEST_ATTRIBUTE, required = false) Tenant tenant,
			@RequestBody ShLinkManifestRequestBody body) throws IOException, ServletException {
		String passcode = body.getPasscode();
		if (StringUtils.isNotBlank(passcode)) {
			tenant = tenantAuthService.authenticateTenantNoUsername(tenantName, passcode);
			requestTenantUtil.setTenantForRequest(tenant, req);
		}
		if (tenant == null) {
			throw new AuthenticationCredentialsNotFoundException("No tenant found or invalid passcode");
		}
		return getShLinkManifest(req, id, tenant);
	}

	@PostMapping({"/patient/ips", "/patient/{id}/ips"})
	protected ShLinkManifest postPatientIpsShLinkManifest(HttpServletRequest req, HttpServletResponse resp,
																			@PathVariable(value = "id", required = false) String id,
																			@PathVariable(PARAM_TENANT_NAME) String tenantName,
																			@RequestAttribute(value = IisRequestAttribute.TENANT_REQUEST_ATTRIBUTE, required = false) Tenant tenant,
																			@RequestBody ShLinkManifestRequestBody body) throws IOException, ServletException {
		String passcode = body.getPasscode();
		if (StringUtils.isNotBlank(passcode)) {
			tenant = tenantAuthService.authenticateTenantNoUsername(tenantName, passcode);
			requestTenantUtil.setTenantForRequest(tenant, req);
		}
		if (tenant == null) {
			throw new AuthenticationCredentialsNotFoundException("No tenant found or invalid passcode");
		}
		return getShLinkManifestIps(req, id, tenant);
	}

	@GetMapping({ "/patient", "/patient/{id}" })
	protected ShLinkManifest getPatientShLinkManifest(HttpServletRequest req, HttpServletResponse resp,
			@PathVariable(value = "id", required = false) String id,
																	  @RequestAttribute(IisRequestAttribute.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
			@RequestParam(value = "recipient", required = false) String recipient,
			@RequestParam(value = "passcode", required = false) String passcode,
			@RequestParam(value = "embeddedLengthMax", required = false) String embeddedLengthMax) {
		return getShLinkManifest(req, id, tenant);
	}

	@GetMapping({"/patient/ips", "/patient/{id}/ips"})
	protected ShLinkManifest getPatientIpsShLinkManifest(HttpServletRequest req, HttpServletResponse resp,
																		  @PathVariable(value = "id", required = false) String id,
																		  @RequestAttribute(IisRequestAttribute.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
																		  @RequestParam(value = "recipient", required = false) String recipient,
																		  @RequestParam(value = "passcode", required = false) String passcode,
																		  @RequestParam(value = "embeddedLengthMax", required = false) String embeddedLengthMax) {
		return getShLinkManifestIps(req, id, tenant);
	}

	private ShLinkManifest getShLinkManifest(HttpServletRequest req, String id, Tenant tenant) {
		IGenericClient fhirClient = iisFhirClientFactory.getOrCreateGenericClient(req);
		IAnyResource patientSelected = patientServletUtil.fetchPatientFromParameters(id, "", fhirClient, fhirSearchRequester);
		return shLinkManifestGenerator.generateExamplePatientManifest(tenant, patientSelected.getIdElement());
	}

	private ShLinkManifest getShLinkManifestIps(HttpServletRequest req, String id, Tenant tenant) {
		IGenericClient fhirClient = iisFhirClientFactory.getOrCreateGenericClient(req);
		IAnyResource patientSelected = patientServletUtil.fetchPatientFromParameters(id, "", fhirClient, fhirSearchRequester);
		return shLinkManifestGenerator.generateExamplePatientIpsManifest(tenant, patientSelected.getIdElement());
	}
}
