package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.SoftwareVersion;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.mapping.internalClient.FhirRequester;
import org.immregistries.iis.kernal.model.ProcessingFlavor;
import org.immregistries.iis.kernal.model.Tenant;
import org.immregistries.iis.kernal.model.UserAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

/**
 * UI Homepage
 */
public class HomeServlet extends HttpServlet {
	@Autowired
	FhirContext fhirContext;

	/**
	 * Helping method for unified Header printing in UI
	 *
	 * @param out   PrintWriter
	 * @param title Page title for tab header
	 */
	public static void doHeader(PrintWriter out, String title) {
		out.println("<html>");
		out.println("  <head>");
		out.println("    <title>" + title + "</title>");
		out.println("	  <link rel=\"icon\" type=\"image/x-icon\" href=\"img/favicon.ico\">");
		out.println("    <link rel=\"stylesheet\" href=\"https://www.w3schools.com/w3css/4/w3.css\"/>");
		out.println("  </head>");
		out.println("  <body>");
		out.println("    <header class=\"w3-container w3-light-grey\">");
		out.println("      <div class=\"w3-bar w3-light-grey\">");
		out.println("<a href=\"home\" class=\"w3-bar-item w3-button w3-green\">IIS Sandbox</a>");
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		out.println("<a href=\"pop\" class=\"w3-bar-item w3-button\">Send Now</a>");
		out.println("<a href=\"message\" class=\"w3-bar-item w3-button\">Messages</a>");
		out.println("<a href=\"patient\" class=\"w3-bar-item w3-button\">Patients</a>");
		out.println("<a href=\"location\" class=\"w3-bar-item w3-button\">Locations</a>");
		out.println("<a href=\"subscription\" class=\"w3-bar-item w3-button\">Subscriptions</a>");
		out.println("<a href=\"soap\" class=\"w3-bar-item w3-button\">CDC WSDL</a>");
		if (authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
			out.println("<a class='w3-bar-item w3-button w3-right' href=\"logout\">Logout</a>");
			Tenant tenant = ServletHelper.getTenant();
			if (tenant != null) {
				String link = "tenant";
				out.println("<a class='w3-bar-item w3-button w3-right w3-green' href=\"" + link + "\">Tenant : " + tenant.getOrganizationName() + " </a>");
				out.println("<a href=\"fhir/" + ServletHelper.getTenant().getOrganizationName() + "/metadata\" class=\"w3-bar-item w3-button w3-right \">Tenant Fhir Server Base</a>");
			} else {
				String link = "tenant";
				out.println("<a class='w3-bar-item w3-button w3-right w3-green' href=\"" + link + "\">No Tenant selected</a>");
			}
		} else {
			out.println("<a class='w3-bar-item w3-button w3-right' href=\"loginForm\">Login</a>");
		}

		out.println("      </div>");
		out.println("    </header>");
		out.println("    <div class=\"w3-container\">");
	}

	/**
	 * Helping method for unified Header printing in UI
	 *
	 * @param out PrintWriter
	 */
	public static void doFooter(PrintWriter out) {
		out.println("  </div>");
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		UserAccess userAccess = ServletHelper.getUserAccess();
		if (userAccess != null) {
			out.println("  <div class=\"w3-container\">");
			out.println("    <p><a href=\"logout\">Logout</a></p>");
			out.println("  </div>");
		}

		out.println("  <div class=\"w3-container w3-green\">");
		out.println("    <p>IIS Sandbox v" + SoftwareVersion.VERSION + " - Current Time " + sdf.format(System.currentTimeMillis()) + "</p>");
		out.println("    <a href=\"https://aira.memberclicks.net/assets/docs/Organizational_Docs/AIRA%20Privacy%20Policy%20-%20Final%202024_.pdf\" class=\"underline\">AIRA Privacy Policy</a> - ");
		out.println("    <a href=\"https://aira.memberclicks.net/assets/docs/Organizational_Docs/AIRA%20Terms%20of%20Use%20-%20Final%202024_.pdf\" class=\"underline\">AIRA Terms and Conditions of Use</a></p>");
		out.println("  </div>");
		out.println("  </body>");
		out.println("</html>");
	}

	public static void printFlavors(PrintWriter out) {
		out.println("    <h2>Processing Flavors</h2>");
		out.println("    <p>If any of the following words appear in the name of the tenant then special processing rules will apply. " +
			"These processing rules can be used to simulate specific IIS behavior. </p>");
		out.println("    <ul class=\"w3-ul w3-hoverable\">");
		for (ProcessingFlavor processingFlavor : ProcessingFlavor.values()) {
			out.println("      <li>" + processingFlavor.getKey() + ": " + processingFlavor.getBehaviorDescription() + "</li>");
		}
		out.println("    </ul>");
	}

