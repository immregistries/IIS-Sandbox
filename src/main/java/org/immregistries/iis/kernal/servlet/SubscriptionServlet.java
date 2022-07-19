package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Subscription;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.repository.RepositoryClientFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;

public class SubscriptionServlet extends HttpServlet {
	@Autowired
	RepositoryClientFactory repositoryClientFactory;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		doGet(req, resp);
	}

	public static final String PARAM_ACTION = "action";
	public static final String ACTION_SEARCH = "search";
	public static final String PARAM_SUBSCRIPTION_ENDPOINT = "endpoint";
	public static final String PARAM_SUBSCRIPTION_ID = "identifier";


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

		try {
			IGenericClient fhirClient = repositoryClientFactory.newGenericClient(orgAccess);
			Bundle bundle;

			String endpoint = req.getParameter(PARAM_SUBSCRIPTION_ENDPOINT) == null ? "" : req.getParameter(PARAM_SUBSCRIPTION_ENDPOINT);
			String action = req.getParameter(PARAM_ACTION);
			if (action != null) {
				if (action.equals(ACTION_SEARCH)) {
					bundle = fhirClient.search().forResource(Subscription.class)
						.where(Subscription.URL.matches().value(endpoint)).returnBundle(Bundle.class).execute();
				} else {
					bundle = fhirClient.search().forResource(Subscription.class).returnBundle(Bundle.class).execute();
				}
			} else {
				bundle = fhirClient.search().forResource(Subscription.class).returnBundle(Bundle.class).execute();
			}

			HomeServlet.doHeader(out, session);

			out.println("    <div class=\"w3-container w3-half w3-margin-top\">");
			out.println("    <h3>Search Subscription</h3>");
			out.println(
				"    <form method=\"GET\" action=\"patient\" class=\"w3-container w3-card-4\">");
			out.println("      <input class=\"w3-input\" type=\"text\" name=\""
				+ PARAM_SUBSCRIPTION_ENDPOINT + "\" value=\"" + endpoint + "\"/>");
			out.println("      <label>ENDPOINT</label>");
			out.println(
				"          <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\""
					+ PARAM_ACTION + "\" value=\"" + ACTION_SEARCH + "\"/>");
			out.println("    </form>");
			out.println("    </div>");

			out.println("  <div class=\"w3-container\">");

			if (bundle.hasEntry()) {
				out.println(
					"<table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
				out.println("  <tr class=\"w3-green\">");
				out.println("    <th>Endpoint</th>");
				out.println("    <th>Status</th>");
				out.println("  </tr>");
				out.println("  <tbody>");
				SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
				int count = 0;
				for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
					Subscription subscription = (Subscription) entry.getResource();
					count++;
					if (count > 100) {
						break;
					}
					String link = "subscriptionDetail?" + PARAM_SUBSCRIPTION_ID + "="
						+ subscription.getIdentifierFirstRep().getValue(); // TODO or id
					out.println("  <tr>");
					out.println("    <td><a href=\"" + link + "\">"
						+ subscription.getEndpoint() + "</a></td>");
					out.println("    <td><a href=\"" + link + "\">"
						+ subscription.getStatus() + "</a></td>");
					out.println("  </tr>");
				}
				out.println("  </tbody>");
				out.println("</table>");

				if (count > 100) {
					out.println("<em>Only the first 100 are shown</em>");
				}
			} else {
				out.println("<div class=\"w3-panel w3-yellow\"><p>No Records Found</p></div>");
			}
			HomeServlet.doFooter(out,session);

		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		out.flush();
		out.close();
	}

}
