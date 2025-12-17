package org.immregistries.iis.kernal.logic.shlink;

import ca.uhn.fhir.context.FhirContext;
import com.google.gson.Gson;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.nimbusds.jose.util.Base64URL;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IIdType;
import org.immregistries.iis.kernal.controllers.rest.shlink.ShLinkContentRestController;
import org.immregistries.iis.kernal.fhir.Application;
import org.immregistries.iis.kernal.fhir.shl.ShLinkPayload;
import org.immregistries.iis.kernal.mapping.internalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.ShLinkFilePayload;
import org.immregistries.iis.kernal.persisted.model.*;
import org.immregistries.iis.kernal.persisted.repository.IisShlinkContentRepository;
import org.immregistries.iis.kernal.persisted.repository.ShlinkManifestRepository;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.immregistries.iis.kernal.controllers.rest.shlink.ShLinkManifestRestController.SHLINKS_CONTROLLER_REST_BASE_URL;
import static org.immregistries.iis.kernal.logic.shlink.ShCardUtilService.VERIFIABLE_CREDENTIAL_TYPE;

@Service
public class ShLinkUtilService {

	private static final Logger logger = LoggerFactory.getLogger(ShLinkUtilService.class);

	@Autowired
	private RepositoryClientFactory repositoryClientFactory;
	@Autowired
	private IisShlinkContentRepository iisShlinkContentRepository;
	@Autowired
	private ShCardUtilService shCardUtilService;

	@Autowired
	private ShlinkManifestRepository shlinkManifestRepository;

	public static final String APPLICATION_SMART_HEALTH_CARD_CONTENT_TYPE = "application/smart-health-card";
	public static final String APPLICATION_FHIR_JSON_CONTENT_TYPE = "application/fhir+json";

	public static final String SHLINK_PREFIX = "shlink:/";

	@Autowired
	FhirContext fhirContext;
	@Autowired
	CompressionService compressionService;

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
		// ShLinkManifest.FileManifest fileManifest = generateFhirFileManifest();
		// shLinkManifest.addFiles(fileManifest);
		// fileManifest.setLocation("/fhir/" + tenant.getOrganizationName() +
		// "/Patient?identifier=test");
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
		return shlinkManifestRepository.save(shLinkManifest);
	}

	public ShLinkManifest readShLinkManifest(String manifestId) {
		return shlinkManifestRepository.findById(manifestId).orElse(null);
	}

	public String qrCode(ShLinkPayload shLinkPayload) {
		Gson gson = new Gson();
		String payload = gson.toJson(shLinkPayload);
		// String minified = payload.trim();
		Base64URL base64URL = Base64URL.encode(payload);
		return SHLINK_PREFIX + base64URL;
	}

	public void printQrCodeAsImage(OutputStream outputStream, String data) throws ServletException {
		int width = 300; // Desired QR code width
		int height = 300; // Desired QR code height
		try {
			BitMatrix bitMatrix = compressionService.qrCodeBitMatrix(data, width, height);
			MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
		} catch (IOException e) {
			throw new ServletException("Error generating QR code", e);
		}
	}

	public String generateShLinkUrlForShCards(List<IBaseBundle> bundleList, ShLinkPayload shLinkPayload,
			HttpServletRequest req, IisKey iisSigningKey, SecretKeySpec encryptionKey, UserAccess userAccess,
			Tenant tenant) throws IOException {
		String url;
		UriComponentsBuilder builder = ServletUriComponentsBuilder.fromRequest(req);

		ShLinkFilePayload shLinkFilePayload = new ShLinkFilePayload();
		shLinkFilePayload.setType(List.of(VERIFIABLE_CREDENTIAL_TYPE, APPLICATION_SMART_HEALTH_CARD_CONTENT_TYPE));

		List<String> verifiableCredentials = new ArrayList<>(bundleList.size());
		for (IBaseBundle bundle : bundleList) {
			String shCardCompact = shCardUtilService.qrCompact(bundle, req, iisSigningKey, userAccess, tenant);
			verifiableCredentials.add(shCardCompact);
		}
		shLinkFilePayload.setVerifiableCredential(verifiableCredentials);

		String encryptedContent = Jwts.builder()
			.content(compressionService
						.minifyJson(shLinkFilePayload))
				.header().add("cty", APPLICATION_SMART_HEALTH_CARD_CONTENT_TYPE)
				.and()
				.encryptWith(encryptionKey, Jwts.ENC.A256GCM).compact(); // Alg specified in Smart health card IG

		/*
		 * Direct file
		 */
		if (StringUtils.contains(shLinkPayload.getFlag().orElse(""), "U")) {
			IisShLinkContent iisShLinkContent = new IisShLinkContent();
			iisShLinkContent.setUserAccess(userAccess);
			iisShLinkContent.setExp(shLinkPayload.getExp().orElse(10000000L));
			iisShLinkContent.setContent(encryptedContent);
			iisShlinkContentRepository.save(iisShLinkContent);
			builder.replacePath(
					Application.IIS_PATH_BASE  + ShLinkContentRestController.SHLINK_CONTENT_PATH + "/{contentId}");
			url = builder
					.build(Map.of("contentId", iisShLinkContent.getId()))
					.toURL().toString();
		} else {
			/*
			 * Manifest
			 */
			ShLinkManifest shLinkManifest = new ShLinkManifest();
			shLinkManifest.setTenant(tenant);
			shLinkManifest.setStatus("finalized");

			ShLinkManifest.FileManifest fileManifest = new ShLinkManifest.FileManifest();
			fileManifest.setContentType(APPLICATION_SMART_HEALTH_CARD_CONTENT_TYPE);
			fileManifest.setEmbedded(encryptedContent);
			shLinkManifest.addFiles(fileManifest);

			saveManifest(shLinkManifest);


			builder.replacePath(Application.IIS_PATH_BASE + SHLINKS_CONTROLLER_REST_BASE_URL + "/{manifestId}");
			url = builder
					.build(Map.of("manifestId", shLinkManifest.getId()))
					.toURL().toString();
		}
		return url;
	}

	public @NotNull SecretKeySpec generateSecretKey() throws NoSuchAlgorithmException {
		byte[] randomBytes = new byte[32];
		SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(randomBytes);
		return new SecretKeySpec(randomBytes, "AES");
	}

}
