package org.immregistries.iis.kernal.servlet;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.fhir.shl.ShLinkPayload;
import org.immregistries.iis.kernal.fhir.shl.ShlUtilService;
import org.immregistries.iis.kernal.mapping.interfaces.PatientMapper;
import org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester;
import org.immregistries.iis.kernal.mapping.internalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.model.persisted.ShLinkManifest;
import org.immregistries.iis.kernal.model.persisted.Tenant;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.OutputStream;

import static org.immregistries.iis.kernal.servlet.PatientController.PATIENT_BASE_PATH;
import static org.immregistries.iis.kernal.servlet.PatientServletUtil.fetchPatientFromParameter;

@RestController
@RequestMapping({PATIENT_BASE_PATH, TenantController.TENANT_PATH + PATIENT_BASE_PATH})
public class PatientShLinkController {

	public static final String SHLINK_QR_CODE_PATH_SUFFIX = "/qr";
	public static final String MANIFEST_PATH_SUFFIX = "/manifest";

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

	@GetMapping({MANIFEST_PATH_SUFFIX + "/{id}"})
	protected ShLinkManifest getPatientShLinkManifest(HttpServletRequest req, HttpServletResponse resp, @PathVariable("id") String id) throws IOException, ServletException {
		try (Session dataSession = ServletHelper.getDataSession()) {
			Tenant tenant = ServletHelper.getTenant(req, dataSession);
			if (tenant == null) {
				if (ServletHelper.getUserAccess() != null) {
					resp.sendRedirect("/iis/tenant");
				}
				throw new AuthenticationCredentialsNotFoundException("");
			}
			IGenericClient fhirClient = repositoryClientFactory.newGenericClient(req);
			IBaseResource patientSelected = fetchPatientFromParameter(req, fhirClient, fhirRequester);
			return shlUtilService.generateExamplePatientManifest(tenant, patientSelected.getIdElement());
		}
	}

	@GetMapping({SHLINK_QR_CODE_PATH_SUFFIX})
	protected void doGetShLinkQrCode(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		OutputStream outputStream = resp.getOutputStream();
//		PrintWriter out = new PrintWriter(outputStream);
		resp.setContentType("image/png"); // Set content type for PNG image

		try (Session dataSession = ServletHelper.getDataSession()) {
			Tenant tenant = ServletHelper.getTenant(req, dataSession);
			if (tenant == null) {
				if (ServletHelper.getUserAccess() != null) {
					resp.sendRedirect("/iis/tenant");
				}
				throw new AuthenticationCredentialsNotFoundException("");
			}
			IGenericClient fhirClient = repositoryClientFactory.newGenericClient(req);
			IBaseResource patientSelected = fetchPatientFromParameter(req, fhirClient, fhirRequester);
			if (patientSelected == null) {
			} else {
//				PatientMaster patientMaster = patientMapper.localObject(patientSelected);
//
//				ShLinkManifest shLinkManifest = shlUtilService.generateExamplePatientManifest(tenant, patientMaster);
//				shLinkManifest = shlUtilService.saveManifest(shLinkManifest);

				String qrcode = getQrcode(req, patientSelected);
				shlUtilService.printQrCodeAsImage(outputStream, qrcode);
			}
		}
		outputStream.flush();
		outputStream.close();
	}

	private String getQrcode(HttpServletRequest req, IBaseResource patientSelected) {
		String manifestUrl = getManifestUrl(req, patientSelected);
		ShLinkPayload shLinkPayload = getPatientShLinkPayload(manifestUrl);

		return shlUtilService.qrCode(shLinkPayload);
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

	private static @NotNull String getManifestUrl(HttpServletRequest req, IBaseResource patientSelected) {
		return StringUtils.substringBeforeLast(req.getRequestURL().toString(), SHLINK_QR_CODE_PATH_SUFFIX) + MANIFEST_PATH_SUFFIX + "/" + patientSelected.getIdElement().getIdPart();
	}


}
