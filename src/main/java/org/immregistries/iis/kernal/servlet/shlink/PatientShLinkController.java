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
import org.immregistries.iis.kernal.fhir.shl.ShLinkPayload;
import org.immregistries.iis.kernal.logic.shlink.ShLinkUtilService;
import org.immregistries.iis.kernal.mapping.interfaces.PatientMapper;
import org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester;
import org.immregistries.iis.kernal.mapping.internalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.rest.PatientRestController;
import org.immregistries.iis.kernal.rest.shlink.PatientShLinkRestController;
import org.immregistries.iis.kernal.servlet.TenantController;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.OutputStream;

import static org.immregistries.iis.kernal.servlet.PatientController.PATIENT_BASE_PATH;
import static org.immregistries.iis.kernal.servlet.shlink.PatientShLinkManifestController.MANIFEST_PATH_SUFFIX;
import static org.immregistries.iis.kernal.servlet.util.PatientServletUtil.fetchPatientFromParameter;

@RestController
@RequestMapping({ PATIENT_BASE_PATH, TenantController.TENANT_PATH + PATIENT_BASE_PATH })
public class PatientShLinkController {

	public static final String SHLINK_QR_CODE_PATH_SUFFIX = "/qr";

	@Autowired
	private ShLinkUtilService shLinkUtilService;
	@Autowired
	private RepositoryClientFactory repositoryClientFactory;
	@Autowired
	private PatientRestController patientRestController;
	@Autowired
	private PatientShLinkRestController patientShLinkRestController;
	@Autowired
	private AbstractFhirRequester fhirRequester;

	@GetMapping({ SHLINK_QR_CODE_PATH_SUFFIX })
	protected void doGetShLinkQrCode(HttpServletRequest req, HttpServletResponse resp)
			throws IOException, ServletException {
		OutputStream outputStream = resp.getOutputStream();
		// PrintWriter out = new PrintWriter(outputStream);
		resp.setContentType("image/png"); // Set content type for PNG image

		Tenant tenant = CurrentTenantUtil.getTenantRedirectIfNone(req, resp);
		IGenericClient fhirClient = repositoryClientFactory.newGenericClient(req);
		// patientShLinkRestController.doGetPatientShLinkQrCode(req, resp);
		IBaseResource patientSelected = fetchPatientFromParameter(req, fhirClient, fhirRequester);
		if (patientSelected != null) {
			String qrCode = getQrCode(req, patientSelected, tenant);
			shLinkUtilService.printQrCodeAsImage(outputStream, qrCode);
		}
		outputStream.flush();
		outputStream.close();
	}

	private String getQrCode(HttpServletRequest req, IBaseResource patientSelected, Tenant tenant) {
		String manifestUrl = getManifestUrl(req, patientSelected, tenant);
		ShLinkPayload shLinkPayload = getPatientShLinkPayload(manifestUrl);

		return shLinkUtilService.qrCode(shLinkPayload);
	}

	public static @NotNull ShLinkPayload getPatientShLinkPayload(String manifestUrl) {
		ShLinkPayload shLinkPayload = new ShLinkPayload();
		shLinkPayload.setUrl(manifestUrl);
		shLinkPayload.setLabel("Generated for testing");
		shLinkPayload.setKey(null);
		shLinkPayload.setFlag("LP");
		shLinkPayload.setExp(10000000L);
		return shLinkPayload;
	}

	public static @NotNull String getManifestUrl(HttpServletRequest req, IBaseResource patientSelected, Tenant tenant) {
		String baseUrl = StringUtils.substringBefore(req.getRequestURL().toString(), req.getServletPath());
		return getManifestUrl(baseUrl, patientSelected, tenant);
	}

	public static @NotNull String getManifestUrl(String baseUrl, IBaseResource patientSelected, Tenant tenant) {
		return baseUrl + TenantController.TENANT_BASE_PATH + "/" + tenant.getOrganizationName() + MANIFEST_PATH_SUFFIX
				+ "/patient/" + patientSelected.getIdElement().getIdPart();
	}

}
