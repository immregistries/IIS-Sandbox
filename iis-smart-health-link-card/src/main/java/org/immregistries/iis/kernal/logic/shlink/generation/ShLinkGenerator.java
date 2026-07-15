package org.immregistries.iis.kernal.logic.shlink.generation;

import ca.uhn.fhir.jpa.ips.generator.IIpsGeneratorSvc;
import io.jsonwebtoken.Jwts;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.IdType;
import org.immregistries.iis.kernal.logic.shlink.IShApiUrlService;
import org.immregistries.iis.kernal.logic.shlink.ShLinkManifestStoreService;
import org.immregistries.iis.kernal.model.shlink.ShLinkFilePayload;
import org.immregistries.iis.kernal.model.shlink.ShLinkPayload;
import org.immregistries.iis.kernal.persisted.entities.*;
import org.immregistries.iis.kernal.persisted.repository.IisShlinkContentRepository;
import org.immregistries.iis.kernal.persisted.repository.ShLinkGeneratedRepository;
import org.immregistries.iis.kernal.security.RequestTenantUtil;
import org.immregistries.iis.kernal.services.CompressionService;
import org.immregistries.iis.kernal.services.KeyStoreService;
import org.immregistries.iis.kernal.services.QrCodeEncoder;
import org.immregistries.iis.kernal.services.SecretKeyUtilService;
import org.immregistries.iis.kernal.services.api.IWellKnownKeyApiService;
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
import java.util.*;

import static org.immregistries.iis.kernal.logic.shlink.generation.ShCardGenerator.VERIFIABLE_CREDENTIAL_TYPE;

@Service
public class ShLinkGenerator {

	private static final Logger logger = LoggerFactory.getLogger(ShLinkGenerator.class);
	public static final String CONTENT_ID = "contentId";
	public static final String APPLICATION_SMART_HEALTH_CARD_CONTENT_TYPE = "application/smart-health-card";
	public static final String APPLICATION_FHIR_JSON_CONTENT_TYPE = "application/fhir+json";
	public static final String MANIFEST_ID = "manifestId";

	@Autowired
	private IisShlinkContentRepository iisShlinkContentRepository;
	@Autowired
	private ShCardGenerator shCardGenerator;
	@Autowired
	private CompressionService compressionService;
	@Autowired
	private ShLinkManifestStoreService shlinkManifestStoreService;
	@Autowired
	private IIpsGeneratorSvc iIpsGeneratorSvc;
	@Autowired
	private KeyStoreService keyStoreService;
	@Autowired
	private SecretKeyUtilService secretKeyUtilService;
	@Autowired
	private IWellKnownKeyApiService wellKnownKeyService;
	@Autowired
	private IShApiUrlService shApiUrlService;
	@Autowired
	private QrCodeEncoder qrCodeEncoder;
	@Autowired
	private RequestTenantUtil requestTenantUtil;
	@Autowired
	private ShLinkGeneratedRepository shLinkGeneratedRepository;

	public ShLinkGenerated generateShLink(String keyId, String secretKey, String patientId, String flag, String exp, Tenant tenant, UserAccess userAccess, ServletUriComponentsBuilder uriBuilder, String passcode, String label, String description) throws NoSuchAlgorithmException, IOException {
		/*
		 * Choosing or generating the keys based on the parameters
		 */
		SecretKeySpec encryptionKeySpec = secretKeyUtilService.getSecretEncryptionKeyOrCreate(secretKey);
		IisKey iisSigningKey = keyStoreService.getIisSigningKeyOrCreate(keyId, userAccess, tenant);
		/*
		 * Payload skeleton
		 */
		ShLinkPayload shLinkPayload = createEmptyShlinkPayload(flag, exp, encryptionKeySpec);
		/*
		 * Getting the bundle for the payload content
		 */
		IBaseBundle ipsToBeEncoded = iIpsGeneratorSvc.generateIps(requestTenantUtil.requestDetailsWithPartitionName(tenant),
			new IdType(patientId), "");
		/*
		 * Convert the bundle to a shcard file
		 */
		URL url = generateShLinkForShCards(List.of(ipsToBeEncoded), shLinkPayload,
			iisSigningKey, encryptionKeySpec, tenant, patientId, uriBuilder, passcode);
		shLinkPayload.setUrl(url.toString());

		String base64QrCode = qrCodeEncoder.toBase64QrCode(shLinkPayload);


		ShLinkGenerated shLinkGenerated = new ShLinkGenerated();
		shLinkGenerated.setId(UUID.randomUUID().toString());
		shLinkGenerated.setTenant(tenant);
		shLinkGenerated.setPatientId(patientId);
		shLinkGenerated.setExp(Long.valueOf(exp));
		shLinkGenerated.setFlag(flag);
		shLinkGenerated.setCreatedAt(new Date());
		shLinkGenerated.setUrl(shLinkPayload.getUrl());
		shLinkGenerated.setEncodedQR(base64QrCode);
		shLinkGenerated.setLabel(label);
		shLinkGenerated.setDescription(description);
		return shLinkGeneratedRepository.save(shLinkGenerated);
	}

