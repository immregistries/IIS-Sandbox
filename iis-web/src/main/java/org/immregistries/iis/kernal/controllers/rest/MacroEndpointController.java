package org.immregistries.iis.kernal.controllers.rest;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import jakarta.servlet.ServletException;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.logic.macro.MacroEndpointService;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.immregistries.iis.kernal.security.UserAccessUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController()
@RequestMapping({ IisRestPath.BasePath.REST_PATH + IisRestPath.BasePath.$_CREATE_PATH, "/$create" })
public class MacroEndpointController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private FhirContext fhirContext;
	@Autowired
	private UserAccessUtil userAccessUtil;
	@Autowired
	private MacroEndpointService macroEndpointService;

	// @GetMapping("/StructureDefinition")
	// public ResponseEntity<String> getStructureDefinition() {
	// fhirContext.
	// StructureDefinition structureDefinition =
	// ClasspathUtil.loadResource(fhirContext,StructureDefinition.class,
	// "org/hl7/fhir/r4/model/patient.json");
	// return ResponseEntity.ok().body(structureDefinition.getUrl());
	// }
	@GetMapping("/StructureDefinition")
	public ResponseEntity<String> getStructureDefinition2() {
		RuntimeResourceDefinition runtimeResourceDefinition = fhirContext.getResourceDefinition("Patient");
		logger.info("profile test{}", runtimeResourceDefinition.getResourceProfile("localhost:8080/fhir/b"));
		logger.info("profile test{}", runtimeResourceDefinition);

		return ResponseEntity.ok()
				.body(fhirContext.newJsonParser().encodeResourceToString(runtimeResourceDefinition.toProfile("test")));
	}

	@PostMapping
	protected ResponseEntity<Tenant> doPost(@RequestBody String bundleString)
			throws ServletException, IOException {
		UserAccess userAccess = userAccessUtil.getUserAccess();
		return ResponseEntity.ok().body(macroEndpointService.generateTenantAndContent(bundleString, userAccess));
	}

	@GetMapping
	protected ResponseEntity<Tenant> doGet(@RequestBody String bundleString) throws ServletException, IOException {
		return doPost(bundleString);
	}

}