	public static void printGoldenRecordExplanation(PrintWriter out, IBaseResource iBaseResource) {
		String color;
		String message;
		if (FhirRequester.isGoldenRecord(iBaseResource)) {
			color = "yellow";
			message = "Golden/Master record, As part of the Master Data Management (MDM), this record was generated aggregating the information across records identified as potential duplicates";
		} else {
			color = "blue";
			message = "Non-Golden/Reported record, As part of the Master Data Management (MDM), " +
				"This record represents the information as it was first received, before a merging process, " +
				"and is kept separated from the golden record for preserving history and later potential merging";
		}
		out.println("<div class=\"w3-panel w3-leftbar w3-border-" + color + " w3-pale-" + color + "\"><p class=\"w3-left-align\">");
		out.println(message);
		out.println("</p></div>");
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		try {
			doHeader(out, "IIS Sandbox - Home");
			out.println("    <div class=\"w3-container w3-half w3-margin-top\">");
			out.println("    <div class=\"w3-panel w3-yellow\"><p class=\"w3-left-align\">This system is for test purposes only. " +
				"Do not submit production data. As a precaution all submitted data will be deleted once a day.  </p></div>");
			if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
				out.println("    <div class=\"w3-panel w3-pale-blue\"><p class=\"w3-left-align\">" +
					"IIS Sandbox is deployed using a R5 FHIR Server, functionalities related to FHIR Subscriptions and Covid are enabled</p></div>");
			} else if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R4)) {
				out.println("    <div class=\"w3-panel w3-pale-green\"><p class=\"w3-left-align\">" +
					"IIS Sandbox is deployed using a R4 FHIR Server, functionalities related to FHIR Subscriptions and Covid are disabled</p></div>");
			}
			out.println("    <h2>Documentation</h2>");
			out.println("    <p class=\"w3-left-align\">Please see the project wiki: " +
				"<a href=\"https://github.com/immregistries/IIS-Sandbox/wiki\">https://github.com/immregistries/IIS-Sandbox/wiki</a></p>");
			out.println("    <h2>Primary Functions Supported</h2>");
			out.println("    <ul class=\"w3-ul w3-hoverable\">");
			out.println("      <li><a href=\"pop\">Send Now</a>: Send an HL7 message in now.</li>");
			out.println("      <li><a href=\"message\">Messages</a>: Review recently submitted messages</li>");
			out.println("      <li><a href=\"patient\">Patients</a>: See data received by patient</li>");
			out.println("      <li><a href=\"location\">Locations</a>: See administered-at-locations</li>");
			out.println("      <li><a href=\"recommendation\">Recommendations</a>: Generate Immunization Recommendations for Patients</li>");
			if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
				out.println("      <li><a href=\"subscription\">Subscriptions</a>: Visualize and manually trigger FHIR subscriptions</li>");
			} else if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R4)) {
				out.println("      <li><a>Subscriptions</a>: (Unavailable in R4 mode) Visualize and manually trigger FHIR subscriptions</li>");
			}
			out.println("      <li><a href=\"soap\">CDC WSDL</a>: HL7 realtime interfacing using CDC WSDL</li>");
			if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
				out.println("      <li><a href=\"fhir\">FHIR Server</a>: FHIR Restful server, partitioned for each tenants</li>");
			} else if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R4)) {
				out.println("      <li><a href=\"fhir\">FHIR Server</a>: FHIR Restful server, partitioned for each tenants</li>");
			}
			out.println("    </ul>");
			out.println("    <h3>Concepts and dependencies</h3>");
			out.println("    <ul class=\"w3-ul w3-hoverable\">");
			out.println("      <li><h4>HAPIFHIR Server Backend:</h4> This sandbox uses HAPIFHIR JPA Server framework as an end layer to store records, " +
				"using an experimental mapping layer to use inherited Hl7v2 based functionalities, current version of HAPIFHIR is a " +
				"<a href='https://github.com/cerbeor/hapi-fhir-Subscription-custom'>modded</a> 6.8.3</li>");
			out.println("      <li><h4>Multitenancy:</h4> Tenants allow separate testing environments, using different Flavors and different partitions of FHIR Server,	" +
				"Base URLs are formatted as <a href='fhir'>/iis/fhir/{tenantName}</a></li>");
			out.println("      <li><h4>Record's Matching:</h4>Matching resources using " +
				"<a href='https://github.com/immregistries/mismo-match'>MISMO</a> for Patients (Activated with a Flavor), " +
				"<a href='https://github.com/usnistgov/vaccination_deduplication'>vaccination_deduplication</a> for Immunizations</li>");
			out.println("      <li><h4>Master Data Management (MDM):</h4> Expanded from " +
				"<a href='https://hapifhir.io/hapi-fhir/docs/server_jpa_mdm/mdm.html'>HAPIFHIR's MDM</a>" +
				", Customization includes the above matching logic, a layer to add all the record's identifiers in Golden records, and R5 Support");
			out.println("      <li><h4>Golden/Master record:</h4>Golden records are generated by MDM system as the reference records to match with and merge information in");
			out.println("      <li><h4>Bulk data exchange server:</h4>Bulk data <a href='https://build.fhir.org/ig/HL7/bulk-data/export.html'>export</a> implemented, " +
				"alongside $member-add and $member-remove operations on Groups, SMART authentication support in progress (Currently disabled)");
			out.println("      <li><h4>Immunization Recommendation Forecast:</h4>" +
				"<a href='http://hl7.org/fhir/us/immds/STU1/OperationDefinition-ImmDSForecastOperation.html'>ImmDSForecast</a>" +
				" operations implemented, current result is randomly generated");
			out.println("    </ul>");
			out.println("    <h3>Secondary Functions Supported</h3>");
			out.println("    <ul class=\"w3-ul w3-hoverable\">");
			out.println("      <li><a href=\"lab\">Convert ORU to VXU</a>: Convert an ORU lab message to a VXU. </li>");
			out.println("      <li><a href=\"queryConverter\">Convert VXU to QBP</a>: Convert an VXU immunization message into an immunization query. </li>");
			if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
				out.println("      <li><a href=\"covid\">COVID-19 Reporting</a>: Export data to demonstrate COVID-19 reporting </li>");
			} else if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R4)) {
				out.println("      <li><a href=\"covid\">COVID-19 Reporting</a>:(Unavailable in R4 mode)  Export data to demonstrate COVID-19 reporting </li>");
			}
			out.println("      <li><a href=\"VXUDownloadForm\">COVID-19 Reporting (HL7)</a>: Download data in HL7 format demonstrate COVID-19 reporting </li>");
			out.println("      <li><a href=\"covidGenerate\">COVID-19 HL7 Generator</a>: Generate HL7 Messages</li>");
