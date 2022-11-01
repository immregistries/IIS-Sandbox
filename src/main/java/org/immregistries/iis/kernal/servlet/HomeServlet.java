package org.immregistries.iis.kernal.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.immregistries.iis.kernal.SoftwareVersion;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgMaster;
import org.immregistries.iis.kernal.model.ProcessingFlavor;
import org.springframework.stereotype.Component;

//@SuppressWarnings("serial")
//@Component
public class HomeServlet extends HttpServlet {

	public static final String PARAM_SHOW = "show";
	public static final String SHOW_FACILITIES = "facilities";

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		doGet(req, resp);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {

		HttpSession session = req.getSession(true);
		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		try {
			{
				doHeader(out, session);
				String show = req.getParameter(PARAM_SHOW);
				out.println("    <div class=\"w3-container w3-half w3-margin-top\">");
				if (show == null) {
					out.println(
						"    <div class=\"w3-panel w3-yellow\"><p class=\"w3-left-align\">This system is for test purposes only. "
							+ "Do not submit production data. As a precaution all submitted data will be deleted once a day.  </p></div>");
					out.println("    <h2>Documentation</h2>");
					out.println("    <p class=\"w3-left-align\">Please see the project wiki: "
						+ "<a href=\"https://github.com/immregistries/IIS-Sandbox/wiki\">https://github.com/immregistries/IIS-Sandbox/wiki</a></p>");
					out.println("    <h2>Primary Functions Supported</h2>");
					out.println("    <ul class=\"w3-ul w3-hoverable\">");
					out.println("      <li><a href=\"pop\">Send Now</a>: Send an HL7 message in now.</li>");
					out.println(
						"      <li><a href=\"message\">Messages</a>: Review recently submitted messages</li>");
					out.println(
						"      <li><a href=\"patient\">Patients</a>: See data received by patient</li>");
					out.println(
						"      <li><a href=\"location\">Patients</a>: See administered-at-locations</li>");
					out.println(
						"      <li><a href=\"subscription\">Subscriptions</a>: See fhir subscriptions</li>");
					out.println(
						"      <li><a href=\"soap\">CDC WSDL</a>: HL7 realtime interfacing using CDC WSDL</li>");
					out.println("    </ul>");
					out.println("    <h3>Secondary Functions Supported</h3>");
					out.println("    <ul class=\"w3-ul w3-hoverable\">");
					out.println(
						"      <li><a href=\"lab\">Convert ORU to VXU</a>: Convert an ORU lab message to a VXU. </li>");
					out.println(
						"      <li><a href=\"queryConverter\">Convert VXU to QBP</a>: Convert an VXU immunization message into an immunization query. </li>");
					out.println(
						"      <li><a href=\"covid\">COVID-19 Reporting</a>: Export data to demonstrate COVID-19 reporting </li>");
					out.println(
						"      <li><a href=\"VXUDownloadForm\">COVID-19 Reporting (HL7)</a>: Download data in HL7 format demonstrate COVID-19 reporting </li>");
					out.println(
						"      <li><a href=\"covidGenerate\">COVID-19 HL7 Generator</a>: Generate HL7 Messages</li>");
					out.println(
						"      <li><a href=\"event\">Submit Event</a>: Submit a patient and vaccination event manually.</li>");
					out.println(
						"      <li><a href=\"fhirTest\">FHIR Test Endpoint</a>: Create FHIR resources to test with IIS Sandbox.</li>");
					out.println(
						"      <li><a href=\"vciDemo\">VCI Demonstration</a>: Demonstration of RSP conversion steps for the Vaccine Credential Initiative</li>");
					out.println("    </ul>");

					out.println("    <h2>Processing Flavors</h2>");
					out.println(
						"    <p>If any of the following words appear in the name of the facility then special processing rules will apply. "
							+ "These processing rules can be used to simulate specific IIS behavior. </p>");
					out.println("    <ul class=\"w3-ul w3-hoverable\">");
					for (ProcessingFlavor processingFlavor : ProcessingFlavor.values()) {
						out.println("      <li>" + processingFlavor.getKey() + ": "
							+ processingFlavor.getBehaviorDescription() + "</li>");
					}

					out.println("    </ul>");

				} else if (show.equals(SHOW_FACILITIES)) {
					OrgAccess orgAccess = (OrgAccess) session.getAttribute("orgAccess");
					if (orgAccess != null) {
						out.println("    <h2>Facilities</h2>");
						Session dataSession = PopServlet.getDataSession();
						Map<Integer, OrgAccess> orgAccessMap =
							(Map<Integer, OrgAccess>) session.getAttribute("orgAccessMap");
						try {
							Query query = dataSession.createQuery("from OrgMaster order by organizationName");
							List<OrgMaster> orgMasterList = query.list();
							out.println("    <ul class=\"w3-ul w3-hoverable\">");
							for (OrgMaster orgMaster : orgMasterList) {
								if (orgAccessMap == null || !orgAccessMap.containsKey(orgMaster.getOrgId())) {
									out.println("      <li>" + orgMaster.getOrganizationName() + "</li>");
								} else {
									if (orgAccess.getOrg().equals(orgMaster)) {
										out.println(
											"      <li>" + orgMaster.getOrganizationName() + " (selected)</li>");
									} else {
										String link = "message?" + MessageServlet.PARAM_ACTION + "="
											+ MessageServlet.ACTION_SWITCH + "&" + MessageServlet.PARAM_ORG_ID + "="
											+ orgMaster.getOrgId();
										out.println("      <li> <a href=\"" + link + "\">"
											+ orgMaster.getOrganizationName() + "</a></li>");
									}
								}
							}
							out.println("    </ul>");
						} finally {
							dataSession.close();
						}
					}
				}
				out.println("  </div>");
				out.println(
					"  <img src=\"img/markus-spiske-dWaRJ3WBnGs-unsplash.jpg\" class=\"w3-round\" alt=\"Sandbox\" width=\"400\">");
				out.println(
					"<a style=\"background-color:black;color:white;text-decoration:none;padding:4px 6px;font-family:-apple-system, BlinkMacSystemFont, &quot;San Francisco&quot;, &quot;Helvetica Neue&quot;, Helvetica, Ubuntu, Roboto, Noto, &quot;Segoe UI&quot;, Arial, sans-serif;font-size:12px;font-weight:bold;line-height:1.2;display:inline-block;border-radius:3px\" href=\"https://unsplash.com/@markusspiske?utm_medium=referral&amp;utm_campaign=photographer-credit&amp;utm_content=creditBadge\" target=\"_blank\" rel=\"noopener noreferrer\" title=\"Download free do whatever you want high-resolution photos from Markus Spiske\"><span style=\"display:inline-block;padding:2px 3px\"><svg xmlns=\"http://www.w3.org/2000/svg\" style=\"height:12px;width:auto;position:relative;vertical-align:middle;top:-2px;fill:white\" viewBox=\"0 0 32 32\"><title>unsplash-logo</title><path d=\"M10 9V0h12v9H10zm12 5h10v18H0V14h10v9h12v-9z\"></path></svg></span><span style=\"display:inline-block;padding:2px 3px\">Markus Spiske</span></a>");
				doFooter(out, session);
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		out.flush();
		out.close();
	}

	public static void doHeader(PrintWriter out, HttpSession session) {
		out.println("<html>");
		out.println("  <head>");
		out.println("    <title>IIS Sandbox - Pop</title>");
		out.println("    <link rel=\"stylesheet\" href=\"https://www.w3schools.com/w3css/4/w3.css\"/>");
		out.println("  </head>");
		out.println("  <body>");
		out.println("    <header class=\"w3-container w3-light-grey\">");
		out.println("      <div class=\"w3-bar w3-light-grey\">");
		out.println(
			"        <a href=\"home\" class=\"w3-bar-item w3-button w3-green\">IIS Sandbox</a>");
		OrgAccess orgAccess = (OrgAccess) session.getAttribute("orgAccess");
		if (orgAccess != null) {
			String link = "home?" + PARAM_SHOW + "=" + SHOW_FACILITIES;
			out.println("<a href=\"" + link + "\" class=\"w3-bar-item w3-button\">Facilities</a>");
		}
		out.println("        <a href=\"pop\" class=\"w3-bar-item w3-button\">Send Now</a>");
		out.println("        <a href=\"message\" class=\"w3-bar-item w3-button\">Messages</a>");
		out.println("        <a href=\"patient\" class=\"w3-bar-item w3-button\">Patients</a>");
		out.println("        <a href=\"location\" class=\"w3-bar-item w3-button\">Locations</a>");
		out.println("        <a href=\"subscription\" class=\"w3-bar-item w3-button\">Subscriptions</a>");
		out.println("        <a href=\"soap\" class=\"w3-bar-item w3-button\">CDC WSDL</a>");
		if (orgAccess != null) {
			out.println("<a class='w3-bar-item w3-button w3-right' href=\"message?" + MessageServlet.PARAM_ACTION + "="
				+ MessageServlet.ACTION_LOGOUT + "\">Logout</a>");
		}
		out.println("      </div>");
		out.println("    </header>");
		out.println("    <div class=\"w3-container\">");

	}

	public static void doFooter(PrintWriter out, HttpSession session) {
		out.println("  </div>");
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		OrgAccess orgAccess = (OrgAccess) session.getAttribute("orgAccess");
		if (orgAccess != null) {
			out.println("  <div class=\"w3-container\">");
			out.println("    <p><a href=\"message?" + MessageServlet.PARAM_ACTION + "="
				+ MessageServlet.ACTION_LOGOUT + "\">Logout</a></p>");
			out.println("  </div>");
		}

		out.println("  <div class=\"w3-container w3-green\">");
		out.println("    <p>IIS Sandbox v" + SoftwareVersion.VERSION + " - Current Time "
			+ sdf.format(System.currentTimeMillis()) + "</p>");
		out.println("  </div>");
		out.println("  </body>");
		out.println("</html>");
	}

}
