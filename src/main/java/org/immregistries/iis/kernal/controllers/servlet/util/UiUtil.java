package org.immregistries.iis.kernal.controllers.servlet.util;

import com.google.common.collect.ImmutableMap;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.Application;
import org.immregistries.iis.kernal.SoftwareVersion;
import org.immregistries.iis.kernal.controllers.servlet.PatientController;
import org.immregistries.iis.kernal.controllers.servlet.PopController;
import org.immregistries.iis.kernal.controllers.servlet.TenantController;
import org.immregistries.iis.kernal.controllers.servlet.shlink.ShLinkController;
import org.immregistries.iis.kernal.mapping.requesters.FhirRequesterUtil;
import org.immregistries.iis.kernal.model.enums.ProcessingFlavor;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.immregistries.iis.kernal.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.security.ServerSecurityConfig;
import org.immregistries.iis.kernal.security.UserAccessUtil;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.UUID;

import static org.immregistries.iis.kernal.Application.IIS_PATH_BASE;

public class UiUtil {
	private final static ImmutableMap<String, String> HEADER_MAP = ImmutableMap.of(PopController.POP_PATH_KEY,
		"Send Now",
		"message", "Messages",
		PatientController.PATIENT_PATH_KEY, "Patients",
		"location", "Locations",
		ShLinkController.SHLINK_CONTROLLER_PATH_KEY, "ShLink"
		// ,FhirMessagingController.FHIR_MESSAGING, "Conversion messaging"
	);

	public static void doHeader(PrintWriter out, String title) {
		doHeader(out, title, CurrentTenantUtil.getTenant());
	}

	/**
	 * Helping method for unified Header printing in UI
	 *
	 * @param out    PrintWriter
	 * @param title  Page title for tab header
	 * @param tenant Currently usedO Tenant or null
	 */
	public static void doHeader(PrintWriter out, String title, Tenant tenant) {
		out.println("<html>");
		out.println("  <head>");
		out.println("    <title>" + title + "</title>");
		out.println("	  <link rel=\"icon\" type=\"image/x-icon\" href=\"" + IIS_PATH_BASE + "/img/favicon.ico\">");
		out.println("    <link rel=\"stylesheet\" href=\"https://www.w3schools.com/w3css/4/w3.css\"/>");
		out.println("  </head>");
		out.println("  <body>");
		out.println("    <header class=\"w3-container w3-light-grey\">");
		out.println("      <div class=\"w3-bar w3-light-grey\">");
		out.println("<a href=\"home\" class=\"w3-bar-item w3-button w3-green\">IIS Sandbox</a>");
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		for (Map.Entry<String, String> header : HEADER_MAP.entrySet()) {
			out.println("<a href=\"" + UrlTenantUtil.tenantifyPathWithContextPath(tenant, header.getKey())
				+ "\" class=\"w3-bar-item w3-button\">" + header.getValue() + "</a>");
		}
		// out.println("<a href=\"subscription\" class=\"w3-bar-item
		// w3-button\">Subscriptions</a>");
		out.println("<a href=\"" + UrlTenantUtil.tenantifyPathWithContextPath(tenant, "soap")
			+ "\" class=\"w3-bar-item w3-button\">CDC WSDL</a>");
		if (authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
			out.println("<a class='w3-bar-item w3-button w3-right' href=\"" + IIS_PATH_BASE
				+ ServerSecurityConfig.LOGOUT_PATH + "\">Logout</a>");
			String link = "tenant";
			if (tenant != null) {
				out.println("<a class='w3-bar-item w3-button w3-right w3-green' href=\"" + link + "\">Tenant : "
					+ tenant.getOrganizationName() + " </a>");
				out.println("<a href=\"" + Application.fhirServerBasePath(tenant)
					+ "/metadata\" class=\"w3-bar-item w3-button w3-right \">Tenant Fhir Server Base</a>");
			} else {
				out.println("<a class='w3-bar-item w3-button w3-right w3-green' href=\"" + link
					+ "\">No Tenant selected</a>");
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
		UserAccess userAccess = UserAccessUtil.get().getUserAccess();
		if (userAccess != null) {
			out.println("  <div class=\"w3-container\">");
			out.println("    <p><a href=\"" + IIS_PATH_BASE + "/logout\">Logout</a></p>");
			out.println("  </div>");
		}

		out.println("  <div class=\"w3-container w3-green\">");
		out.println("    <p>IIS Sandbox v" + SoftwareVersion.VERSION + " - Current Time "
			+ sdf.format(System.currentTimeMillis()) + "</p>");
		out.println(
			"    <a href=\"https://aira.memberclicks.net/assets/docs/Organizational_Docs/AIRA%20Privacy%20Policy%20-%20Final%202024_.pdf\" class=\"underline\">AIRA Privacy Policy</a> - ");
		out.println(
			"    <a href=\"https://aira.memberclicks.net/assets/docs/Organizational_Docs/AIRA%20Terms%20of%20Use%20-%20Final%202024_.pdf\" class=\"underline\">AIRA Terms and Conditions of Use</a></p>");
		out.println("  </div>");
		out.println("  </body>");
		out.println("</html>");
	}

	public static void printFlavors(PrintWriter out, boolean allowCreateShortcut) {
		out.println("    <h2>Processing Flavors</h2>");
		out.println(
			"    <p>If any of the following words appear in the name of the tenant then special processing rules will apply. "
				+
				"These processing rules can be used to simulate specific IIS behavior. </p>");
		out.println("    <ul class=\"w3-ul w3-hoverable\">");
		for (ProcessingFlavor processingFlavor : ProcessingFlavor.values()) {
			out.println("      <li>");
			if (allowCreateShortcut) {
				String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
				String link = IIS_PATH_BASE + TenantController.TENANT_BASE_PATH + "/" + processingFlavor.getKey() + "_"
					+ randomSuffix + TenantController.TENANT_BASE_PATH;
				out.print("<a href=\"" + link + "\">");
				out.print(processingFlavor.getKey());
				out.print("</a>");
			} else {
				out.print(processingFlavor.getKey());
			}
			out.print(": " + processingFlavor.getBehaviorDescription() + "</li>");
		}
		out.println("    </ul>");
	}

	public static void printGoldenRecordExplanation(PrintWriter out, IAnyResource iBaseResource) {
		printGoldenRecordExplanation(out, FhirRequesterUtil.isGoldenRecord(iBaseResource));
	}

	public static void printGoldenRecordExplanation(PrintWriter out, boolean isGolden) {
		String color;
		String message;
		if (isGolden) {
			color = "yellow";
			message = "Consolidated (Golden) record, As part of the Master Data Management (MDM), this record was generated aggregating the information across records identified as potential duplicates";
		} else {
			color = "blue";
			message = "Reported (Non-golden) record, As part of the Master Data Management (MDM), " +
				"This record represents the information as it was first received, before a merging process, " +
				"and is kept separated from the consolidated record for preserving history and later potential merging";
		}
		out.println("<div class=\"w3-panel w3-leftbar w3-border-" + color + " w3-pale-" + color
			+ "\"><p class=\"w3-left-align\">");
		out.println(message);
		out.println("</p></div>");
	}
}
