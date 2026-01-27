package org.immregistries.iis.kernal.controllers.rest;

import org.immregistries.iis.kernal.controllers.RestConstants;

import ca.uhn.fhir.model.api.IElement;
import org.immregistries.iis.kernal.mapping.MappingService;
import org.immregistries.iis.kernal.model.IisMappedToFhir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RestConstants.Path.REST_PATH + RestConstants.Path.MAPPING_KEY_PATH)
public class FhirMappingRestController {
	@Autowired
	private MappingService mappingService;

	@PostMapping("/fhir")
	public IElement toFhirAll(
			@RequestBody IisMappedToFhir iisMappedToFhir) {
		return mappingService.fhirObject(iisMappedToFhir);
	}

	@PostMapping("/local")
	public IisMappedToFhir toIisAll(
			@RequestBody IElement iElement) {
		return mappingService.localObject(iElement);
	}
}
