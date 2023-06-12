package org.immregistries.iis.kernal.servlet;

import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.repository.FhirRequester;
import org.immregistries.iis.kernal.repository.RepositoryClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Prototype of Servlet tool for group which would allow to
 * 	- generate accurate group for bulk and subscription
 * 	- provide option to add/remove a member
 * 	- link with a subscription
 *
 */
public class GroupServlet extends HttpServlet {
	Logger logger = LoggerFactory.getLogger(GroupServlet.class);
	@Autowired
	RepositoryClientFactory repositoryClientFactory;
	@Autowired
	FhirRequester fhirRequester;


	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		doGet(req, resp);
		HttpSession session = req.getSession(true);
		OrgAccess orgAccess = (OrgAccess) session.getAttribute("orgAccess");
		if (orgAccess == null) {
			RequestDispatcher dispatcher = req.getRequestDispatcher("home");
			dispatcher.forward(req, resp);
			return;
		}
		String orgString = req.getParameter("Organization");
//		Reference orgReference = repositoryClientFactory.getFhirContext().newJsonParser().parseResource(Reference.class,orgString);

	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		HttpSession session = req.getSession(true);
		OrgAccess orgAccess = (OrgAccess) session.getAttribute("orgAccess");
		if (orgAccess == null) {
			RequestDispatcher dispatcher = req.getRequestDispatcher("home");
			dispatcher.forward(req, resp);
			return;
		}
		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		HomeServlet.doHeader(out, session, "IIS Sandbox - Groups");
		Group group = new Group();
		group.setManagingEntity(new Reference().setIdentifier(new Identifier().setType(new CodeableConcept(new Coding().setCode("Organization"))).setSystem("AIRA_TEST").setValue("test")));
		group.setDescription("Generated Group in IIS sandbox, for Bulk data export use case and Synchronisation with subscription synchronisation");
		out.println("<p>");
		out.println(repositoryClientFactory.getFhirContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(group));
		out.println("</p>");
		Parameters parameters = new Parameters().addParameter("test", new Identifier().setValue("iii"));
		out.println("<p>");
		out.println(repositoryClientFactory.getFhirContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(parameters));
		out.println("</p>");
		HomeServlet.doFooter(out, session);
		out.flush();
		out.close();
	}

}
