package org.immregistries.iis.kernal.controllers.rest;

import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;
import ca.uhn.hl7v2.HL7Exception;
import gov.cdc.izgw.v2tofhir.converter.MessageParser;
import org.hl7.fhir.r4.model.Bundle;
import org.immregistries.iis.kernal.controllers.IisRestParam;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.*;

import static org.immregistries.iis.kernal.controllers.IisRestPath.BasePath.V2_TO_FHIR_PATH;

@RestController
@RequestMapping({ IisRestPath.BasePath.REST_PATH + V2_TO_FHIR_PATH,
		IisRestPath.REST_TENANT_PATH + V2_TO_FHIR_PATH })
@Conditional(OnR4Condition.class)
public class V2ToFhirRestController {

	@Autowired
	private MessageParser messageParser;

	@PostMapping
	public Bundle convertV2ToFhir(@RequestBody String message,
			@RequestParam(name = IisRestParam.FACILITY_NAME, required = false) String facilityName)
			throws HL7Exception {
		return messageParser.convert(message);
	}
}
