package org.immregistries.iis.kernal.logic;

import ca.uhn.hl7v2.HL7Exception;
import gov.cdc.izgw.v2tofhir.converter.MessageParser;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class V2ToFhirService {

	@Autowired(required = false)
	private MessageParser messageParser;

	public Bundle v2ToFhirBundle(String hl7Message) throws HL7Exception {
		if (messageParser == null) {
			return null;
		}
		return messageParser.convert(hl7Message);
	}
}
