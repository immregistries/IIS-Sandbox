package org.immregistries.iis.kernal.controllers.servlet.legacy;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.immregistries.iis.kernal.controllers.servlet.TenantController;
import org.immregistries.iis.kernal.controllers.servlet.util.UiUtil;
import org.immregistries.iis.kernal.controllers.servlet.util.UrlTenantUtil;
import org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.logic.IExampleMessageWriter;
import org.immregistries.iis.kernal.logic.VXUDownloadGenerator;
import org.immregistries.iis.kernal.mapping.internalClient.FhirSearchRequester;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

@SuppressWarnings("serial")
/**
 * Broken and deprecated
 */
@RestController
@RequestMapping({ "/VXUDownloadForm", TenantController.TENANT_PATH + "/VXUDownloadForm" })
public class VXUDownloadFormController {

	@Autowired
	FhirSearchRequester fhirSearchRequester;
	@Autowired
	IExampleMessageWriter exampleMessageWriter;

  protected static final String CACHED_GENERATOR = "generator";
  protected static final String EXPORT_YYYY_MM_DD = "yyyy-MM-dd";

  public static final String ACTION_GENERATE = "Generate";
  public static final String ACTION_REFRESH = "Refresh";

  public static final String PARAM_ACTION = "action";

  @PostMapping
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

  @GetMapping
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    HttpSession session = req.getSession(true);

    resp.setContentType("text/html");
    PrintWriter out = new PrintWriter(resp.getOutputStream());
    Tenant tenant = CurrentTenantUtil.getTenant(req);
    if (tenant == null) {
      throw new AuthenticationCredentialsNotFoundException("");
    }

    try {
      String action = req.getParameter(PARAM_ACTION);
      if (action == null) {
        action = ACTION_REFRESH;
      }
      VXUDownloadGenerator generator = (VXUDownloadGenerator) session.getAttribute(CACHED_GENERATOR);
      if (generator == null || action == null || action.equals(ACTION_GENERATE)) {
			generator = new VXUDownloadGenerator(req, tenant, fhirSearchRequester, exampleMessageWriter);
        session.setAttribute(CACHED_GENERATOR, generator);
      }
      UiUtil.doHeader(out, "IIS Sandbox", tenant);

      if (action.equals(ACTION_GENERATE) && generator.canGenerate()) {
        generator.start();
        out.println("  <div class=\"w3-panel w3-yellow\">");
        out.println("    <p>Generator started</p>");
        out.println("  </div>");
      } else if (generator.hasMessageError()) {
        out.println("  <div class=\"w3-panel w3-red\">");
        out.println("    <p>" + generator.getMessageError() + "</p>");
        out.println("  </div>");
      }
      out.println("    <div class=\"w3-container w3-card-4\">");
      out.println("    <h2>Download COVID-19 HL7 for CDC Reporting</h2>");
      out.println(
          "    <form method=\"POST\" action=\"VXUDownloadForm\" class=\"w3-container w3-card-4\">");
      out.println("          <label>Start Date</label>");
      out.println("          <input class=\"w3-input\" type=\"text\" name=\""
          + VXUDownloadGenerator.PARAM_DATE_START + "\" value=\"" + generator.getDateStartString()
          + "\"/>");
      out.println("          <label>End Date</label>");
      out.println("          <input class=\"w3-input\" type=\"text\" name=\""
          + VXUDownloadGenerator.PARAM_DATE_END + "\" value=\"" + generator.getDateEndString()
          + "\"/>");
      out.println("          <label>End Date</label>");
      out.println("          <label>CVX Codes to Include</label>");
      out.println("          <input class=\"w3-input\" type=\"text\" name=\""
          + VXUDownloadGenerator.PARAM_CVX_CODES + "\" value=\"" + generator.getCvxCodes()
          + "\"/>");
      out.println("          <label>Include PHI</label>");
      out.println("          <input class=\"w3-input\" type=\"checkbox\" name=\""
          + VXUDownloadGenerator.PARAM_INCLUDE_PHI + "\" value=\"Y\""
          + (generator.isIncludePhi() ? " checked" : "") + "/>");
      if (generator.isRunning() || action.equals(ACTION_GENERATE)) {
        out.println(
            "          <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\""
                + PARAM_ACTION + "\" value=\"" + ACTION_REFRESH + "\"/>");
      } else {
        out.println(
            "          <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\""
                + PARAM_ACTION + "\" value=\"" + ACTION_GENERATE + "\"/>");
      }
      out.println("    </form>");

      out.println("<h2>Generator Status</h2>");
      out.println("<p>" + generator.getRunningMessage() + "</p>");
      if (generator.isFileReady()) {
        String link = "VXUDownload";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        out.println("<a href=\"" + UrlTenantUtil.tenantifyPathWithContextPath(tenant, link) + "\" download=\"export"
            + sdf.format(generator.getDateEnd()) + ".vxu.txt\">Download</a>");
      }
      out.println("    </div>");

    } catch (Exception e) {
      System.err.println("Unable to render page: " + e.getMessage());
      e.printStackTrace(System.err);
    }
    UiUtil.doFooter(out);
    out.flush();
    out.close();
  }

}
