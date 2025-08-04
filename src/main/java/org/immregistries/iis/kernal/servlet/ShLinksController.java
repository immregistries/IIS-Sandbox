package org.immregistries.iis.kernal.servlet;


import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.fhir.shl.ShlUtil;
import org.immregistries.iis.kernal.model.persisted.MessageReceived;
import org.immregistries.iis.kernal.model.persisted.ShLinkManifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

import static org.immregistries.iis.kernal.servlet.ShLinksController.SHLINKS_CONTROLLER_BASE_URL;

@RestController
@RequestMapping(SHLINKS_CONTROLLER_BASE_URL)
public class ShLinksController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	public final static String SHLINKS_CONTROLLER_BASE_URL = "/link";

	@GetMapping("/{id}")
	public ShLinkManifest getManifest(HttpServletRequest req, HttpServletResponse resp, @PathVariable("id") String manifestId) {
		ShLinkManifest shLinkManifest = null;
		resp.setContentType("application/json");
		try (Session dataSession = ServletHelper.getDataSession()) {
			Query query = dataSession.createQuery("from ShLinkManifest where id = :id", ShLinkManifest.class);
			query.setParameter("id", manifestId);
			shLinkManifest = (ShLinkManifest) query.getSingleResult();
		} catch (Exception e) {
			System.err.println("Unable to render page: " + e.getMessage());
			e.printStackTrace(System.err);
		}
		return shLinkManifest;
	}

	@GetMapping()
	public List getManifestAll(HttpServletRequest req, HttpServletResponse resp) {
		resp.setContentType("application/json");
		logger.info("ALL");
		try (Session dataSession = ServletHelper.getDataSession()) {
			Query query = dataSession.createQuery("from MessageReceived", MessageReceived.class);
			logger.info("Result {}", query.getResultList().size());
			return query.getResultList();
		} catch (Exception e) {
			System.err.println("Unable to render page: " + e.getMessage());
			e.printStackTrace(System.err);
		}
		return List.of();
	}

	@GetMapping("/$generate")
	public ShLinkManifest genManifest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		logger.info("generate");
		ShLinkManifest shLinkManifest = ShlUtil.generateManifest(ServletHelper.getTenant(req));
		resp.setContentType("application/json");
		try (Session dataSession = ServletHelper.getDataSession()) {
			dataSession.persist(shLinkManifest);
			logger.info("id {}", shLinkManifest.getId());
		}
		return shLinkManifest;
	}


}