	/**
	 * Generate empty payload skeleton
	 *
	 * @param flag
	 * @param exp
	 * @param encryptionKeySpec
	 * @return
	 */
	private static @NotNull ShLinkPayload createEmptyShlinkPayload(String flag, String exp, SecretKeySpec encryptionKeySpec) {
		ShLinkPayload shLinkPayload = new ShLinkPayload();
		shLinkPayload.setLabel("Generated for ShLink testing with IPS of Synthetic Patient");
		shLinkPayload.setFlag(flag);
		shLinkPayload.setKey(new String(Base64.getUrlEncoder().encode(encryptionKeySpec.getEncoded())));
		try {
			shLinkPayload.setExp(Long.parseLong(exp));
		} catch (NumberFormatException e) {
			shLinkPayload.setExp(10000000L); // default
		}
		return shLinkPayload;
	}


	public URL generateShLinkForShCards(List<IBaseBundle> bundleList, ShLinkPayload shLinkPayload,
	                                    IisKey iisSigningKey, SecretKeySpec encryptionKey,
	                                    Tenant tenant, String patientId, UriComponentsBuilder uriBuilder, String passcode) throws IOException {
		URL shLinkUrl;

		ShLinkFilePayload shLinkFilePayload = new ShLinkFilePayload();
		shLinkFilePayload.setType(List.of(VERIFIABLE_CREDENTIAL_TYPE, APPLICATION_SMART_HEALTH_CARD_CONTENT_TYPE));

		String shcardIssuerUrl = wellKnownKeyService.generateKeyIssuerUrl(tenant, uriBuilder);
		List<String> verifiableCredentials = new ArrayList<>(bundleList.size());
		for (IBaseBundle bundle : bundleList) {
			String shCardCompact = shCardGenerator.shCardCompact(bundle, shcardIssuerUrl, iisSigningKey);
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
			shLinkUrl = directFileShCardUrl(shLinkPayload, tenant, patientId, encryptedContent, uriBuilder);
		} else {
			boolean passcodeProtected = StringUtils.containsAny(shLinkPayload.getFlag(), "P");
			shLinkUrl = createManifestWithShCard(tenant, patientId, encryptedContent, uriBuilder, passcodeProtected, passcode);
		}
		return shLinkUrl;
	}

	private @NotNull URL createManifestWithShCard(Tenant tenant, String patientId, String encryptedContent, UriComponentsBuilder uriBuilder, boolean passcodeProtected, String passcode) throws MalformedURLException {
		URL shLinkUrl;
		/*
		 * Manifest
		 */
		ShLinkManifest shLinkManifest = new ShLinkManifest();
		shLinkManifest.setTenant(tenant);
		shLinkManifest.setPatientId(patientId);
		shLinkManifest.setStatus("finalized");
		shLinkManifest.setPasswordProtected(passcodeProtected);
		shLinkManifest.setPasscode(passcode);

		ShLinkManifest.FileManifest fileManifest = new ShLinkManifest.FileManifest();
		fileManifest.setContentType(APPLICATION_SMART_HEALTH_CARD_CONTENT_TYPE);
		fileManifest.setEmbedded(encryptedContent);
		shLinkManifest.addFiles(fileManifest);

		shlinkManifestStoreService.saveManifest(shLinkManifest);

		shApiUrlService.replaceUrlWithShCardPattern(uriBuilder);
		shLinkUrl = uriBuilder
				.build(Map.of(MANIFEST_ID, shLinkManifest.getId()))
				.toURL();
		return shLinkUrl;
	}


	private @NotNull URL directFileShCardUrl(ShLinkPayload shLinkPayload, Tenant tenant, String patientId, String encryptedContent, UriComponentsBuilder uriBuilder) throws MalformedURLException {
		URL shLinkUrl;
		IisShLinkContent iisShLinkContent = new IisShLinkContent();
		iisShLinkContent.setTenant(tenant);
		if (shLinkPayload.getExp() != null) {
			iisShLinkContent.setExp(shLinkPayload.getExp());
		} else {
			iisShLinkContent.setExp(10000000L);
		}
		iisShLinkContent.setContent(encryptedContent);
		iisShlinkContentRepository.save(iisShLinkContent);
		shApiUrlService.replaceUrlWithShLinkPattern(uriBuilder);
		shLinkUrl = uriBuilder
				.build(Map.of(CONTENT_ID, iisShLinkContent.getId()))
				.toURL();
		return shLinkUrl;
	}

}
