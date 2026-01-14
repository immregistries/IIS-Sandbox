package org.immregistries.iis.kernal.controllers.rest;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.hl7v2.HL7Exception;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.controllers.servlet.SoapDescriptionController;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.security.TenantUtil;
import org.immregistries.iis.kernal.logic.BaseIISSOAPServer;
import org.immregistries.iis.kernal.logic.messageHandling.FhirMessagingHandler;
import org.immregistries.iis.kernal.logic.messageHandling.V2IncomingMessageHandler;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.smm.cdc.CDCWSDLServer;
import org.immregistries.smm.cdc.Fault;
import org.immregistries.smm.cdc.SubmitSingleMessage;
import org.immregistries.smm.cdc.UnknownFault;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;

import static org.immregistries.iis.kernal.controllers.rest.FhirMessagingRestController.FHIR_MESSAGING_KEY_PATH;
import static org.immregistries.iis.kernal.controllers.servlet.PopController.PARAM_FACILITY_NAME;
import static org.immregistries.iis.kernal.controllers.servlet.PopController.PARAM_MESSAGE;
import static org.immregistries.iis.kernal.controllers.servlet.TenantController.TENANT_NAME;
import static org.immregistries.iis.kernal.security.CurrentTenantUtil.SESSION_REQUEST_TENANT;

@RestController
@RequestMapping({ RestUrlUtil.REST_PATH + FHIR_MESSAGING_KEY_PATH, RestUrlUtil.REST_TENANT_PATH + FHIR_MESSAGING_KEY_PATH,
		"/tenant/{tenantName}/fhirMessaging" })
@Conditional(OnR4Condition.class)
public class FhirMessagingRestController {

	public static final String FHIR_MESSAGING_KEY_PATH = "/fhirMessaging";
	@Autowired
	FhirContext fhirContext;
	@Autowired
	FhirMessagingHandler fhirMessagingHandler;

	@Autowired
	private TenantUtil tenantUtil;
	@Autowired
	private V2IncomingMessageHandler handler;

	@PostMapping(produces = MediaType.TEXT_PLAIN_VALUE)
	protected String doPost(@RequestParam(PARAM_MESSAGE) String message,
			@RequestParam(PARAM_FACILITY_NAME) String facilityName,
			@RequestAttribute(SESSION_REQUEST_TENANT) @NotNull Tenant tenant)
			throws ServletException, IOException, HL7Exception {
		// resp.setContentType("text/html");
		if (StringUtils.isBlank(message)) {
			throw new RuntimeException("Blank message not accepted");
		}
		return processInput(message, tenant, facilityName);
	}

	private String processInput(String message, Tenant tenant, String facility_name) {
		IParser parser;
		if (message.startsWith("{")) {
			parser = fhirContext.newJsonParser();
		} else {
			parser = fhirContext.newXmlParser();
		}
		parser.setPrettyPrint(true);
		String fhirResult;
		fhirResult = fhirMessagingHandler.process(message, tenant, facility_name);
		/*
		 * Temporary solution of extracting the original V2 message from Document
		 * reference
		 * TODO integrate or create a converter of FHIR messaging back to V2 message
		 * when available
		 */
		// Bundle bundle = (Bundle) parser.parseResource(message);
		// DocumentReference documentReference = (DocumentReference)
		// bundle.getEntryFirstRep().getResource();
		// StringType v2Message = (StringType)
		// documentReference.getContent().get(0).getExtensionByUrl(ORIGINAL_TEXT_EXTENSION_URL).getValue();
		// String v2Result = incomingMessageHandler.process(v2Message.getValueNotNull(),
		// tenant, facility_name);
		// MessageParser messageParser = new MessageParser();
		// Bundle resultBundle = messageParser.convert(v2Result);
		// fhirResult = parser.encodeResourceToString(resultBundle);
		return fhirResult;
	}

	@PostMapping(SoapDescriptionController.SOAP_BASE_PATH)
	protected void doPost(HttpServletRequest req, HttpServletResponse resp,
			@PathVariable(name = TENANT_NAME, required = false) String tenantName)
			throws ServletException, IOException {

		String path = req.getPathInfo();
		final String processorName = path == null ? "" : (path.startsWith("/") ? path.substring(1) : path);
		CDCWSDLServer server = new BaseIISSOAPServer(tenantName, tenantUtil) {
			@Override
			public void process(SubmitSingleMessage ssm, PrintWriter out) throws Fault {

				String message = ssm.getHl7Message();
				String facilityId = ssm.getFacilityID();

				String ack = "";
				try {
					/*
					 * Tenant is accessed through RequestContext, and was previously set through the
					 * authorize method of WSDL server in BaseIISSOAPServer.java
					 */
					Tenant tenant = CurrentTenantUtil.getTenant();
					if (tenant == null) {
						throw new SecurityException("Username/password combination is unrecognized");
					} else {
						req.setAttribute(SESSION_REQUEST_TENANT, tenant);
						ack = processInput(message, tenant, facilityId);
					}
				} catch (Exception e) {
					throw new UnknownFault("Unable to process request: " + e.getMessage(), e);
				}
				out.print(ack);
			}
		};
		server.setProcessorName(processorName);
		server.process(req, resp);
	}

}
