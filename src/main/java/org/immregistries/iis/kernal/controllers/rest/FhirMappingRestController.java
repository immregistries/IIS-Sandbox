package org.immregistries.iis.kernal.controllers.rest;


import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.immregistries.iis.kernal.mapping.MappingService;
import org.immregistries.iis.kernal.mapping.MapperRegistry;
import org.immregistries.iis.kernal.model.IisMappedToFhir;
import org.immregistries.iis.kernal.model.IisMappedToFhirResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RestUrlUtil.REST_PATH + FhirMappingRestController.MAPPING_KEY_PATH)
public class FhirMappingRestController {

	public static final String MAPPING_KEY_PATH = "/mapping";
	@Autowired
	MappingService mappingService;
	@Autowired
	MapperRegistry mapperRegistry;

	@PostMapping("/resource")
	public IAnyResource toFhir(
		@RequestBody IisMappedToFhirResource iisDiffableObject
	) {
		return mappingService.fhirResource(iisDiffableObject);
	}

	@PostMapping("/field")
	public IBaseDatatype fieldR4(
		@RequestBody IisMappedToFhir iisMappedToFhir
	) {
		return mapperRegistry.fieldMapper(iisMappedToFhir).fhirObject(iisMappedToFhir);
	}

}
