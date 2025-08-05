package org.immregistries.iis.kernal.servlet;


import jakarta.persistence.Query;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.fhir.shl.ShlUtilService;
import org.immregistries.iis.kernal.model.persisted.ShLinkManifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static org.immregistries.iis.kernal.servlet.ShLinksManifestController.SHLINKS_CONTROLLER_BASE_URL;

@RestController
@RequestMapping(SHLINKS_CONTROLLER_BASE_URL)
public class ShLinksManifestController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	public final static String SHLINKS_CONTROLLER_BASE_URL = "/link";

	@Autowired
	ShlUtilService shlUtilService;

	@PostMapping()
	public ShLinkManifest postManifest(HttpServletRequest req, HttpServletResponse resp, @RequestBody ShLinkManifest shLinkManifest) {
		resp.setContentType("application/json");
		return shlUtilService.saveManifest(shLinkManifest);
	}

	@GetMapping("/{id}")
	public ShLinkManifest getManifest(HttpServletRequest req, HttpServletResponse resp, @PathVariable("id") String manifestId) {
		resp.setContentType("application/json");
		return shlUtilService.readShLinkManifest(manifestId);
	}

	@GetMapping("/{id}/qr")
	public void printQr(HttpServletRequest req, HttpServletResponse resp, @PathVariable("id") String manifestId) throws IOException, ServletException {
		ShLinkManifest shLinkManifest = shlUtilService.readShLinkManifest(manifestId);
		String url = req.getContextPath().split("/qr")[0];
		resp.setContentType("image/png"); // Set content type for PNG image
		OutputStream out = resp.getOutputStream();
		shlUtilService.printQrCode(out, url);
		out.flush();
		out.close();
	}


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
		return postManifest(req, resp, shLinkManifest);
	}

}
