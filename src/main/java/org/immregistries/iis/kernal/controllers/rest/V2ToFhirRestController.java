package org.immregistries.iis.kernal.controllers.rest;

import org.immregistries.iis.kernal.controllers.RestConstants;

import ca.uhn.hl7v2.HL7Exception;
import gov.cdc.izgw.v2tofhir.converter.MessageParser;
import org.hl7.fhir.r4.model.Bundle;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.*;

import static org.immregistries.iis.kernal.controllers.RestConstants.Path.V2_TO_FHIR_PATH;

@RestController
@RequestMapping({ RestConstants.Path.REST_PATH + V2_TO_FHIR_PATH,
		RestConstants.Path.REST_TENANT_PATH + V2_TO_FHIR_PATH })
@Conditional(OnR4Condition.class)
public class V2ToFhirRestController {

	@Autowired
	private MessageParser messageParser;

	@PostMapping
	public Bundle convertV2ToFhir(@RequestBody String message,
			@RequestParam(name = RestConstants.Param.FACILITY_NAME, required = false) String facilityName)
			throws HL7Exception {
		return messageParser.convert(message);
	}
}
