package org.immregistries.iis.kernal.controllers.rest.shlink;

import ca.uhn.fhir.jpa.ips.generator.IIpsGeneratorSvc;
import com.authlete.cose.COSEException;
import com.google.zxing.WriterException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.IdType;
import org.immregistries.iis.kernal.IisRequestAttribute;
import org.immregistries.iis.kernal.controllers.IisPathVariable;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.persisted.entities.IisKey;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.immregistries.iis.kernal.security.RequestTenantUtil;
import org.immregistries.iis.kernal.services.KeyStoreService;
import org.immregistries.iis.kernal.services.QrCodeEncoder;
import org.immregitries.clvr.CLVRPdfService;
import org.immregitries.clvr.CLVRService;
import org.immregitries.clvr.mapping.FhirConversionUtil;
import org.immregitries.clvr.model.CLVRPayload;
import org.immregitries.clvr.model.CLVRToken;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;

@RestController
@RequestMapping(IisRestPath.REST_PATIENT_PATH + IisRestPath.BasePath.CLVR_PATH)
public class CLVRRestController {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private KeyStoreService keyStoreService;
    @Autowired
    private CLVRService clvrService;
    @Autowired
    private IIpsGeneratorSvc ipsGeneratorSvc;
    @Autowired
    private FhirConversionUtil fhirConversionUtil;
    @Autowired
    private CLVRPdfService clvrPdfService;
    @Autowired
    private QrCodeEncoder qrCodeEncoder;
	@Autowired
	private RequestTenantUtil requestTenantUtil;

    @GetMapping(value = "/qr", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getPatientClvrQrCode(
		 @AuthenticationPrincipal UserAccess userAccess,
            @PathVariable(IisPathVariable.Key.PATIENT_ID) String patientId,
				@RequestAttribute(IisRequestAttribute.TENANT_REQUEST_ATTRIBUTE) Tenant tenant)
            throws COSEException, SignatureException, NoSuchAlgorithmException, InvalidKeyException,
            NoSuchProviderException, IOException {
        IisKey iisSigningKey = keyStoreService.getIisSigningKeyOrCreate("", userAccess, tenant);
		 CLVRToken clvrToken = getIpsClvrToken(patientId, tenant);
		 String qrCode = clvrService.encodeCLVRtoQrCode(clvrToken, iisSigningKey.keyPair(), iisSigningKey.getKeyId());
        return qrCode;
    }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getPatientClvrPdf(
		 @AuthenticationPrincipal UserAccess userAccess,
            @PathVariable(IisPathVariable.Key.PATIENT_ID) String patientId,
				@RequestAttribute(IisRequestAttribute.TENANT_REQUEST_ATTRIBUTE) Tenant tenant)
            throws COSEException, IOException, SignatureException, NoSuchAlgorithmException, InvalidKeyException,
            NoSuchProviderException, WriterException, URISyntaxException {

        IisKey iisSigningKey = keyStoreService.getIisSigningKeyOrCreate("", userAccess, tenant);
		 CLVRToken clvrToken = getIpsClvrToken(patientId, tenant);
		 String qrCode = clvrService.encodeCLVRtoQrCode(clvrToken, iisSigningKey.keyPair(), iisSigningKey.getKeyId());
        PDDocument pdDocument = clvrPdfService.createPdf(clvrToken, qrCode.getBytes(), "IIS SANDBOX");
        return pdfResponseEntity(pdDocument, "clvrDocument");
    }

	private @NotNull CLVRToken getIpsClvrToken(String patientId, Tenant tenant) {
        IBaseBundle ipsToBeEncoded = ipsGeneratorSvc
			  .generateIps(requestTenantUtil.requestDetailsWithPartitionName(tenant), new IdType(patientId), "");
        @SuppressWarnings("unchecked")
        CLVRPayload clvrPayload = fhirConversionUtil.toCLVRPayloadFromBundle(ipsToBeEncoded);

        CLVRToken clvrToken = new CLVRToken(clvrPayload, "IIS");
        return clvrToken;
    }

    protected ResponseEntity<byte[]> pdfResponseEntity(
            PDDocument pdDocument,
            String name) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = getByteArrayOutputStream(pdDocument);

        HttpHeaders headers = new HttpHeaders();
        // headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", name + ".pdf");
        return new ResponseEntity<>(byteArrayOutputStream.toByteArray(), headers, HttpStatus.OK);
    }

    private static @NotNull ByteArrayOutputStream getByteArrayOutputStream(PDDocument pdDocument) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        pdDocument.save(byteArrayOutputStream);
        pdDocument.close();
        return byteArrayOutputStream;
    }


}
