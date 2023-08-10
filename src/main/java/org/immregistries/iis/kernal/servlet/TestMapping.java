package org.immregistries.iis.kernal.servlet;

import org.hibernate.Session;
import org.hl7.fhir.r5.model.*;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.iis.kernal.logic.*;
import org.immregistries.iis.kernal.mapping.Interfaces.*;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgMaster;
import org.immregistries.iis.kernal.model.ProcessingFlavor;
import org.immregistries.smm.tester.manager.HL7Reader;
import org.immregistries.smm.transform.ScenarioManager;
import org.immregistries.smm.transform.TestCaseMessage;
import org.immregistries.smm.transform.Transformer;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static javolution.testing.TestContext.assertEquals;

public class TestMapping extends HttpServlet {
	public static final String PARAM_MESSAGE = "MESSAGEDATA";

	@Autowired
	private IncomingMessageHandler handler;

	@Autowired
	PatientMapper<Patient> patientMapper;
	@Autowired
	ImmunizationMapper<Immunization> immunizationMapper;
	@Autowired
	LocationMapper<Location> locationMapper;
	@Autowired
	PractitionerMapper<Practitioner> practitionerMapper;
	@Autowired
	ObservationMapper<Observation> observationMapper;
	@Autowired
	PersonMapper<Person> personMapper;
	@Autowired
	RelatedPersonMapper<RelatedPerson> relatedPersonMapper;


	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		try {
			String message = req.getParameter(PARAM_MESSAGE);
			String userId = "utest";
			String password = "utest";
			String facilityId = "utest";
			OrgMaster orgMaster = ServletHelper.getOrgMaster();
			String ack = "";
			String[] messages;
			StringBuilder ackBuilder = new StringBuilder();
			Session dataSession = PopServlet.getDataSession();
			try {
				if (orgMaster == null) {
					resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					out.println(
						"Access is not authorized. Facilityid, userid and/or password are not recognized. ");
				} else {
					messages = message.split( "MSH\\|\\^~\\\\&\\|");
					for (String msh: messages) {
						if(!msh.isBlank()){
							testPatientMapping(orgMaster,msh);
							ackBuilder.append(handler.process("MSH|^~\\&|" + msh, orgMaster));
							ackBuilder.append("\r\n");
						}
					}
					ack = ackBuilder.toString();
				}
			} finally {
				dataSession.close();
			}
			resp.setContentType("text/plain");
			out.print(ack);
		} catch (Exception e) {
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			e.printStackTrace(out);
			e.printStackTrace(System.err);
		}
		out.flush();
		out.close();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {


		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		OrgAccess orgAccess = ServletHelper.getOrgAccess();
		try {
			String message = req.getParameter(PARAM_MESSAGE);
			if (message == null || message.equals("")) {
				TestCaseMessage testCaseMessage =
					ScenarioManager.createTestCaseMessage(ScenarioManager.SCENARIO_1_R_ADMIN_CHILD); // TODO TEST EVERY SCENARIO
				Transformer transformer = new Transformer();
				transformer.transform(testCaseMessage);
				message = testCaseMessage.getMessageText();
			}
			{
				HomeServlet.doHeader(out, "IIS Sandbox");
				out.println("    <h2>Send Now to Test Mapping</h2>");
				out.println("    <form action=\"utest\" method=\"POST\" target=\"_blank\">");
				out.println("      <h3>VXU Message</h3>");
				out.println("      <textarea class=\"w3-input\" name=\"" + PARAM_MESSAGE
					+ "\" rows=\"15\" cols=\"160\">" + message + "</textarea></td>");
				out.println("    <div class=\"w3-container w3-half w3-margin-top\">");

				out.println("    <div class=\"w3-container w3-card-4\">");
				out.println(
					"      <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"submit\" value=\"Submit\"/>");
				out.println("    <span class=\"w3-yellow\">Test Data Only</span>");

				out.println("    </div>");
				out.println("    </div>");
				out.println("    </form>");
				HomeServlet.doFooter(out);
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		out.flush();
		out.close();
	}

	void  testPatientMapping(OrgMaster orgMaster,String message) throws ProcessingException {
		List<ProcessingException> processingExceptionList = new ArrayList<>();
		HL7Reader hl7Reader = new HL7Reader(message);
		Set<ProcessingFlavor> processingFlavorSet = orgMaster.getProcessingFlavorSet();
		CodeMap codeMap = CodeMapManager.getCodeMap();

//		PatientReported patientReported = handler.processPatient(orgAccess,hl7Reader,processingExceptionList, orgAccess.getOrg().getProcessingFlavorSet(),codeMap,true,null,  handler.processManagingOrganization(hl7Reader));
//		PatientReported processedPatientReported = patientMapper.getReported(patientMapper.getFhirResource(patientReported));
//		patientReported.setPatientNameFirst("fff");
//
//		assertEquals(patientReported,processedPatientReported);

//		VaccinationReported vaccinationReported = handler.processVXU(orgAccess,hl7Reader,processingExceptionList, orgAccess.getOrg().getProcessingFlavorSet(),codeMap,true,null, handler.processManagingOrganization(hl7Reader));


	}

}
