package org.immregistries.iis.kernal.controllers.servlet.shlink;

import com.google.zxing.WriterException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.controllers.request.shlink.ShLinkCreationRequestDTO;
import org.immregistries.iis.kernal.controllers.rest.PatientRestController;
import org.immregistries.iis.kernal.controllers.rest.shlink.IisKeyRestController;
import org.immregistries.iis.kernal.controllers.rest.shlink.ShLinkRestController;
import org.immregistries.iis.kernal.controllers.servlet.TenantController;
import org.immregistries.iis.kernal.controllers.servlet.util.UiQrCodeUtil;
import org.immregistries.iis.kernal.controllers.servlet.util.UiUtil;
import org.immregistries.iis.kernal.model.IisPatient;
import org.immregistries.iis.kernal.persisted.entities.IisKey;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static org.immregistries.iis.kernal.controllers.servlet.LocationController.PARAM_ACTION;

@RestController
@RequestMapping({ ShLinkController.SHLINK_CONTROLLER_BASE_PATH,
		TenantController.TENANT_PATH + ShLinkController.SHLINK_CONTROLLER_BASE_PATH })
public class ShLinkController {


	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public static final String SHLINK_CONTROLLER_PATH_KEY = IisRestPath.Key.SH_LINK_KEY;
	public static final String SHLINK_CONTROLLER_BASE_PATH = "/" + SHLINK_CONTROLLER_PATH_KEY;

	public static final String PARAM_SECRET_KEY = "secretKey";
	public static final String PARAM_KEY_ID = "keyId";
	public static final String PARAM_PATIENT_ID = "patientId";
	public static final String PARAM_FLAG = "flag";
	private static final String PARAM_EXP = "exp";
	private static final String PARAM_PASSCODE = "passcode";

	public static final String ACTION_SAVE = "Generate";

	public static final String QR_TO_IMAGE_BASE_PATH = "/qr-to-image";
	public static final String QR_CODE_PARAM = "qrCode";

	@Autowired
	private IisKeyRestController iisKeyRestController;
	@Autowired
	private ShLinkRestController shLinkRestController;
	@Autowired
	private PatientRestController patientRestController;

	@Autowired
	private UiUtil uiUtil;
	@Autowired
	private UiQrCodeUtil uiQrCodeUtil;
	@Autowired
	private IisKeyController iisKeyController;

	@PostMapping()
	protected void shLinkIPS(
		@AuthenticationPrincipal UserAccess userAccess,
		HttpServletRequest req,
		HttpServletResponse resp,
			@RequestParam(value = PARAM_KEY_ID, required = false) String keyId,
			@RequestParam(value = PARAM_SECRET_KEY, required = false) String secretKey,
			@RequestParam(PARAM_PATIENT_ID) String patientId,
			@RequestParam(PARAM_FLAG) String flag,
			@RequestParam(PARAM_EXP) String exp,
		@RequestParam(PARAM_PASSCODE) String passcode,
			@RequestParam(value = "image", required = false) boolean image)
			throws ServletException, IOException, NoSuchAlgorithmException, WriterException {
		Tenant tenant = uiUtil.getTenantRedirectIfNone(req, resp);

		OutputStream outputStream = resp.getOutputStream();
		PrintWriter out = new PrintWriter(outputStream);
		/*
		 * Choosing or generating the keys based on the parameters
		 */
		IisKey iisSigningKey = iisKeyRestController.getOrCreateKey(userAccess, tenant, keyId);

		ShLinkCreationRequestDTO dto = ShLinkCreationRequestDTO.builder()
			.keyId(iisSigningKey.getKeyId()) // Extracting from the object as requested
			.secretKey(secretKey)
			.patientId(patientId)
			.flag(flag)
			.passcode(passcode)
			.exp(exp)
			.build();
		String qrCode = shLinkRestController.createShLinkIPSQrCode(userAccess, req, dto, tenant);


		if (image) {
			resp.setContentType("image/png"); // Set content type for PNG image
			uiQrCodeUtil.printQrCodeAsImage(outputStream, qrCode);
		} else {
			IisPatient iisPatient = patientRestController.getPatient(patientId, tenant, true);
			String description = "Newly generated Qr Code, with IPS of patient " + iisPatient.getLegalNameOrFirst().asSingleString();
			String imageUrl = req.getRequestURL().toString() + QR_TO_IMAGE_BASE_PATH + "?" + QR_CODE_PARAM + "=" + qrCode;
			resp.setContentType("text/html");
			uiUtil.doHeader(out, "Smart Health Link Result", tenant);
			out.println("<h2>Smart health link Generated</h2>");
			uiQrCodeUtil.prettyPrintQrCodeCard(out, "Smart Health link", description, imageUrl, qrCode, "");
			/*
			 * Useful instructions
			 */
			out.println("<h3>Signing Key used</h3>");
			out.println("<h4>Key id : " + iisSigningKey.getKeyId() + "</h4>");
			out.println("<textarea textarea name=\"sh-link\" readonly style=\"height: 3em;\" >" +
				iisSigningKey.jwk().toPublicJWK().toJSONString() + "</textarea>");
			if (flag.contains("P")) {
				out.println("<h3>Passcode Protected - P flag activated</h3>");
				if ("".equals(passcode)) {
					out.println("You activated the P Flag without giving a passcode, IIS sandbox user password will be used as passcode.");
				} else {
					out.println("<h4>Smart Health Link's Passcode</h4>");
					out.println("<textarea textarea name=\"passcode\" readonly >" + passcode + "</textarea>");
				}
			}
			if (flag.contains("U")) {
				out.println("<h3>Direct File type Smart Health Link - U flag activated</h3>");
				out.println("<div style=\"width: 100%; height: 5em;\">");
				out.println("Direct file type Smart Health Link, no manifest was created");
				out.println("<a href='https://build.fhir.org/ig/HL7/smart-health-cards-and-links/links-specification.html#smart-health-link-direct-file-request-with-u-flag'>More details</a>");
				out.println("</div>");
			}
			uiUtil.doFooter(out);
		}
		out.flush();
		out.close();

	}

