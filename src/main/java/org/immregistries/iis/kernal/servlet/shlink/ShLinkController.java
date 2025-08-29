package org.immregistries.iis.kernal.servlet.shlink;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.partition.IPartitionLookupSvc;
import com.google.gson.Gson;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.IdType;
import org.immregistries.iis.kernal.JwtUtils;
import org.immregistries.iis.kernal.fhir.Application;
import org.immregistries.iis.kernal.fhir.ips.IpsGeneratorSvcIIS;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.fhir.shl.ShLinkPayload;
import org.immregistries.iis.kernal.logic.KeyStoreService;
import org.immregistries.iis.kernal.logic.shlink.IisShLinkContentService;
import org.immregistries.iis.kernal.logic.shlink.ShCardUtil;
import org.immregistries.iis.kernal.logic.shlink.ShLinkUtilService;
import org.immregistries.iis.kernal.model.persisted.*;
import org.immregistries.iis.kernal.servlet.HomeController;
import org.immregistries.iis.kernal.servlet.TenantController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.immregistries.iis.kernal.logic.shlink.ShCardUtil.VERIFIABLE_CREDENTIAL_TYPE;
import static org.immregistries.iis.kernal.logic.shlink.ShLinkUtilService.VERIFIABLE_CREDENTIAL;
import static org.immregistries.iis.kernal.servlet.LocationController.PARAM_ACTION;
import static org.immregistries.iis.kernal.servlet.shlink.ShLinkManifestController.SHLINKS_CONTROLLER_BASE_URL;

@RestController
@RequestMapping({ShLinkController.SHLINK_CONTROLLER_BASE_PATH, TenantController.TENANT_PATH + ShLinkController.SHLINK_CONTROLLER_BASE_PATH})
public class ShLinkController {
	public static final String APPLICATION_SMART_HEALTH_CARD_CONTENT_TYPE = "application/smart-health-card";
	public static final String APPLICATION_FHIR_JSON_CONTENT_TYPE = "application/fhir+json";
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public static final String SHLINK_CONTROLLER_PATH_KEY = "shlink";
	public static final String SHLINK_CONTROLLER_BASE_PATH = "/" + SHLINK_CONTROLLER_PATH_KEY;

	public static final String PARAM_KEY_ID = "keyId";
	public static final String PARAM_PATIENT_ID = "patientId";
	public static final String PARAM_FLAG = "flag";
	private static final String PARAM_EXP = "exp";

	public static final String ACTION_SAVE = "Generate";


	@Autowired
	IpsGeneratorSvcIIS ipsGeneratorSvcIIS;

	@Autowired
	ShLinkUtilService shLinkUtilService;

	@Autowired
	KeyStoreService keyStoreService;

	@Autowired
	JwtUtils jwtUtils;

	@Autowired
	IPartitionLookupSvc partitionLookupSvc;
	@Autowired
	IisShLinkContentService iisShLinkContentService;
	@Autowired
	FhirContext fhirContext;

	@Autowired
	ShCardUtil shCardUtil;

