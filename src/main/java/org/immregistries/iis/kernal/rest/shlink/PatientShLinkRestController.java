package org.immregistries.iis.kernal.rest.shlink;

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
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.OutputStream;

import static org.immregistries.iis.kernal.servlet.shlink.PatientShLinkManifestController.MANIFEST_PATH_SUFFIX;
import static org.immregistries.iis.kernal.servlet.util.PatientServletUtil.fetchPatientFromParameter;

@RestController
@RequestMapping({ "rest/patient", "rest/tenant/{tenantName}/patient" })
public class PatientShLinkRestController {

    public static final String SHLINK_QR_CODE_PATH_SUFFIX = "/qr";

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

    @GetMapping({ SHLINK_QR_CODE_PATH_SUFFIX })
    protected void doGetShLinkQrCode(HttpServletRequest req, HttpServletResponse resp,
            @RequestAttribute(CurrentTenantUtil.SESSION_REQUEST_TENANT) Tenant tenant)
            throws IOException, ServletException {
        OutputStream outputStream = resp.getOutputStream();
        resp.setContentType("image/png"); // Set content type for PNG image

        IGenericClient fhirClient = repositoryClientFactory.newGenericClient(req);
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
        // Adjusting Base URL for REST
        String baseUrl = StringUtils.substringBefore(req.getRequestURL().toString(), "/rest/");
        return getManifestUrl(baseUrl, patientSelected, tenant);
    }

    public static @NotNull String getManifestUrl(String baseUrl, IBaseResource patientSelected, Tenant tenant) {
        return baseUrl + "/rest/tenant/" + tenant.getOrganizationName() + MANIFEST_PATH_SUFFIX
                + "/patient/" + patientSelected.getIdElement().getIdPart();
    }

}
