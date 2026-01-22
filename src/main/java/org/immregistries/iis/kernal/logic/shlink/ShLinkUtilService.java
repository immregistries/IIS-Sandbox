package org.immregistries.iis.kernal.logic.shlink;

import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.IdType;
import org.immregistries.iis.kernal.Application;
import org.immregistries.iis.kernal.controllers.WellKnownKeyController;
import org.immregistries.iis.kernal.controllers.rest.shlink.ShLinkContentRestController;
import org.immregistries.iis.kernal.fhir.ips.IpsGeneratorSvcIIS;
import org.immregistries.iis.kernal.fhir.shl.ShLinkPayload;
import org.immregistries.iis.kernal.logic.KeyStoreService;
import org.immregistries.iis.kernal.logic.SecretKeyUtilService;
import org.immregistries.iis.kernal.model.ShLinkFilePayload;
import org.immregistries.iis.kernal.persisted.entities.*;
import org.immregistries.iis.kernal.persisted.repository.IisShlinkContentRepository;
import org.immregistries.iis.kernal.security.TenantAuthService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.immregistries.iis.kernal.controllers.rest.shlink.ShLinkManifestRestController.SHLINKS_CONTROLLER_REST_BASE_URL;
import static org.immregistries.iis.kernal.logic.shlink.ShCardUtilService.VERIFIABLE_CREDENTIAL_TYPE;

@Service
public class ShLinkUtilService {

	private static final Logger logger = LoggerFactory.getLogger(ShLinkUtilService.class);
	public static final String CONTENT_ID = "contentId";
	public static final String APPLICATION_SMART_HEALTH_CARD_CONTENT_TYPE = "application/smart-health-card";
	public static final String APPLICATION_FHIR_JSON_CONTENT_TYPE = "application/fhir+json";
	public static final String MANIFEST_ID = "manifestId";

	@Autowired
	private IisShlinkContentRepository iisShlinkContentRepository;
	@Autowired
	private ShCardUtilService shCardUtilService;
	@Autowired
	private CompressionService compressionService;
	@Autowired
	private ShLinkManifestService shlinkManifestService;
	@Autowired
	private IpsGeneratorSvcIIS ipsGeneratorSvcIIS;
	@Autowired
	private KeyStoreService keyStoreService;
	@Autowired
	private SecretKeyUtilService secretKeyUtilService;

	public String generateShLink(HttpServletRequest req, String keyId, String secretKey, String patientId, String flag, String exp, Tenant tenant, UserAccess userAccess) throws NoSuchAlgorithmException, IOException {
		/*
		 * Choosing or generating the keys based on the parameters
		 */
		SecretKeySpec encryptionKeySpec = secretKeyUtilService.getSecretEncryptionKeyOrCreate(secretKey);
		IisKey iisSigningKey = keyStoreService.getIisSigningKeyOrCreate(keyId, userAccess, tenant);
		/*
		 * Payload skeleton
		 */
		ShLinkPayload shLinkPayload = new ShLinkPayload();
		shLinkPayload.setLabel("Generated for ShLink testing with IPS of Synthetic Patient");
		shLinkPayload.setFlag(flag);
		shLinkPayload.setKey(new String(Base64.getUrlEncoder().encode(encryptionKeySpec.getEncoded())));
		try {
			shLinkPayload.setExp(Long.parseLong(exp));
		} catch (NumberFormatException e) {
			shLinkPayload.setExp(10000000L); // default
		}
		/*
		 * Getting the bundle for the payload content
		 */
		IBaseBundle ipsToBeEncoded = ipsGeneratorSvcIIS.generateIps(TenantAuthService.get().requestDetailsWithPartitionName(),
			new IdType(patientId), "");
		/*
		 * Convert the bundle to a shcard file
		 */
		URL url = generateShLinkForShCards(List.of(ipsToBeEncoded), shLinkPayload, req,
			iisSigningKey, encryptionKeySpec, userAccess, tenant);
		shLinkPayload.setUrl(url.toString());
		return ShLinkPayloadUtil.toBase64QrCode(shLinkPayload);
	}



	public URL generateShLinkForShCards(List<IBaseBundle> bundleList, ShLinkPayload shLinkPayload,
													HttpServletRequest req, IisKey iisSigningKey, SecretKeySpec encryptionKey, UserAccess userAccess,
													Tenant tenant) throws IOException {
		URL shLinkUrl;
		UriComponentsBuilder uriBuilder = ServletUriComponentsBuilder.fromRequest(req);

		ShLinkFilePayload shLinkFilePayload = new ShLinkFilePayload();
		shLinkFilePayload.setType(List.of(VERIFIABLE_CREDENTIAL_TYPE, APPLICATION_SMART_HEALTH_CARD_CONTENT_TYPE));

		String shcardIssuerUrl = WellKnownKeyController.getKeyIssuerUrl(req, tenant);
		List<String> verifiableCredentials = new ArrayList<>(bundleList.size());
		for (IBaseBundle bundle : bundleList) {
			String shCardCompact = shCardUtilService.qrCompact(bundle, shcardIssuerUrl, iisSigningKey);
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
			shLinkUrl = directFileShCardUrl(shLinkPayload, userAccess, encryptedContent, uriBuilder);
		} else {
			shLinkUrl = manifestShCardUrl(tenant, encryptedContent, uriBuilder);
		}
		return shLinkUrl;
	}

	private @NotNull URL manifestShCardUrl(Tenant tenant, String encryptedContent, UriComponentsBuilder uriBuilder) throws MalformedURLException {
		URL shLinkUrl;
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


		uriBuilder.replacePath(Application.IIS_PATH_BASE + SHLINKS_CONTROLLER_REST_BASE_URL + "/{" + MANIFEST_ID + "}");
		shLinkUrl = uriBuilder
				.build(Map.of(MANIFEST_ID, shLinkManifest.getId()))
				.toURL();
		return shLinkUrl;
	}

	private @NotNull URL directFileShCardUrl(ShLinkPayload shLinkPayload, UserAccess userAccess, String encryptedContent, UriComponentsBuilder uriBuilder) throws MalformedURLException {
		URL shLinkUrl;
		IisShLinkContent iisShLinkContent = new IisShLinkContent();
		iisShLinkContent.setUserAccess(userAccess);
		if (shLinkPayload.getExp() != null) {
			iisShLinkContent.setExp(shLinkPayload.getExp());
		} else {
			iisShLinkContent.setExp(10000000L);
		}
		iisShLinkContent.setContent(encryptedContent);
		iisShlinkContentRepository.save(iisShLinkContent);
		uriBuilder.replacePath(
				Application.IIS_PATH_BASE  + ShLinkContentRestController.SHLINK_CONTENT_PATH + "/{" + CONTENT_ID + "}");
		shLinkUrl = uriBuilder
				.build(Map.of(CONTENT_ID, iisShLinkContent.getId()))
				.toURL();
		return shLinkUrl;
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
