package org.immregistries.iis.kernal.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.immregistries.vaccination_deduplication.Immunization;
import org.immregistries.vaccination_deduplication.LinkedImmunization;

import org.immregistries.vaccination_deduplication.reference.ImmunizationSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.immregistries.iis.kernal.rest.VacDedupRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.persisted.model.Tenant;

@RestController
@RequestMapping({ "/vacDedup", TenantController.TENANT_PATH + "/vacDedup" })
public class VacDedupController {
  private static final long serialVersionUID = 1L;

  @Autowired
  private VacDedupRestController vacDedupRestController;

  public static final String PARAM_ACTION = "action";
  public static final String PARAM_CVX = "cvx";
  public static final String PARAM_DATE = "date";
  public static final String PARAM_SOURCE = "source";
  public static final String PARAM_ORG = "org=";
  public static final String PARAM_MVX = "mvx";
  public static final String PARAM_LOT = "lot";
  public static final String ACTION_SUBMIT = "Submit";
  public static final String PARAM_VIEW = "view";
  public static final String PARAM_ALGORITHM = "algorithm";
  public static final String ALGORITHM_DETERMINISTIC = "Deterministic";
  public static final String ALGORITHM_WEIGHTED = "Weighted";
  public static final String ALGORITHM_HYBRID = "Hybrid";

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
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    LinkedImmunization immunizationList = new LinkedImmunization();
    ArrayList<LinkedImmunization> immunizationListResults = null;
    try {
      String action = req.getParameter(PARAM_ACTION);
      String actionStatus = null;
      String algorithm = req.getParameter(PARAM_ALGORITHM);
      if (algorithm == null) {
        algorithm = ALGORITHM_DETERMINISTIC;
      }
      if (action != null) {
        if (action.equals(ACTION_SUBMIT)) {
          VacDedupRestController.VacDedupRequest request = new VacDedupRestController.VacDedupRequest();
          request.setAlgorithm(algorithm);
          ArrayList<VacDedupRestController.VacDedupRequest.ImmunizationItem> items = new ArrayList<>();

          int i = 0;
          while (req.getParameter(PARAM_CVX + i) != null) {
            if (req.getParameter(PARAM_DATE + i).equals("")
                || req.getParameter(PARAM_CVX + i).equals("")) {
              i++;
              continue;
            }
            // Add to list for display
            Date date = null;
            try {
              date = sdf.parse(req.getParameter(PARAM_DATE + i));
            } catch (ParseException pe) {
              actionStatus = "Unrecognized date format: '" + req.getParameter(PARAM_DATE)
                  + "' always use MM/DD/YYYY format";
            }
            String cvx = req.getParameter(PARAM_CVX + i);
            String mvx = req.getParameter(PARAM_MVX + i);
            String lot = req.getParameter(PARAM_LOT + i);
            String org = req.getParameter(PARAM_ORG + i);
            String source = req.getParameter(PARAM_SOURCE + i);
            ImmunizationSource immunizationSource;
            if (source.equals("")) {
              immunizationSource = ImmunizationSource.HISTORICAL;
            } else {
              immunizationSource = ImmunizationSource.valueOf(source);
            }
            Immunization immunization = new Immunization();
            immunization.setCVX(cvx);
            immunization.setDate(date);
            immunization.setMVX(mvx);
            immunization.setLotNumber(lot);
            immunization.setOrganisationID(org);
            immunization.setSource(immunizationSource);
            immunizationList.add(immunization);

            // Add to request for REST controller
            VacDedupRestController.VacDedupRequest.ImmunizationItem item = new VacDedupRestController.VacDedupRequest.ImmunizationItem();
            item.setDate(req.getParameter(PARAM_DATE + i));
            item.setCvx(cvx);
            item.setMvx(mvx);
            item.setLot(lot);
            item.setOrg(org);
            item.setSource(source);
            items.add(item);

            i++;
          }
          request.setImmunizations(items);

          if (immunizationList.size() > 1) {
            // Call REST controller
            Tenant tenant = CurrentTenantUtil.getTenant();
            List<LinkedImmunization> results = vacDedupRestController.deduplicate(tenant, request, req);
            immunizationListResults = new ArrayList<>(results);
          }
        }
      }
      {
        out.println("<html>");
        out.println("  <head>");
        out.println("    <title>Vacc Dedup Demo</title>");
        out.println("  </head>");
        out.println("  <body>");
        out.println("    <h1>Vaccination Deduplication Demo</h1>");
        if (actionStatus != null) {
          out.println("    <p style=\"color: red;\">" + actionStatus + "</p>");

        }

        out.println("    <form method=\"POST\" action=\"vacDedup\">");
        out.println("      <table border=\"0\" cellpadding=\"3\">");
        out.println("        <tr>");
        out.println("          <th>Date</th>");
        out.println("          <th>CVX</th>");
        out.println("          <th>MVX</th>");
        out.println("          <th>Lot Number</th>");
        out.println("          <th>Org</th>");
        out.println("          <th>Source</th>");
        out.println("        </tr>");
        int rowCount = immunizationList.size() + 4;
        for (int i = 0; i < rowCount; i++) {
          String dateString = "";
          String cvx = "";
          String mvx = "";
          String lot = "";
          String org = "";
          String source = "";
          if (immunizationList.size() > i) {
            Immunization immunization = immunizationList.get(i);
            dateString = sdf.format(immunization.getDate());
            cvx = immunization.getCVX();
            mvx = immunization.getMVX();
            lot = immunization.getLotNumber();
            org = immunization.getOrganisationID();
            source = immunization.getSource().toString();
          }
          out.println("        <tr>");
          out.println("          <td><input type=\"text\" name=\"" + PARAM_DATE + i + "\" value=\""
              + dateString + "\" size=\"10\"/></td>");
          out.println("          <td><input type=\"text\" name=\"" + PARAM_CVX + i + "\" value=\""
              + cvx + "\" size=\"3\"/></td>");
          out.println("          <td><input type=\"text\" name=\"" + PARAM_MVX + i + "\" value=\""
              + mvx + "\" size=\"3\"/></td>");
          out.println("          <td><input type=\"text\" name=\"" + PARAM_LOT + i + "\" value=\""
              + lot + "\" size=\"9\"/></td>");
          out.println("          <td><input type=\"text\" name=\"" + PARAM_ORG + i + "\" value=\""
              + org + "\" size=\"12\"/></td>");
          out.println("          <td>");
          printSource(out, i, source, ImmunizationSource.SOURCE);
          printSource(out, i, source, ImmunizationSource.HISTORICAL);
          out.println("          </td>");
          out.println("        </tr>");
        }
        out.println("      </table>");
        printAlgorithm(out, algorithm, ALGORITHM_DETERMINISTIC);
        printAlgorithm(out, algorithm, ALGORITHM_WEIGHTED);
        printAlgorithm(out, algorithm, ALGORITHM_HYBRID);
        out.println("      <input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\""
            + ACTION_SUBMIT + "\"/>");
        out.println("      <input type=\"hidden\" name=\"" + PARAM_VIEW + "\" value=\"user\"/>");
        out.println("    </form>");
        if (immunizationListResults != null) {
          out.println("    <h2>" + algorithm + " Results</h2>");
          int i = 0;
          for (LinkedImmunization li : immunizationListResults) {
            i++;
            out.println("    <h3>Immunization Set " + i + " " + li.getType() + "</h3>");
            out.println("      <table border=\"1\" cellpadding=\"3\" cellspacing=\"0\">");
            out.println("        <tr>");
            out.println("          <th>Date</th>");
            out.println("          <th>CVX</th>");
            out.println("          <th>MVX</th>");
            out.println("          <th>Lot Number</th>");
            out.println("          <th>Org</th>");
            out.println("          <th>Source</th>");
            out.println("        </tr>");
            for (Immunization imm : li) {
              out.println("        <tr>");
              out.println("          <td>" + sdf.format(imm.getDate()) + "</td>");
              out.println("          <td>" + imm.getCVX() + "</td>");
              out.println("          <td>" + imm.getMVX() + "</td>");
              out.println("          <td>" + imm.getLotNumber() + "</td>");
              out.println("          <td>" + imm.getOrganisationID() + "</td>");
              out.println("          <td>" + imm.getSource() + "</td>");
              out.println("        </tr>");
            }
            out.println("      </table>");
          }
        }
        out.println("  </body>");
        out.println("</html>");
      }
    } catch (Exception e) {
      e.printStackTrace(System.err);
    }
    out.flush();
    out.close();
  }

  private void printAlgorithm(PrintWriter out, String algorithm, String a) {
    out.println("<input type=\"radio\" name=\"" + PARAM_ALGORITHM + "\" value=\"" + a + "\""
        + (a.equals(algorithm) ? " checked=\"true\"" : "") + "/>" + a);
  }

  private void printSource(PrintWriter out, int i, String source, ImmunizationSource is) {
    out.println("            <input type=\"radio\" name=\"" + PARAM_SOURCE + i + "\" value=\"" + is
        + "\"" + (source.equals(is.toString()) ? " checked=\"true\"" : "") + "/> " + is.toString());
  }
}
