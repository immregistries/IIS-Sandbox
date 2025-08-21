package org.immregistries.iis.kernal.servlet.shlink;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.partition.IPartitionLookupSvc;
import ca.uhn.fhir.rest.param.TokenParam;
import com.nimbusds.jose.jwk.JWK;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.immregistries.iis.kernal.fhir.Application;
import org.immregistries.iis.kernal.fhir.ips.IpsGeneratorSvcIIS;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.fhir.shl.ShLinkPayload;
import org.immregistries.iis.kernal.logic.KeyStoreService;
import org.immregistries.iis.kernal.logic.shlink.IisShlinkContentService;
import org.immregistries.iis.kernal.logic.shlink.ShCardUtil;
import org.immregistries.iis.kernal.logic.shlink.ShlUtilService;
import org.immregistries.iis.kernal.model.persisted.*;
import org.immregistries.iis.kernal.servlet.HomeController;
import org.immregistries.iis.kernal.servlet.TenantController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import static org.immregistries.iis.kernal.servlet.LocationController.ACTION_SAVE;
import static org.immregistries.iis.kernal.servlet.LocationController.PARAM_ACTION;
import static org.immregistries.iis.kernal.servlet.shlink.ShLinkManifestController.SHLINKS_CONTROLLER_BASE_URL;

@RestController
@RequestMapping({ShLinkController.SHLINK_CONTROLLER_BASE_PATH, TenantController.TENANT_PATH + ShLinkController.SHLINK_CONTROLLER_BASE_PATH})
public class ShLinkController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public static final String SHLINK_CONTROLLER_PATH_KEY = "shlink";
	public static final String SHLINK_CONTROLLER_BASE_PATH = "/" + SHLINK_CONTROLLER_PATH_KEY;

	public static final String PARAM_KEY_ID = "keyId";
	public static final String PARAM_PATIENT_ID = "patientId";
	public static final String PARAM_FLAG = "flag";
	private static final String PARAM_EXP = "exp";

	@Autowired
	IpsGeneratorSvcIIS ipsGeneratorSvcIIS;

	@Autowired
	ShlUtilService shlUtilService;

	@Autowired
	KeyStoreService keyStoreService;

	@Autowired
	IPartitionLookupSvc partitionLookupSvc;
	@Autowired
	IisShlinkContentService iisShlinkContentService;
	@Autowired
	FhirContext fhirContext;

	@Autowired
	ShCardUtil shCardUtil;

	@PostMapping("/ips")
	protected void doPost(HttpServletRequest req, HttpServletResponse resp,
								 @RequestParam(PARAM_KEY_ID) String keyId,
								 @RequestParam(PARAM_PATIENT_ID) String patientId,
								 @RequestParam(PARAM_FLAG) String flag,
								 @RequestParam(PARAM_EXP) String exp,
								 @RequestParam("image") boolean image
	)
		throws ServletException, IOException {
		Long expLong = Long.getLong(exp);
		Tenant tenant = ServletHelper.getTenantRedirectIfNone(req, resp);
		UserAccess userAccess = ServletHelper.getUserAccess();

		OutputStream outputStream = resp.getOutputStream();
		PrintWriter out = new PrintWriter(outputStream);

		IisKey iisKey;
		if (keyId == null) {
			iisKey = keyStoreService.saveKey(keyStoreService.generateEc(), tenant, userAccess);
		} else {
			iisKey = keyStoreService.getKey(keyId, userAccess);
		}
		JWK jwk = iisKey.jwk();

		String url = "";
		IBaseBundle ips = ipsGeneratorSvcIIS.generateIps(ServletHelper.requestDetailsWithPartitionName(partitionLookupSvc), new TokenParam(patientId), "");
		String content = fhirContext.newJsonParser().encodeResourceToString(ips); // TODO compress
		String shCard = shCardUtil.qrCodeWrite(content, req, iisKey.getId(), userAccess);
		if (StringUtils.contains(flag, "U")) {
			IisShlinkContent iisShlinkContent = new IisShlinkContent();
			iisShlinkContent.setUserAccess(userAccess);
			iisShlinkContent.setExp(expLong);
			iisShlinkContent.setContent(shCard);
			iisShlinkContentService.saveIisShlinkContent(iisShlinkContent);
		} else {
			ShLinkManifest.FileManifest fileManifest = new ShLinkManifest.FileManifest();
			fileManifest.setContentType("shcard");
			fileManifest.setEmbedded(shCard);


			ShLinkManifest shLinkManifest = new ShLinkManifest();
			shLinkManifest.setTenant(tenant);
			shLinkManifest.setStatus("finalized");
			shLinkManifest.addFiles(fileManifest);

			shlUtilService.saveManifest(shLinkManifest);
			UriComponentsBuilder builder = ServletUriComponentsBuilder.fromRequest(req);
			builder.replacePath(Application.IIS_PATH_BASE + SHLINKS_CONTROLLER_BASE_URL + shLinkManifest.getId());

			url = builder.build().toUri().toURL().toString();
		}


		ShLinkPayload shLinkPayload = new ShLinkPayload();
		shLinkPayload.setLabel("Generated for Shlink testing with IPS of Synthetic Patient");
		shLinkPayload.setFlag(flag);
		shLinkPayload.setExp(expLong);
		shLinkPayload.setUrl(url);

		String qrCode = shlUtilService.qrCode(shLinkPayload);
		if (image) {
			shlUtilService.printQrCodeAsImage(outputStream, qrCode);
		} else {
			resp.setContentType("text/html");
			HomeController.doHeader(out, "Smart Health Link Result", tenant);

			out.println("<p>" +
				qrCode +
				"</p>");
			HomeController.doFooter(out);
		}
		out.flush();
		out.close();

	}


	@GetMapping()
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		logger.info("Called");
		Tenant tenant = ServletHelper.getTenantRedirectIfNone(req, resp);

		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		HomeController.doHeader(out, "Smart Health Link Form", tenant);

		out.println("    <div class=\"w3-container w3-margin-top\">");
		out.println("    <h3>Generate Shlink</h3>");
		out.println(
			"    <form method=\"POST\" action=\"ips\" class=\"w3-container w3-card-4\">");
		out.println("      <label>Patient ID</label>");
		out.println(
			"      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_PATIENT_ID
				+ "\" value=\"Patient/\"/>");
		out.println("      <label>Flag</label>");
		out.println(
			"      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_FLAG
				+ "\" value=\"\"/>");
		out.println("      <label>Key id</label>");
		out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_KEY_ID
			+ "\" value=\"\"/>");
		out.println("      <label>Expiration (s)</label>");
		out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_EXP
			+ "\" value=\"" + 10000000 + "\"/>");

		out.println(
			"          <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\""
				+ PARAM_ACTION + "\" value=\"" + ACTION_SAVE + "\"/>");
		out.println("    </form>");
		out.println("    </div>");

		HomeController.doFooter(out);
		out.flush();
		out.close();


	}
}
