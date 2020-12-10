package org.immregistries.iis.kernal.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.immregistries.vaccination_deduplication.Immunization;
import org.immregistries.vaccination_deduplication.LinkedImmunization;
import org.immregistries.vaccination_deduplication.VaccinationDeduplication;
import org.immregistries.vaccination_deduplication.reference.ImmunizationSource;

public class VacDedupServlet extends HttpServlet {

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

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

  @Override
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
          int i = 0;
          while (req.getParameter(PARAM_CVX + i) != null) {
            if (req.getParameter(PARAM_DATE + i).equals("")
                || req.getParameter(PARAM_CVX + i).equals("")) {
              i++;
              continue;
            }
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
            i++;
          }
          if (immunizationList.size() > 1) {
            VaccinationDeduplication vaccinationDeduplication = new VaccinationDeduplication();

            if (algorithm.equals(ALGORITHM_DETERMINISTIC)) {
              immunizationListResults =
                  vaccinationDeduplication.deduplicateDeterministic(immunizationList);

            } else if (algorithm.equals(ALGORITHM_WEIGHTED)) {
              immunizationListResults =
                  vaccinationDeduplication.deduplicateWeighted(immunizationList);
            } else if (algorithm.equals(ALGORITHM_HYBRID)) {
              immunizationListResults =
                  vaccinationDeduplication.deduplicateHybrid(immunizationList);
            }
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
            for (Immunization immunization : li) {
              out.println("        <tr>");
              out.println("          <td>" + sdf.format(immunization.getDate()) + "</td>");
              out.println("          <td>" + immunization.getCVX() + "</td>");
              out.println("          <td>" + immunization.getMVX() + "</td>");
              out.println("          <td>" + immunization.getLotNumber() + "</td>");
              out.println("          <td>" + immunization.getOrganisationID() + "</td>");
              out.println("          <td>" + immunization.getSource() + "</td>");
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
