package org.immregistries.iis.kernal.controllers.servlet.shlink;

import com.google.zxing.WriterException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.controllers.rest.shlink.IisKeyRestController;
import org.immregistries.iis.kernal.controllers.rest.shlink.ShLinkRestController;
import org.immregistries.iis.kernal.controllers.servlet.TenantController;
import org.immregistries.iis.kernal.controllers.servlet.util.UiQrCodeUtil;
import org.immregistries.iis.kernal.controllers.servlet.util.UiUtil;
import org.immregistries.iis.kernal.persisted.entities.IisKey;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

	public static final String SHLINK_CONTROLLER_PATH_KEY = "shlink";
	public static final String SHLINK_CONTROLLER_BASE_PATH = "/" + SHLINK_CONTROLLER_PATH_KEY;

	public static final String PARAM_SECRET_KEY = "secretKey";
	public static final String PARAM_KEY_ID = "keyId";
	public static final String PARAM_PATIENT_ID = "patientId";
	public static final String PARAM_FLAG = "flag";
	private static final String PARAM_EXP = "exp";

	public static final String ACTION_SAVE = "Generate";

	@Autowired
	IisKeyRestController iisKeyRestController;

	@Autowired
	ShLinkRestController shLinkRestController;

	@Autowired
	private UiUtil uiUtil;
	@Autowired
	private UiQrCodeUtil uiQrCodeUtil;
	@Autowired
	private IisKeyController iisKeyController;

	@PostMapping()
	protected void shLinkIPS(HttpServletRequest req, HttpServletResponse resp,
			@RequestParam(value = PARAM_KEY_ID, required = false) String keyId,
			@RequestParam(value = PARAM_SECRET_KEY, required = false) String secretKey,
			@RequestParam(PARAM_PATIENT_ID) String patientId,
			@RequestParam(PARAM_FLAG) String flag,
			@RequestParam(PARAM_EXP) String exp,
			@RequestParam(value = "image", required = false) boolean image)
			throws ServletException, IOException, NoSuchAlgorithmException, WriterException {
		Tenant tenant = uiUtil.getTenantRedirectIfNone(req, resp);

		OutputStream outputStream = resp.getOutputStream();
		PrintWriter out = new PrintWriter(outputStream);
		/*
		 * Choosing or generating the keys based on the parameters
		 */
		IisKey iisSigningKey = iisKeyRestController.getOrCreateKey(tenant, keyId);

		String qrCode = shLinkRestController.shLinkIPSQrCode(req, iisSigningKey.getKeyId(), secretKey, patientId, flag,
				exp, tenant);
		if (image) {
			resp.setContentType("image/png"); // Set content type for PNG image
			uiQrCodeUtil.printQrCodeAsImage(outputStream, qrCode);
		} else {
			resp.setContentType("text/html");
			uiUtil.doHeader(out, "Smart Health Link Result", tenant);
			out.println("<h3>Smart health link</h3>");
			out.println("<textarea name=\"shlink\" readonly style=\"width: 100%; height: 5em;\" >");
			out.print(qrCode);
			out.println("</textarea>");
			iisKeyController.printIisKey(out, iisSigningKey);
			uiUtil.doFooter(out);
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
		Tenant tenant = uiUtil.getTenantRedirectIfNone(req, resp);

		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		uiUtil.doHeader(out, "Smart Health Link Form", tenant);

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
		// out.println(" <label>Encryption Key for documents (generated if
		// null)</label>");
		// out.println(" <input class=\"w3-input\" type=\"text\" name=\"" +
		// PARAM_SECRET_KEY
		// + "\" value=\"" + StringUtils.defaultIfBlank(keyId, "")
		// + "\"/>");
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

		List<IisKey> iisKeys = iisKeyRestController.getKeys();
		iisKeyController.printIisKeys(out, iisKeys, tenant);
		out.println("    </div>");

		uiUtil.doFooter(out);
		out.flush();
		out.close();

	}
}
