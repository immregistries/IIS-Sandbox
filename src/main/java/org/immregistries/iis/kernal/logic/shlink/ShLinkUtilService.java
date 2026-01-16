package org.immregistries.iis.kernal.logic.shlink;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.nimbusds.jose.util.Base64URL;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.immregistries.iis.kernal.controllers.rest.shlink.ShLinkContentRestController;
import org.immregistries.iis.kernal.Application;
import org.immregistries.iis.kernal.fhir.shl.ShLinkPayload;
import org.immregistries.iis.kernal.mapping.IisFhirClientFactory;
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
	private IisFhirClientFactory iisFhirClientFactory;
	@Autowired
	private IisShlinkContentRepository iisShlinkContentRepository;
	@Autowired
	private ShCardUtilService shCardUtilService;



	public static final String APPLICATION_SMART_HEALTH_CARD_CONTENT_TYPE = "application/smart-health-card";
	public static final String APPLICATION_FHIR_JSON_CONTENT_TYPE = "application/fhir+json";

	public static final String SHLINK_PREFIX = "shlink:/";

	@Autowired
	private CompressionService compressionService;
	@Autowired
	private ShlinkManifestService shlinkManifestService;

	ObjectMapper objectMapper = new ObjectMapper();


	public String qrCode(ShLinkPayload shLinkPayload) {
		String payload = "";
		try {
			payload = objectMapper.writeValueAsString(shLinkPayload);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		Base64URL base64URL = Base64URL.encode(payload);
		return SHLINK_PREFIX + base64URL;
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
		if (StringUtils.containsAny(shLinkPayload.getFlag(), "U")) {
			IisShLinkContent iisShLinkContent = new IisShLinkContent();
			iisShLinkContent.setUserAccess(userAccess);
			if (shLinkPayload.getExp() != null) {
				iisShLinkContent.setExp(shLinkPayload.getExp());
			} else {
				iisShLinkContent.setExp(10000000L);
			}
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

			shlinkManifestService.saveManifest(shLinkManifest);


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

//	public String fullExamplePatientQrCode(Tenant tenant, PatientMaster patientMaster, String baseUrl) {
//
//		ShLinkManifest shLinkManifest = shLinkManifestGenerator.generateExamplePatientManifest(tenant, patientMaster);
//		shLinkManifest = shlinkManifestService.saveManifest(shLinkManifest);
//		String manifestUrl = baseUrl + shLinkManifest.getId();
//
//		ShLinkPayload shLinkPayload = new ShLinkPayload();
//		shLinkPayload.setUrl(manifestUrl);
//		shLinkPayload.setLabel("Generated for testing");
//		shLinkPayload.setKey(null);
//		shLinkPayload.setFlag("LP");
//		shLinkPayload.setExp(10000000L);
//
//		return qrCode(shLinkPayload);
//
//	}

}
