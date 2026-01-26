package org.immregistries.iis.kernal.controllers.rest;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import jakarta.servlet.http.HttpServletRequest;
import org.immregistries.iis.kernal.controllers.rest.filters.RestTenantUrlFilter;
import org.immregistries.iis.kernal.controllers.servlet.PopController;
import org.immregistries.iis.kernal.logic.hl7v2.handling.V2IncomingMessageHandler;
import org.immregistries.iis.kernal.mapping.IisFhirClientFactory;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.smm.transform.ScenarioManager;
import org.immregistries.smm.transform.TestCaseMessage;
import org.immregistries.smm.transform.Transformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;

import static org.immregistries.iis.kernal.controllers.servlet.PopController.PARAM_FACILITY_NAME;

@RestController
@RequestMapping(RestUrlUtil.REST_TENANT_PATH + PopRestController.POP)
public class PopRestController {

	public static final String POP = "/pop";
	@Autowired
	private FhirContext fhirContext;
	@Autowired
	private IisFhirClientFactory iisFhirClientFactory;
	@Autowired
	private V2IncomingMessageHandler handler;

	@GetMapping(value = "/sample", produces = MediaType.TEXT_PLAIN_VALUE)
	public String getSampleMessage() {
		TestCaseMessage testCaseMessage = ScenarioManager
			.createTestCaseMessage(ScenarioManager.SCENARIO_1_R_ADMIN_CHILD);
		Transformer transformer = new Transformer();
		transformer.transform(testCaseMessage);
		return testCaseMessage.getMessageText();
	}

	@PostMapping
	public String postPop(
		@RequestBody String message,
		@RequestParam(value = PARAM_FACILITY_NAME, required = false) String facilityName,
		@RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
		HttpServletRequest req) {
		if (message == null) {
			return "";
		}

		String[] messages = message.split(PopController.MSH_HEADER_REGEX);
		if (messages.length > 2) {
			req.setAttribute("groupPatientIds", new ArrayList<String>());
		}

		StringBuilder ackBuilder = new StringBuilder();
		for (String msh : messages) {
			if (!msh.isBlank()) {
				String ack = handler.process(PopController.MSH_HEADER + msh, tenant, facilityName);
				ackBuilder.append(ack);
			}
		}

		/**
		 * Saving a group if multiple patients were sent
		 */
		@SuppressWarnings("unchecked")
		ArrayList<String> groupPatientIds = (ArrayList<String>) req.getAttribute("groupPatientIds");
		if (groupPatientIds != null) {
			if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
				org.hl7.fhir.r5.model.Group group = new org.hl7.fhir.r5.model.Group();
				for (String id : groupPatientIds) {
					group.addMember()
						.setEntity(new org.hl7.fhir.r5.model.Reference().setReference("Patient/" + id));
				}
				group.setDescription("Generated from Hl2v2 VXU Query on  time " + new Date());
				iisFhirClientFactory.getOrCreateGenericClient(req).create().resource(group).execute();
			} else {
				org.hl7.fhir.r4.model.Group group = new org.hl7.fhir.r4.model.Group();
				for (String id : groupPatientIds) {
					group.addMember()
						.setEntity(new org.hl7.fhir.r4.model.Reference().setReference("Patient/" + id));
				}
				iisFhirClientFactory.getOrCreateGenericClient(req).create().resource(group).execute();
			}
		}

		return ackBuilder.toString();
	}
}
