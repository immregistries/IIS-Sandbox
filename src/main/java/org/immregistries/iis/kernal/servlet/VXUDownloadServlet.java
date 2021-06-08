package org.immregistries.iis.kernal.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.immregistries.iis.kernal.logic.IncomingMessageHandler;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.VaccinationReported;

@SuppressWarnings("serial")
public class VXUDownloadServlet extends VXUDownloadFormServlet {


  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    HttpSession session = req.getSession(true);

    resp.setContentType("text/plain");
    PrintWriter out = new PrintWriter(resp.getOutputStream());
    Session dataSession = PopServlet.getDataSession();
    OrgAccess orgAccess = (OrgAccess) session.getAttribute("orgAccess");
    if (orgAccess == null) {
      RequestDispatcher dispatcher = req.getRequestDispatcher("home");
      dispatcher.forward(req, resp);
      return;
    }

    try {
      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
      String messageError = null;
      String dateStartString = req.getParameter(PARAM_DATE_START);
      String dateEndString = req.getParameter(PARAM_DATE_END);


      Date dateStart = null;
      Date dateEnd = null;
      if (dateStartString == null) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        dateStartString = sdf.format(calendar.getTime());
      } else {
        try {
          dateStart = sdf.parse(dateStartString);
        } catch (ParseException pe) {
          messageError = "Start date is unparsable";
        }
      }
      if (dateEndString == null) {
        dateEndString = sdf.format(new Date());
      } else {
        try {
          dateEnd = sdf.parse(dateEndString);
        } catch (ParseException pe) {
          messageError = "End date is unparsable";
        }
      }
      String cvxCodes = req.getParameter(PARAM_CVX_CODES);
      if (StringUtils.isEmpty(cvxCodes)) {
        cvxCodes = "*";
      }
      boolean includePhi =
          req.getParameter(PARAM_CVX_CODES) == null || req.getParameter(PARAM_INCLUDE_PHI) != null;

      if (messageError != null) {
        out.println("Error: " + messageError);
      } else if (dateStart != null && dateEnd != null) {

        boolean allVaccines = false;
        Set<String> cvxCodeSet = new HashSet<>();
        {
          String codes[] = cvxCodes.split("\\,");
          for (String c : codes) {
            c = c.trim();
            if (StringUtils.isNotEmpty(c)) {
              cvxCodeSet.add(c);
              if (c.equals("*"))
              {
                allVaccines = true;
              }
            }
          }
        }
        List<VaccinationReported> vaccinationReportedList;
        {
          Query query = dataSession.createQuery(
              "from VaccinationReported where reportedDate >= :dateStart and reportedDate <= :dateEnd "
                  + "and patientReported.orgReported = :orgReported");
          query.setParameter("dateStart", dateStart);
          query.setParameter("dateEnd", dateEnd);
          query.setParameter("orgReported", orgAccess.getOrg());
          vaccinationReportedList = query.list();
        }

        IncomingMessageHandler incomingMessageHandler = new IncomingMessageHandler(dataSession);
        
        for (VaccinationReported vaccinationReported : vaccinationReportedList) {
          if (allVaccines || cvxCodeSet.contains(vaccinationReported.getVaccineCvxCode())) {
            if (vaccinationReported.getCompletionStatus().equals("NA")) {
              // not reporting missed appointments anymore
              continue;
            }
            out.print(incomingMessageHandler.buildVxu(vaccinationReported, orgAccess));
          }
        }

      }

    } catch (

    Exception e) {
      System.err.println("Unable to render page: " + e.getMessage());
      e.printStackTrace(System.err);
    } finally {
      dataSession.close();
    }
    out.flush();
    out.close();
  }


}
