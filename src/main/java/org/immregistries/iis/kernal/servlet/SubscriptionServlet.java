package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import ca.uhn.fhir.jpa.subscription.submit.interceptor.SubscriptionMatcherInterceptor;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.instance.model.api.IBaseResource;
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

public class SubscriptionServlet extends HttpServlet {
	@Autowired
	RepositoryClientFactory repositoryClientFactory;
	@Autowired
	SubscriptionMatcherInterceptor subscriptionMatcherInterceptor;

	public static final String PARAM_ACTION = "action";
	public static final String PARAM_MESSAGE = "message";
	public static final String ACTION_SEARCH = "search";
	public static final String PARAM_SUBSCRIPTION_ENDPOINT = "endpoint";
	public static final String PARAM_SUBSCRIPTION_ID = "identifier";

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		// TODO action as manual trigger with content
		HttpSession session = req.getSession(true);
		OrgAccess orgAccess = (OrgAccess) session.getAttribute("orgAccess");
		if (orgAccess == null) {
			RequestDispatcher dispatcher = req.getRequestDispatcher("home");
			dispatcher.forward(req, resp);
			return;
		}
		IGenericClient localClient = (IGenericClient) session.getAttribute("fhirClient");
		IParser parser = repositoryClientFactory.getFhirContext().newJsonParser();

		String subscriptionId = req.getParameter(PARAM_SUBSCRIPTION_ID);

		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		try {
			Bundle searchBundle = localClient.search().forResource(Subscription.class)
				.where(Subscription.IDENTIFIER.exactly().identifier(subscriptionId)).returnBundle(Bundle.class).execute();
//			Subscription subscription = localClient.read().resource(Subscription.class).withId(subscriptionId).execute();
			if (searchBundle.hasEntry()) {
				Subscription subscription = (Subscription) searchBundle.getEntryFirstRep().getResource();

				String message = req.getParameter(PARAM_MESSAGE);
				IBaseResource parsedResource = parser.parseResource(message);
//				subscriptionMatcherInterceptor.resourceCreated(parsedResource, new SystemRequestDetails().setRequestPartitionId(RequestPartitionId.fromPartitionName(""+orgAccess.getAccessName())));

				IGenericClient endpointClient = repositoryClientFactory.newGenericClient(subscription.getEndpoint());
				Bundle notificationBundle = new Bundle(Bundle.BundleType.SUBSCRIPTIONNOTIFICATION);
				SubscriptionStatus status = new SubscriptionStatus()
					.setType(SubscriptionStatus.SubscriptionNotificationType.EVENTNOTIFICATION)
					.setStatus(subscription.getStatus())
					.setSubscription(subscription.getIdentifierFirstRep().getAssigner())
					.setTopic(subscription.getTopic());
				notificationBundle.addEntry().setResource(status);
				notificationBundle.addEntry().setResource((Resource) parsedResource);

				MethodOutcome outcome = endpointClient.create().resource(notificationBundle).execute();
				if (outcome.getResource() != null) {
					out.println(parser.encodeResourceToString(outcome.getResource()));
				}
				if (outcome.getOperationOutcome() != null) {
					out.println(parser.encodeResourceToString(outcome.getOperationOutcome()));
				}
				if (outcome.getId() != null){
					out.println(outcome.getId());
				}
			} else {
				out.println("NO SUBSCRIPTION FOUND FOR THIS IDENTIFIER");
			}
		} catch (Exception e) {
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			e.printStackTrace(out);
			e.printStackTrace(System.err);
		}
		out.flush();
		out.close();
//		doGet(req, resp);
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
		IGenericClient fhirClient = (IGenericClient) session.getAttribute("fhirClient");
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
			String message = "";

			Subscription subscription;
			Bundle bundle = fhirClient.search().forResource(Subscription.class)
				.where(Subscription.IDENTIFIER.exactly().code(subscriptionId)).returnBundle(Bundle.class).execute();
			if (bundle.hasEntry()) {
				subscription = (Subscription) bundle.getEntryFirstRep().getResource();
				printSubscription(out,subscription);
				out.println("<form action=\"subscription?identifier=" + subscription.getIdentifierFirstRep().getValue() + "\" method=\"POST\" target=\"_blank\">");
				out.println("<textarea class=\"w3-input w3-border\" name=\"" + PARAM_MESSAGE
					+ "\"rows=\"15\" cols=\"160\">" + message + "</textarea></td>");
				out.println("<h4>Send OperationOutcome  resource to subscriber</h4>");
				out.println("<input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"submit\" value=\"Submit\"/>");
				out.println("</form>");


			} else {
				out.println("<div class=\"w3-panel w3-yellow\"><p>Not Found</p></div>");
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
				"    <form method=\"GET\" action=\"subscription\" class=\"w3-container w3-card-4\">");
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
					String link = "subscription?" + PARAM_SUBSCRIPTION_ID + "="
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

	public void printSubscription(PrintWriter out, Subscription subscription) {
		SimpleDateFormat sdfDate = new SimpleDateFormat("MM/dd/yyyy");
		out.println("    <div class=\"w3-container w3-half w3-margin-top\">");
		out.println("<table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
		out.println("  <tbody>");
		out.println("  <tr>");
		out.println("    <th class=\"w3-green\">Identifier</th>");
		out.println("    <td>" + subscription.getIdentifierFirstRep().getValue() + "</td>");
		out.println("  </tr>");
		out.println("  <tr>");
		out.println("    <th class=\"w3-green\">Topic</th>");
		out.println("    <td>" + subscription.getTopicElement().getValue() + "</td>");
		out.println("  </tr>");
		out.println("  <tr>");
		out.println("    <th class=\"w3-green\">Endpoint</th>");
		out.println("    <td>" + subscription.getEndpoint() + "</td>");
		out.println("  </tr>");
		out.println("  <tr>");
		out.println("    <th class=\"w3-green\">Status</th>");
		out.println("    <td>" + subscription.getStatus() + "</td>");
		out.println("  </tr>");
		out.println("  <tr>");
		out.println("    <th class=\"w3-green\">Content Type</th>");
		out.println("    <td>" + subscription.getContentType()+ "</td>");
		out.println("  </tr>");
		out.println("  </tbody>");
		out.println("</table>");
		out.println("</div>");
	}

}
