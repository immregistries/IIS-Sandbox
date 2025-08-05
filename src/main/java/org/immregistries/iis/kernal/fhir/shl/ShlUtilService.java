package org.immregistries.iis.kernal.fhir.shl;

import ca.uhn.fhir.context.FhirContext;
import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.nimbusds.jose.util.Base64URL;
import jakarta.persistence.Query;
import jakarta.servlet.ServletException;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hl7.fhir.instance.model.api.IIdType;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.mapping.internalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.persisted.ShLinkManifest;
import org.immregistries.iis.kernal.model.persisted.Tenant;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

@Service
public class ShlUtilService {
	private static final Logger logger = LoggerFactory.getLogger(ShlUtilService.class);

	@Autowired
	private RepositoryClientFactory repositoryClientFactory;

	public static final String EMBEDDED = "embedded";
	public static final String CONTENT_TYPE = "contentType";
	public static final String FILES = "files";
	public static final String U = "U";
	public static final String URL = "url";
	public static final String KEY = "key";
	public static final String FLAG = "flag";
	public static final String EXP = "exp";
	public static final String LABEL = "label";
	public static final String V = "v";
	public static final String SHLINK_PREFIX = "shlink:/";
	public static final String LOCATION = "location";
	public static final String VERIFIABLE_CREDENTIAL = "verifiableCredential";
	public static final String APPLICATION_JOSE = "application/jose";

	@Autowired
	FhirContext fhirContext;

	public String fullExamplePatientQrCode(Tenant tenant, PatientMaster patientMaster, String baseUrl) {

		ShLinkManifest shLinkManifest = generateExamplePatientManifest(tenant, patientMaster);
		shLinkManifest = saveManifest(shLinkManifest);
		String manifestUrl = baseUrl + shLinkManifest.getId();

		ShLinkPayload shLinkPayload = new ShLinkPayload();
		shLinkPayload.setUrl(manifestUrl);
		shLinkPayload.setLabel("Generated for testing");
		shLinkPayload.setKey(null);
		shLinkPayload.setFlag("LP");
		shLinkPayload.setExp(10000000L);

		return qrCode(shLinkPayload);

	}

	public ShLinkManifest generateExamplePatientManifest(Tenant tenant, PatientMaster patientMaster) {
		String patientLocation = "/fhir/" + tenant.getOrganizationName() + "/Patient/" + patientMaster.getPatientId();
		return generateManifest(tenant, patientLocation);
	}

	public ShLinkManifest generateExamplePatientManifest(Tenant tenant, IIdType iIdType) {
		String patientLocation = "/fhir/" + tenant.getOrganizationName() + "/Patient/" + iIdType.getIdPart();
		return generateManifest(tenant, patientLocation);
	}


	public ShLinkManifest generateManifest(Tenant tenant, String fhirLocation) {
		ShLinkManifest shLinkManifest = generateManifest(tenant);
		ShLinkManifest.FileManifest fileManifest = generateFhirFileManifest();
		fileManifest.setLocation(fhirLocation);
		shLinkManifest.addFiles(fileManifest);
		return shLinkManifest;
	}

	public ShLinkManifest generateManifest(Tenant tenant) {
		ShLinkManifest shLinkManifest = new ShLinkManifest();
		shLinkManifest.setTenant(tenant);
		shLinkManifest.setStatus("finalized");
		ShLinkManifest.FileManifest fileManifest = generateFhirFileManifest();
		shLinkManifest.addFiles(fileManifest);
		fileManifest.setLocation("/fhir/" + tenant.getOrganizationName() + "/Patient?identifier=test");
		return shLinkManifest;
	}

	private ShLinkManifest.@NotNull FileManifest generateFhirFileManifest() {
		ShLinkManifest.FileManifest fileManifest = new ShLinkManifest.FileManifest();
		String fhirVersion = fhirContext.getVersion().getVersion().getFhirVersionString();
		fileManifest.setContentType("application/fhir+json;fhirVersion=" + fhirVersion);
		return fileManifest;
	}

	public ShLinkManifest saveManifest(ShLinkManifest shLinkManifest) {
		if (StringUtils.isBlank(shLinkManifest.getId())) {
			shLinkManifest.setId(UUID.randomUUID().toString());
		}
		try (Session dataSession = ServletHelper.getDataSession()) {
			Transaction transaction = dataSession.beginTransaction();
			dataSession.persist(shLinkManifest);
			transaction.commit();
		}
		return shLinkManifest;
	}

	public ShLinkManifest readShLinkManifest(String manifestId) {
		ShLinkManifest shLinkManifest;
		try (Session dataSession = ServletHelper.getDataSession()) {
			Query query = dataSession.createQuery("from ShLinkManifest where id = :id", ShLinkManifest.class);
			query.setParameter("id", manifestId);
			shLinkManifest = (ShLinkManifest) query.getSingleResult();
		}
		return shLinkManifest;
	}

	public String qrCode(ShLinkPayload shLinkPayload) {
		Gson gson = new Gson();
		String payload = gson.toJson(shLinkPayload);
//		String minified = payload.trim();
		Base64URL base64URL = Base64URL.encode(payload);
		return SHLINK_PREFIX + base64URL;
	}

	public void printQrCode(OutputStream outputStream, String data) throws ServletException {
		int width = 200; // Desired QR code width
		int height = 200; // Desired QR code height
		try {
			QRCodeWriter qrCodeWriter = new QRCodeWriter();
			BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height);
//			response.setContentType("image/png"); // Set content type for PNG image
			MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
		} catch (WriterException | IOException e) {
			throw new ServletException("Error generating QR code", e);
		}
	}


}