//			out.println("      <li><a href=\"event\">Submit Event</a>: Submit a patient and vaccination event manually.</li>");
//			out.println("      <li><a href=\"fhirTest\">FHIR Test Endpoint</a>: Create FHIR resources to test with IIS Sandbox.</li>");
			out.println("      <li><a href=\"vciDemo\">VCI Demonstration</a>: Demonstration of RSP conversion steps for the Vaccine Credential Initiative</li>");
			out.println("    </ul>");

			printFlavors(out);
			out.println("  </div>");
			out.println("  <img src=\"img/markus-spiske-dWaRJ3WBnGs-unsplash.jpg\" class=\"w3-round\" alt=\"Sandbox\" width=\"400\">");
			out.println("<a " +
				"style=\"background-color:black;color:white;text-decoration:none;padding:4px 6px;font-family:-apple-system, " +
				"BlinkMacSystemFont, &quot;San Francisco&quot;, &quot;Helvetica Neue&quot;, Helvetica, Ubuntu, Roboto, Noto," +
				" &quot;Segoe UI&quot;, Arial, sans-serif;font-size:12px;font-weight:bold;line-height:1.2;display:inline-block;border-radius:3px\" " +
				"href=\"https://unsplash.com/@markusspiske?utm_medium=referral&amp;utm_campaign=photographer-credit&amp;utm_content=creditBadge\" " +
				"target=\"_blank\" rel=\"noopener noreferrer\" title=\"Download free do whatever you want high-resolution photos from Markus Spiske\">" +
				"<span style=\"display:inline-block;padding:2px 3px\">" +
				"<svg xmlns=\"http://www.w3.org/2000/svg\" style=\"height:12px;width:auto;position:relative;vertical-align:middle;top:-2px;fill:white\" viewBox=\"0 0 32 32\">" +
				"<title>unsplash-logo</title>" +
				"<path d=\"M10 9V0h12v9H10zm12 5h10v18H0V14h10v9h12v-9z\"></path></svg></span><span style=\"display:inline-block;padding:2px 3px\">Markus Spiske</span></a>");
			doFooter(out);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		out.flush();
		out.close();
	}
}
