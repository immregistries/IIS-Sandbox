package org.immregistries.iis.kernal.servlet.shlink;


import jakarta.persistence.Query;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.immregistries.iis.kernal.fhir.interceptors.PartitionCreationInterceptor;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.logic.shlink.ShlUtilService;
import org.immregistries.iis.kernal.model.persisted.ShLinkManifest;
import org.immregistries.iis.kernal.model.persisted.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

import static org.immregistries.iis.kernal.servlet.shlink.ShLinkManifestController.SHLINKS_CONTROLLER_BASE_URL;

@RestController
@RequestMapping(SHLINKS_CONTROLLER_BASE_URL)
public class ShLinkManifestController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	public final static String SHLINKS_CONTROLLER_BASE_URL = "/link";

	@Autowired
	ShlUtilService shlUtilService;
	@Autowired
	PartitionCreationInterceptor partitionCreationInterceptor;

	@GetMapping("/{id}")
	public ShLinkManifest getManifest(HttpServletRequest req, HttpServletResponse resp, @PathVariable("id") String manifestId) {
		resp.setContentType("application/json");
		return shlUtilService.readShLinkManifest(manifestId);
	}


	@PostMapping("/{id}")
	protected ShLinkManifest readShLinkManifest(HttpServletRequest req, HttpServletResponse resp,
															  @PathVariable("id") String manifestId,
															  @PathVariable(value = "tenantName", required = false) String tenantName,
															  @RequestParam(value = "recipient", required = false) String recipient,
															  @RequestParam(value = "passcode", required = false) String passcode,
															  @RequestParam(value = "embeddedLengthMax", required = false) String embeddedLengthMax
	) throws IOException, ServletException {
		resp.setContentType("application/json");
		if (StringUtils.isNoneBlank(passcode, tenantName)) {
			try (Session dataSession = ServletHelper.getDataSession()) {
				Tenant tenant = null;
				{
					tenant = ServletHelper.authenticateTenantNoUsername(passcode, tenantName, dataSession, partitionCreationInterceptor);
					if (tenant == null) {
						throw new AuthenticationCredentialsNotFoundException("No tenant found or invalid passcode");
					}
				}
			}
		}
		ShLinkManifest shLinkManifest = shlUtilService.readShLinkManifest(manifestId);
		return shLinkManifest;
	}

//	@GetMapping("/{id}/qr")
//	public void printQr(HttpServletRequest req, HttpServletResponse resp, @PathVariable("id") String manifestId) throws IOException, ServletException {
//		ShLinkManifest shLinkManifest = shlUtilService.readShLinkManifest(manifestId);
//		ShLinkPayload fullExamplePatientQrCode

	/// /		UriComponentsBuilder builder = ServletUriComponentsBuilder.fromRequestUri(req);
	/// /		builder.replacePath()
//		String url = req.getContextPath().split("/qr")[0];
//		resp.setContentType("image/png"); // Set content type for PNG image
//		OutputStream out = resp.getOutputStream();
//		shlUtilService.printQrCodeAsImage(out, url);
//		out.flush();
//		out.close();
//	}


	@GetMapping()
	public List getManifestAll(HttpServletRequest req, HttpServletResponse resp) {
		resp.setContentType("application/json");
		try (Session dataSession = ServletHelper.getDataSession()) {
			Query query = dataSession.createQuery("from ShLinkManifest", ShLinkManifest.class);
			return query.getResultList();
		} catch (Exception e) {
			System.err.println("Unable to render page: " + e.getMessage());
			e.printStackTrace(System.err);
		}
		return List.of();
	}

	@GetMapping("/$generate")
	public ShLinkManifest genManifest(HttpServletRequest req, HttpServletResponse resp) {
		ShLinkManifest shLinkManifest = shlUtilService.generateManifest(ServletHelper.getTenant(req));
		return shlUtilService.saveManifest(shLinkManifest);
	}

}
