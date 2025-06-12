package org.immregistries.iis.kernal.logic.messageHandling;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.hl7v2.HL7Exception;
import gov.cdc.izgw.v2tofhir.converter.MessageParser;
import org.hl7.fhir.r4.model.Bundle;
import org.immregistries.iis.kernal.mapping.internalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.model.Tenant;
import org.springframework.beans.factory.annotation.Autowired;

//@Service
//@Conditional(OnR4Condition.class)
public class V2ToFhirMessageHandlerDeprecated {

	@Autowired
	RepositoryClientFactory repositoryClientFactory;
	@Autowired
	FhirContext fhirContext;

	public String process(String message, Tenant tenant, String facilityName) {
		MessageParser parser = new MessageParser();
		try {
			Bundle bundle = parser.convert(message);

			return fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);
//			MethodOutcome result = repositoryClientFactory.getFhirClient().create().resource(bundle).execute();
//			return  fhirContext.newJsonParser().encodeResourceToString(result.getResource());
		} catch (HL7Exception e) {
			throw new RuntimeException(e);
		}
	}
//
//	public MethodOutcome methodOutcomeFromAckTest(String ack) {
//		MessageParser parser = new MessageParser();
//		parser.convert(ack)
//	}
}