	@PostMapping()
	protected void doPost(HttpServletRequest req, HttpServletResponse resp,
								 @RequestParam(PARAM_KEY_ID) String keyId,
								 @RequestParam(PARAM_PATIENT_ID) String patientId,
								 @RequestParam(PARAM_FLAG) String flag,
								 @RequestParam(PARAM_EXP) String exp,
								 @RequestParam(value = "image", required = false) boolean image
	)
		throws ServletException, IOException, NoSuchAlgorithmException {
		Gson gson = new Gson();
		Long expLong = Long.getLong(exp);
		Tenant tenant = ServletHelper.getTenantRedirectIfNone(req, resp);
		UserAccess userAccess = ServletHelper.getUserAccess();

		SecretKeySpec encryptionKeySpec = shCardUtil.generateSecretKey();

		OutputStream outputStream = resp.getOutputStream();
		PrintWriter out = new PrintWriter(outputStream);

		IisKey iisKey;
		if (StringUtils.isNotBlank(keyId)) {
			iisKey = keyStoreService.getKey(keyId, userAccess);
		} else {
			iisKey = keyStoreService.saveKey(keyStoreService.generateEc(), tenant, userAccess);
		}
		String url = "";
		IBaseBundle ips = ipsGeneratorSvcIIS.generateIps(ServletHelper.requestDetailsWithPartitionName(partitionLookupSvc), new IdType(patientId), "");

		String shCardCompact = shCardUtil.qrCompact(ips, req, iisKey.getKeyId(), userAccess, tenant);

		Map<String, List<String>> contentToEncrypt = new HashMap<>(2);
		contentToEncrypt.put("type", List.of(VERIFIABLE_CREDENTIAL_TYPE, APPLICATION_SMART_HEALTH_CARD_CONTENT_TYPE));
		contentToEncrypt.put(VERIFIABLE_CREDENTIAL, List.of(shCardCompact));
		String encryptedContent = Jwts.builder()
			.content(gson.toJson(contentToEncrypt))
			.header().add("cty", APPLICATION_SMART_HEALTH_CARD_CONTENT_TYPE).and()
			.encryptWith(encryptionKeySpec, Jwts.ENC.A256GCM).compact(); // Alg specified in Smart health card IG

		byte[] decryptforLog = (byte[]) Jwts.parser().decryptWith(encryptionKeySpec).build().parse(encryptedContent).getPayload();
//		logger.info("Decrypt test 2 {}", new String(decryptforLog));
//		logger.info("Decrypt test 3 {}", Base64.getUrlDecoder().decode(decryptforLog));
		if (StringUtils.contains(flag, "U")) {
			IisShLinkContent iisShLinkContent = new IisShLinkContent();
			iisShLinkContent.setUserAccess(userAccess);
			iisShLinkContent.setExp(expLong);
			iisShLinkContent.setContent(encryptedContent);
			iisShLinkContentService.saveIisShLinkContent(iisShLinkContent);
		} else {
			ShLinkManifest shLinkManifest = new ShLinkManifest();
			shLinkManifest.setTenant(tenant);
			shLinkManifest.setStatus("finalized");

			ShLinkManifest.FileManifest fileManifest = new ShLinkManifest.FileManifest();
			fileManifest.setContentType(APPLICATION_SMART_HEALTH_CARD_CONTENT_TYPE);
			fileManifest.setEmbedded(encryptedContent);
			shLinkManifest.addFiles(fileManifest);

			shLinkUtilService.saveManifest(shLinkManifest);
			UriComponentsBuilder builder = ServletUriComponentsBuilder.fromRequest(req);
			builder.replacePath(Application.IIS_PATH_BASE + SHLINKS_CONTROLLER_BASE_URL + "/{manifestId}");


			url = builder
				.build(Map.of("manifestId", shLinkManifest.getId()))
				.toURL().toString();
		}


		ShLinkPayload shLinkPayload = new ShLinkPayload();
		shLinkPayload.setLabel("Generated for ShLink testing with IPS of Synthetic Patient");
		shLinkPayload.setFlag(flag);
		shLinkPayload.setKey(new String(Base64.getUrlEncoder().encode(encryptionKeySpec.getEncoded())));
		shLinkPayload.setExp(expLong);
		shLinkPayload.setUrl(url);
		logger.info("shlink payload {}", shLinkPayload);

		String qrCode = shLinkUtilService.qrCode(shLinkPayload);
		if (image) {
			shLinkUtilService.printQrCodeAsImage(outputStream, qrCode);
		} else {
			resp.setContentType("text/html");
			HomeController.doHeader(out, "Smart Health Link Result", tenant);
			out.println("<h3>Smart health link</h3>");
			out.println("<textarea name=\"shlink\" readonly style=\"width: 100%; height: 5em;\" >");
			out.print(qrCode);
			out.println("</textarea>");
			IisKeyController.printIisKey(out, iisKey);
			HomeController.doFooter(out);
		}
		out.flush();
		out.close();

	}


	@GetMapping()
	protected void doGet(HttpServletRequest req, HttpServletResponse resp,
								@RequestParam(value = PARAM_KEY_ID, required = false) String keyId,
								@RequestParam(value = PARAM_PATIENT_ID, required = false) String patientId,
								@RequestParam(value = PARAM_FLAG, required = false) String flag,
								@RequestParam(value = PARAM_EXP, required = false) String exp)
		throws ServletException, IOException {
		Tenant tenant = ServletHelper.getTenantRedirectIfNone(req, resp);
		UserAccess userAccess = ServletHelper.getUserAccess();

		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		HomeController.doHeader(out, "Smart Health Link Form", tenant);

		out.println("    <div class=\"w3-container w3-margin-top\">");
		out.println("    <h3>Generate ShLink</h3>");
		out.println(
			"    <form method=\"POST\" action=\"" +
				"shlink" +
				"\" target=\"_blank\"  class=\"w3-container w3-card-4\">");
		out.println("      <label>Patient ID</label>");
		out.println(
			"      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_PATIENT_ID
				+ "\" value=\"" + StringUtils.defaultIfBlank(patientId, "Patient/")
				+ "\"/>");
		out.println("      <label>Flag</label>");
		out.println(
			"      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_FLAG
				+ "\" value=\"" + StringUtils.defaultIfBlank(flag, "")
				+ "\"/>"); // TODO add options
		out.println("      <label>Encryption Key for documents (generated if null)</label>");
		out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_KEY_ID
			+ "\" value=\"" + StringUtils.defaultIfBlank(keyId, "")
			+ "\"/>");
		out.println("      <label>Expiration (s)</label>");
		out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_EXP
			+ "\" value=\"" + StringUtils.defaultIfBlank(exp, "10000000") + "\"/>");

		out.println(
			"          <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\""
				+ PARAM_ACTION + "\" value=\"" + ACTION_SAVE + "\"/>");
		out.println("    </form>");
		out.println("    </div>");

		out.println("    <div class=\"w3-container w3-half w3-margin-top\">");
		out.println("    <h2>Keys Available</h2>");
		out.println("    <h3>Keys used for signing Smart Health Cards signing (generated for the user)</h3>");
		out.println("    </div>");

		out.println("    <div class=\"w3-container\">");

		List<IisKey> iisKeys = keyStoreService.getKeys(userAccess);
		IisKeyController.printIisKeys(out, iisKeys, tenant);
		out.println("    </div>");

		HomeController.doFooter(out);
		out.flush();
		out.close();


	}
}
