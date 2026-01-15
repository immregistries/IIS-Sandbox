package org.immregistries.iis.kernal.controllers.rest;


import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.mapping.AllMappingService;
import org.immregistries.iis.kernal.model.IisMappedToFhirResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RestUrlUtil.REST_PATH + "/mapping")
public class FhirMappingRestController {

	@Autowired
	AllMappingService mappingService;

	@PostMapping("/toFhir")
	public IBaseResource toFhir(
		@RequestBody IisMappedToFhirResource iisDiffableObject
	) {
		return mappingService.fhirResource(iisDiffableObject);
	}
}
