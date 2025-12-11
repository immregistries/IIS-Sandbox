package org.immregistries.iis.kernal.servlet.shlink;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.partition.IPartitionLookupSvc;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.IdType;
import org.immregistries.iis.kernal.fhir.ips.IpsGeneratorSvcIIS;

import org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.fhir.security.TenantUtil;
import org.immregistries.iis.kernal.fhir.security.UserAccessUtil;
import org.immregistries.iis.kernal.fhir.shl.ShLinkPayload;
import org.immregistries.iis.kernal.logic.KeyStoreService;
import org.immregistries.iis.kernal.logic.shlink.ShLinkUtilService;
import org.immregistries.iis.kernal.persisted.model.IisKey;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.persisted.model.UserAccess;
import org.immregistries.iis.kernal.servlet.HomeController;
import org.immregistries.iis.kernal.servlet.TenantController;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

import static org.immregistries.iis.kernal.servlet.LocationController.PARAM_ACTION;

@RestController
@RequestMapping({ShLinkController.SHLINK_CONTROLLER_BASE_PATH, TenantController.TENANT_PATH + ShLinkController.SHLINK_CONTROLLER_BASE_PATH})
public class ShLinkController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public static final String SHLINK_CONTROLLER_PATH_KEY = "shlink";
	public static final String SHLINK_CONTROLLER_BASE_PATH = "/" + SHLINK_CONTROLLER_PATH_KEY;

	public static final String PARAM_SECRET_KEY = "secretKey";
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
	IPartitionLookupSvc partitionLookupSvc;

	@Autowired
	FhirContext fhirContext;

	@PostMapping()
	protected void shLinkIPS(HttpServletRequest req, HttpServletResponse resp,
									 @RequestParam(value = PARAM_KEY_ID, required = false) String keyId,
									 @RequestParam(value = PARAM_SECRET_KEY, required = false) String secretKey,
								 @RequestParam(PARAM_PATIENT_ID) String patientId,
								 @RequestParam(PARAM_FLAG) String flag,
								 @RequestParam(PARAM_EXP) String exp,
								 @RequestParam(value = "image", required = false) boolean image
	)
		throws ServletException, IOException, NoSuchAlgorithmException {
		Tenant tenant = CurrentTenantUtil.getTenantRedirectIfNone(req, resp);
		UserAccess userAccess = UserAccessUtil.getUserAccess();
		OutputStream outputStream = resp.getOutputStream();
		PrintWriter out = new PrintWriter(outputStream);
		/*
		 * Choosing or generating the keys based on the parameters
		 */
		SecretKeySpec encryptionKeySpec = getSecretEncryptionKey(secretKey);
		IisKey iisSigningKey = keyStoreService.getIisSigningKeyOrCreate(keyId, userAccess, tenant);
		/*
		 * Payload skeleton
		 */
		ShLinkPayload shLinkPayload = new ShLinkPayload();
		shLinkPayload.setLabel("Generated for ShLink testing with IPS of Synthetic Patient");
		shLinkPayload.setFlag(flag);
		shLinkPayload.setKey(new String(Base64.getUrlEncoder().encode(encryptionKeySpec.getEncoded())));
		shLinkPayload.setExp(Long.getLong(exp));
		/*
		 * Getting the bundle for the payload content
		 */
		IBaseBundle ipsToBeEncoded = ipsGeneratorSvcIIS.generateIps(TenantUtil.requestDetailsWithPartitionName(partitionLookupSvc), new IdType(patientId), "");
		/*
		 * Convert the bundle to a shcard file
		 */
		String url = shLinkUtilService.generateShLinkUrlForShCards(List.of(ipsToBeEncoded), shLinkPayload, req, iisSigningKey, encryptionKeySpec, userAccess, tenant);
		shLinkPayload.setUrl(url);
		String qrCode = shLinkUtilService.qrCode(shLinkPayload);
		if (image) {
			resp.setContentType("image/png"); // Set content type for PNG image
			shLinkUtilService.printQrCodeAsImage(outputStream, qrCode);
		} else {
			resp.setContentType("text/html");
			HomeController.doHeader(out, "Smart Health Link Result", tenant);
			out.println("<h3>Smart health link</h3>");
			out.println("<textarea name=\"shlink\" readonly style=\"width: 100%; height: 5em;\" >");
			out.print(qrCode);
			out.println("</textarea>");
			IisKeyController.printIisKey(out, iisSigningKey);
			HomeController.doFooter(out);
		}
		out.flush();
		out.close();

	}

	private @NotNull SecretKeySpec getSecretEncryptionKey(String secretKey) throws NoSuchAlgorithmException {
		SecretKeySpec encryptionKeySpec;
		if (StringUtils.isNotBlank(secretKey)) {
			encryptionKeySpec = new SecretKeySpec(Base64.getDecoder().decode(secretKey), 0, secretKey.length(), "AES");
		} else {
			encryptionKeySpec = shLinkUtilService.generateSecretKey();
		}
		return encryptionKeySpec;
	}

	private IisKey getIisSigningKey(String keyId, UserAccess userAccess, Tenant tenant) {
		IisKey iisSigningKey;
		if (StringUtils.isNotBlank(keyId)) {
			iisSigningKey = keyStoreService.getKey(keyId, userAccess);
		} else {
			iisSigningKey = keyStoreService.getAnyKey(userAccess);
		}
		if (iisSigningKey == null) {
			iisSigningKey = keyStoreService.saveKey(keyStoreService.generateEc(), tenant, userAccess);
		}
		return iisSigningKey;
	}


	@GetMapping()
	protected void doGet(HttpServletRequest req, HttpServletResponse resp,
								@RequestParam(value = PARAM_KEY_ID, required = false) String keyId,
								@RequestParam(value = PARAM_PATIENT_ID, required = false) String patientId,
								@RequestParam(value = PARAM_FLAG, required = false) String flag,
								@RequestParam(value = PARAM_EXP, required = false) String exp)
		throws ServletException, IOException {
		Tenant tenant = CurrentTenantUtil.getTenantRedirectIfNone(req, resp);
		UserAccess userAccess = UserAccessUtil.getUserAccess();

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
//		out.println("      <label>Encryption Key for documents (generated if null)</label>");
//		out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_SECRET_KEY
//			+ "\" value=\"" + StringUtils.defaultIfBlank(keyId, "")
//			+ "\"/>");
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