	/**
	 * Used in UI to quickly print within page
	 *
	 * @param req
	 * @param resp
	 * @param qrCode
	 * @throws ServletException
	 * @throws IOException
	 * @throws WriterException
	 */
	@GetMapping(value = QR_TO_IMAGE_BASE_PATH)
	public void quickPrint(
		HttpServletRequest req,
		HttpServletResponse resp,
		@RequestParam(QR_CODE_PARAM) String qrCode)
		throws ServletException, IOException, WriterException {
		OutputStream outputStream = resp.getOutputStream();
		PrintWriter out = new PrintWriter(outputStream);
		resp.setContentType("image/png"); // Set content type for PNG image
		uiQrCodeUtil.printQrCodeAsImage(outputStream, qrCode);
	}


	@GetMapping()
	protected void doGet(
		@AuthenticationPrincipal UserAccess userAccess,
		HttpServletRequest req, HttpServletResponse resp,
			@RequestParam(value = PARAM_KEY_ID, required = false) String keyId,
			@RequestParam(value = PARAM_PATIENT_ID, required = false) String patientId,
			@RequestParam(value = PARAM_FLAG, required = false) String flag,
			@RequestParam(value = PARAM_EXP, required = false) String exp)
			throws ServletException, IOException {
		Tenant tenant = uiUtil.getTenantRedirectIfNone(req, resp);

		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		uiUtil.doHeader(out, "Smart Health Link Form", tenant);

		out.println("    <div class=\"w3-container w3-margin-top\">");
		out.println("    <h3>Generate ShLink</h3>");
		out.println(
			"    <form method=\"POST\" action=\"" + SHLINK_CONTROLLER_PATH_KEY +
						"\" target=\"_blank\"  class=\"w3-container w3-card-4\">");
		out.println("      <label>Patient ID</label>");
		out.println(
				"      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_PATIENT_ID
						+ "\" value=\"" + StringUtils.defaultIfBlank(patientId, "Patient/")
					+ "\"/>");
		out.println("      <label>Flag (examples: 'LU' | 'LP' | 'P')</label>");
		out.println(
				"      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_FLAG
						+ "\" value=\"" + StringUtils.defaultIfBlank(flag, "")
						+ "\"/>"); // TODO add options
		// out.println(" <label>Encryption Key for documents (generated if
		// null)</label>");
		// out.println(" <input class=\"w3-input\" type=\"text\" name=\"" +
		// PARAM_SECRET_KEY
		// + "\" value=\"" + StringUtils.defaultIfBlank(keyId, "")
		// + "\"/>");
		out.println("      <label>Expiration (s)</label>");
		out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_EXP
				+ "\" value=\"" + StringUtils.defaultIfBlank(exp, "10000000") + "\"/>");

		out.println("      <label>Passcode - Only if flag includes \"P\" </label>");
		out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_PASSCODE
			+ "\" value=\"\"/>");

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

		List<IisKey> iisKeys = iisKeyRestController.getKeys(userAccess);
		iisKeyController.printIisKeys(out, iisKeys, tenant);
		out.println("    </div>");
		out.println("    <div class=\"w3-container\">");
		out.println("    <h3>Flag Explanation</h3>");
		out.println("<ul>" +
			"<li>L: Indicates the SMART Health Link is intended for long-term use and manifest content can evolve over time.</li> " +
			"<li>P: Indicates the SMART Health Link requires a Passcode to resolve. (In IIS Sandbox -> request must be authenticated)</li> " +
			"<li>U: Indicates the SMART Health Links's `url` resolves to a single encrypted file accessible via `GET`, bypassing the manifest. SHALL NOT be used in combination with P.</li> " +
			"</ul>");

		out.println("    </div>");

		uiUtil.doFooter(out);
		out.flush();
		out.close();

	}
}
