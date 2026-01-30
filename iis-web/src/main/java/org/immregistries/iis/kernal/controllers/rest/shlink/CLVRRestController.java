package org.immregistries.iis.kernal.controllers.rest.shlink;

import ca.uhn.fhir.jpa.ips.generator.IIpsGeneratorSvc;
import com.authlete.cose.COSEException;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import jakarta.servlet.ServletException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.IdType;
import org.immregistries.iis.kernal.controllers.IisPathVariable;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.persisted.entities.IisKey;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.immregistries.iis.kernal.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.security.TenantAuthService;
import org.immregistries.iis.kernal.security.UserAccessUtil;
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
import org.springframework.web.bind.annotation.*;

import java.awt.image.BufferedImage;
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

    @GetMapping(value = "/qr", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getPatientClvrQrCode(
            @PathVariable(IisPathVariable.Key.PATIENT_ID) String patientId,
            @RequestAttribute(CurrentTenantUtil.SESSION_REQUEST_TENANT) Tenant tenant)
            throws COSEException, SignatureException, NoSuchAlgorithmException, InvalidKeyException,
            NoSuchProviderException, IOException {

        UserAccess userAccess = UserAccessUtil.get().getUserAccess();
        IisKey iisSigningKey = keyStoreService.getIisSigningKeyOrCreate("", userAccess, tenant);
        CLVRToken clvrToken = getIpsClvrToken(patientId);
        String qrCode = clvrService.encodeCLVRtoQrCode(clvrToken, iisSigningKey.keyPair());
        logger.info("qrCode {}", qrCode);
        return qrCode;
    }

    @GetMapping(value = "/qr/png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getPatientClvrPng(
            @PathVariable(IisPathVariable.Key.PATIENT_ID) String patientId,
            @RequestAttribute(CurrentTenantUtil.SESSION_REQUEST_TENANT) Tenant tenant)
            throws COSEException, IOException, SignatureException, NoSuchAlgorithmException, InvalidKeyException,
            NoSuchProviderException, ServletException {
        UserAccess userAccess = UserAccessUtil.get().getUserAccess();
        IisKey iisSigningKey = keyStoreService.getIisSigningKeyOrCreate("", userAccess, tenant);
        CLVRToken clvrToken = getIpsClvrToken(patientId);
        String qrCode = clvrService.encodeCLVRtoQrCode(clvrToken, iisSigningKey.keyPair());

        ByteArrayOutputStream byteArrayOutputStreamPNG = qrCodeEncoder.toQrCodeStreamPNG(qrCode);
        return ResponseEntity.ok(byteArrayOutputStreamPNG.toByteArray());
        // HttpHeaders headers = new HttpHeaders();
        // headers.setContentDispositionFormData("attachment", "qr.png");
        // return new ResponseEntity<>(byteArrayOutputStreamPNG, headers,
        // HttpStatus.OK);
    }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getPatientClvrPdf(
            @PathVariable(IisPathVariable.Key.PATIENT_ID) String patientId,
            @RequestAttribute(CurrentTenantUtil.SESSION_REQUEST_TENANT) Tenant tenant)
            throws COSEException, IOException, SignatureException, NoSuchAlgorithmException, InvalidKeyException,
            NoSuchProviderException, WriterException, URISyntaxException {

        UserAccess userAccess = UserAccessUtil.get().getUserAccess();
        IisKey iisSigningKey = keyStoreService.getIisSigningKeyOrCreate("", userAccess, tenant);
        CLVRToken clvrToken = getIpsClvrToken(patientId);
        String qrCode = clvrService.encodeCLVRtoQrCode(clvrToken, iisSigningKey.keyPair());
        PDDocument pdDocument = clvrPdfService.createPdf(clvrToken, qrCode.getBytes(), "IIS SANDBOX");
        return pdfResponseEntity(pdDocument, "clvrDocument");
    }

    private @NotNull CLVRToken getIpsClvrToken(String patientId) {
        IBaseBundle ipsToBeEncoded = ipsGeneratorSvc
                .generateIps(TenantAuthService.get().requestDetailsWithPartitionName(), new IdType(patientId), "");
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

    public BufferedImage bufferedImage(String data) throws ServletException {
        int width = 300; // Desired QR code width
        int height = 300; // Desired QR code height
        BitMatrix bitMatrix = qrCodeEncoder.qrCodeBitMatrix(data, width, height);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

}
