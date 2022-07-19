package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.model.OrgAccess;
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

public class SubscriptionTools extends HttpServlet {
	@Autowired
	RepositoryClientFactory repositoryClientFactory;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		// TODO action as manual trigger with content
		SubscriptionTopic.SubscriptionTopicResourceTriggerComponent patientTrigger = new SubscriptionTopic.SubscriptionTopicResourceTriggerComponent()
			.setResource("Patient");
		SubscriptionTopic.SubscriptionTopicResourceTriggerComponent operationOutcomeTrigger = new SubscriptionTopic.SubscriptionTopicResourceTriggerComponent()
			.setResource("OperationOutcome");
		SubscriptionTopic.SubscriptionTopicEventTriggerComponent eventTrigger =
			new SubscriptionTopic.SubscriptionTopicEventTriggerComponent().setEvent( new CodeableConcept()
				// https://terminology.hl7.org/3.1.0/ValueSet-v2-0003.html
				.addCoding(new Coding().setSystem("http://terminology.hl7.org/ValueSet/v2-0003").setCode("A04"))
				.addCoding(new Coding().setSystem("http://terminology.hl7.org/ValueSet/v2-0003").setCode("A28"))
				.addCoding(new Coding().setSystem("http://terminology.hl7.org/ValueSet/v2-0003").setCode("A31"))
			).setResource("Patient");

		SubscriptionTopic topic  = new SubscriptionTopic()
			.setDescription("Testing communication between EHR and IIS and operation outcome")
			.setUrl("https://florence.immregistries.org/iis-sandbox/fhir/SubscriptionTopic")
			.setStatus(Enumerations.PublicationStatus.DRAFT)
			.setExperimental(true).setPublisher("Aira/Nist").setTitle("Health equity data quality requests within Immunization systems");

		topic.addResourceTrigger(patientTrigger);
		topic.addResourceTrigger(operationOutcomeTrigger);
		topic.addEventTrigger(eventTrigger);
		topic.addNotificationShape().setResource("OperationOutcome");
		// TODO include topic in provider

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
		IGenericClient fhirClient = repositoryClientFactory.newGenericClient(orgAccess);
		PrintWriter out = new PrintWriter(resp.getOutputStream());

		String subscriptionId = req.getParameter(PARAM_SUBSCRIPTION_ID);
		if (subscriptionId == null) {
			printSearchAndSelect(req,resp,out,fhirClient);
		} else {
			printTools(req,resp,out,fhirClient, subscriptionId);
		}


		out.flush();
		out.close();
	}

	private void printTools(HttpServletRequest req, HttpServletResponse resp,
												 PrintWriter out,IGenericClient fhirClient, String subscriptionId) {
		HttpSession session = req.getSession(true);
		try {
			HomeServlet.doHeader(out, session);

			Bundle bundle = fhirClient.search().forResource(Subscription.class)
				.where(Subscription.IDENTIFIER.exactly().identifier(subscriptionId)).returnBundle(Bundle.class).execute();
			if (bundle.hasEntry()) {
				Subscription subscription = (Subscription) bundle.getEntryFirstRep().getResource();
				out.println("<div class=\"w3-panel\"><p>"+ subscription.getStatus()+"/p></div>");
			} else {
				out.println("<div class=\"w3-panel w3-yellow\"><p>No Record Found</p></div>");
			}
			HomeServlet.doFooter(out,session);

		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

	}

	private void printSearchAndSelect(HttpServletRequest req, HttpServletResponse resp,
	PrintWriter out,IGenericClient fhirClient) {
		HttpSession session = req.getSession(true);
		try {
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

	}
}
