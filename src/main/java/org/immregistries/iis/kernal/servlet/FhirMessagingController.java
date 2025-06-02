package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.hl7v2.HL7Exception;
import gov.cdc.izgw.v2tofhir.converter.MessageParser;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.StringType;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.fhir.interceptors.PartitionCreationInterceptor;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.logic.BaseIISSOAPServer;
import org.immregistries.iis.kernal.logic.IIncomingMessageHandler;
import org.immregistries.iis.kernal.model.Tenant;
import org.immregistries.smm.cdc.CDCWSDLServer;
import org.immregistries.smm.cdc.Fault;
import org.immregistries.smm.cdc.SubmitSingleMessage;
import org.immregistries.smm.cdc.UnknownFault;
import org.immregistries.smm.transform.ScenarioManager;
import org.immregistries.smm.transform.TestCaseMessage;
import org.immregistries.smm.transform.Transformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;

import static org.immregistries.iis.kernal.fhir.security.ServletHelper.SESSION_TENANT;
import static org.immregistries.iis.kernal.servlet.FhirMessagingController.FHIR_MESSAGING_BASE_PATH;
import static org.immregistries.iis.kernal.servlet.PopController.PARAM_FACILITY_NAME;
import static org.immregistries.iis.kernal.servlet.PopController.PARAM_MESSAGE;
import static org.immregistries.iis.kernal.servlet.TenantController.PATH_VARIABLE_TENANT_NAME;


@RestController()
@RequestMapping({FHIR_MESSAGING_BASE_PATH, TenantController.TENANT_PATH + FHIR_MESSAGING_BASE_PATH})
@Conditional(OnR4Condition.class)
public class FhirMessagingController {
	public static final String FHIR_MESSAGING_PATH_KEY = "fhirMessaging";
	public static final String FHIR_MESSAGING_BASE_PATH = "/" + FHIR_MESSAGING_PATH_KEY;
	public static final String ORIGINAL_TEXT_EXTENSION_URL = "http://hl7.org/fhir/StructureDefinition/originalText";
	@Autowired
	IIncomingMessageHandler incomingMessageHandler;
	@Autowired
	FhirContext fhirContext;

	@Autowired
	private PartitionCreationInterceptor partitionCreationInterceptor;

	@PostMapping
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
//		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		Session dataSession = null;
		try {
			dataSession = ServletHelper.getDataSession();
			Tenant tenant = ServletHelper.getTenant(req, dataSession);
			String result = "";
			String message = req.getParameter(PARAM_MESSAGE);
			String facility_name = req.getParameter(PARAM_FACILITY_NAME);
			if (tenant == null) {
				resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				out.println("Access is not authorized. FacilityId, userid and/or password are not recognized. ");
			} else {
				if (StringUtils.isBlank(message)) {
					throw new RuntimeException("Blank message not accepted");
				}
				String fhirResult = processInput(message, tenant, facility_name);
				out.print(fhirResult);
				resp.setContentType("text/plain");
			}
		} catch (Exception e) {
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			e.printStackTrace(out);
			e.printStackTrace(System.err);
		} finally {
			if (dataSession != null) {
				dataSession.close();
			}
			out.flush();
			out.close();
		}
	}

	private String processInput(String message, Tenant tenant, String facility_name) throws HL7Exception {
		IParser parser;
		if (message.startsWith("{")) {
			parser = fhirContext.newJsonParser();
		} else {
			parser = fhirContext.newXmlParser();
		}
		parser.setPrettyPrint(true);
		Bundle bundle = (Bundle) parser.parseResource(message);
		/*
		 * Temporary solution of extracting the original V2 message from Document reference
		 * TODO integrate or create a converter of FHIR messaging back to V2 message when available
		 */
		DocumentReference documentReference = (DocumentReference) bundle.getEntryFirstRep().getResource();
		StringType v2Message = (StringType) documentReference.getContent().get(0).getExtensionByUrl(ORIGINAL_TEXT_EXTENSION_URL).getValue();
		String v2Result = incomingMessageHandler.process(v2Message.getValueNotNull(), tenant, facility_name);
		MessageParser messageParser = new MessageParser();
		Bundle resultBundle = messageParser.convert(v2Result);
		String fhirResult = parser.encodeResourceToString(resultBundle);
		return fhirResult;
	}

	@GetMapping
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/html");
		Tenant tenant = ServletHelper.getTenant(req, ServletHelper.getDataSession());

		PrintWriter out = new PrintWriter(resp.getOutputStream());
		try {
			String message = req.getParameter(PARAM_MESSAGE);
			String organizationName = req.getParameter(PARAM_FACILITY_NAME);
			if (organizationName == null) {
				organizationName = "";
			}
			if (StringUtils.isBlank(message)) {
				TestCaseMessage testCaseMessage =
					ScenarioManager.createTestCaseMessage(ScenarioManager.SCENARIO_1_R_ADMIN_CHILD);
				Transformer transformer = new Transformer();
				transformer.transform(testCaseMessage);
				MessageParser messageParser = new MessageParser();
				Bundle bundle = messageParser.convert(testCaseMessage.getMessageText());
				message = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);
			}

			HomeServlet.doHeader(out, "IIS Sandbox - FHIR Messaging", tenant);
			out.println("<h2>Experimental FHIR Messaging Endpoint</h2>");
			PopController.printForm(out, "FHIR Bundle", message, organizationName, FHIR_MESSAGING_PATH_KEY);
			HomeServlet.doFooter(out);

		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		out.flush();
		out.close();
	}

	@PostMapping("/soap")
	protected void doPost(HttpServletRequest req, HttpServletResponse resp, @PathVariable(name = PATH_VARIABLE_TENANT_NAME, required = false) String tenantName)
		throws ServletException, IOException {

		String path = req.getPathInfo();
		final String processorName =
			path == null ? "" : (path.startsWith("/") ? path.substring(1) : path);
		CDCWSDLServer server = new BaseIISSOAPServer(partitionCreationInterceptor, tenantName) {
			@Override
			public void process(SubmitSingleMessage ssm, PrintWriter out) throws Fault {

				String message = ssm.getHl7Message();
				String userId = ssm.getUsername();
				String password = ssm.getPassword();
				String facilityId = ssm.getFacilityID();

				String ack = "";
				Session dataSession = ServletHelper.getDataSession();
				String[] messages;
				StringBuilder resultBuilder = new StringBuilder();
				try {
					Tenant tenant;
					if (StringUtils.isNotBlank(tenantName)) {
						tenant = ServletHelper.authenticateTenant(userId, password, tenantName, dataSession, partitionCreationInterceptor);
					} else {
						tenant = ServletHelper.authenticateTenant(userId, password, facilityId, dataSession, partitionCreationInterceptor);
					}
					if (tenant == null) {
						throw new SecurityException("Username/password combination is unrecognized");
					} else {
						HttpSession session = req.getSession(true);
						session.setAttribute(SESSION_TENANT, tenant);
						processInput(message, tenant, facilityId);
						ack = resultBuilder.toString();
					}
				} catch (Exception e) {
					throw new UnknownFault("Unable to process request: " + e.getMessage(), e);
				} finally {
					dataSession.close();
				}
				out.print(ack);
			}
		};
		server.setProcessorName(processorName);
		server.process(req, resp);
	}

}
