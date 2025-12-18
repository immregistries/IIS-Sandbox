package org.immregistries.iis.kernal.controllers.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.immregistries.iis.kernal.controllers.rest.MessageRestController;
import org.immregistries.iis.kernal.controllers.servlet.util.PatientServletUtil;
import org.immregistries.iis.kernal.controllers.servlet.util.UiUtil;
import org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.persisted.model.MessageReceived;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import static org.immregistries.iis.kernal.controllers.servlet.MessageController.MESSAGE_BASE_PATH;

@SuppressWarnings("serial")
@RestController
@RequestMapping({ MESSAGE_BASE_PATH, TenantController.TENANT_PATH + MESSAGE_BASE_PATH })
public class MessageController {

	@Autowired
	private MessageRestController messageRestController;

	public static final String MESSAGE_PATH_KEY = "message";
	public static final String MESSAGE_BASE_PATH = "/" + MESSAGE_PATH_KEY;

	public static final String PARAM_ORG_ID = "orgId";

	public static final String PARAM_ACTION = "action";

	public static final String ACTION_SEARCH = "Search";

	public static final String PARAM_SEARCH = "search";

	@PostMapping
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doGet(req, resp);
	}

	@GetMapping
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		try {
			String messageError = null;
			String messageConfirmation = null;
			UiUtil.doHeader(out, "IIS Sandbox");
			if (messageError != null) {
				out.println("  <div class=\"w3-panel w3-red\">");
				out.println("    <p>" + messageError + "</p>");
				out.println("  </div>");
			}
			if (messageConfirmation != null) {
				out.println("  <div class=\"w3-panel w3-green\">");
				out.println("    <p>" + messageConfirmation + "</p>");
				out.println("  </div>");
			}

			Tenant tenant = CurrentTenantUtil.getTenant();
			if (tenant != null) {
				out.println("    <div class=\"w3-container w3-half w3-margin-top\">");
				out.println("    <h2>Facility: " + tenant.getOrganizationName() + "</h2>");
				out.println("    <h3>Messages Recently Received</h3>");
				String search = req.getParameter(PARAM_SEARCH);
				if (search == null) {
					search = "";
				}
				out.println(
						"    <form method=\"GET\" action=\"message\" class=\"w3-container w3-card-4\">");
				out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_SEARCH
						+ "\" value=\"" + search + "\"/>");
				out.println(
						"          <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\""
								+ PARAM_ACTION + "\" value=\"" + ACTION_SEARCH + "\"/>");
				out.println("    </form>");
				out.println("    </div>");

				out.println("    <div class=\"w3-container\">");
				List<MessageReceived> messageReceivedList = messageRestController.getMessages(tenant, search);

				if (messageReceivedList.size() == 0) {
					out.println("     <em>None Received</em>");
				} else {
					for (MessageReceived messageReceived : messageReceivedList) {
						PatientServletUtil.printMessageReceived(out, messageReceived);
					}
				}
				out.println("    </div>");
			}
		} catch (Exception e) {
			System.err.println("Unable to render page: " + e.getMessage());
			e.printStackTrace(System.err);
		}
		UiUtil.doFooter(out);
		out.flush();
		out.close();
	}

}
