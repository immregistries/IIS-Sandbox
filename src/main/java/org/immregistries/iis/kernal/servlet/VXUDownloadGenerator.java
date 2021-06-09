package org.immregistries.iis.kernal.servlet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.immregistries.iis.kernal.logic.IncomingMessageHandler;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.VaccinationReported;

public class VXUDownloadGenerator extends Thread {

  public static final String PARAM_DATE_START = "dateStart";
  public static final String PARAM_DATE_END = "dateEnd";
  public static final String PARAM_CVX_CODES = "cvxCodes";
  public static final String PARAM_INCLUDE_PHI = "includePhi";


  private String messageError;

  public boolean hasMessageError() {
    return messageError != null;
  }

  public String getMessageError() {
    return messageError;
  }

  public String getDateStartString() {
    return dateStartString;
  }

  public String getDateEndString() {
    return dateEndString;
  }

  public Date getDateStart() {
    return dateStart;
  }

  public Date getDateEnd() {
    return dateEnd;
  }

  public String getCvxCodes() {
    return cvxCodes;
  }

  public boolean isIncludePhi() {
    return includePhi;
  }

  public boolean isRunning() {
    return running;
  }

  public String getRunningMessage() {
    return runningMessage;
  }

  public File getFile() {
    return file;
  }

  private String dateStartString;
  private String dateEndString;
  private Date dateStart;
  private Date dateEnd;
  private String cvxCodes;
  private boolean includePhi;
  private SimpleDateFormat sdf;
  private boolean running = false;
  private String runningMessage = "Not Started";
  private Session dataSession;
  private OrgAccess orgAccess;
  private File file;

  public VXUDownloadGenerator(HttpServletRequest req, int orgAccessId) {
    runningMessage = "Initializing";
    this.dataSession = PopServlet.getDataSession();
    this.orgAccess = (OrgAccess) dataSession.get(OrgAccess.class, orgAccessId);
    sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    messageError = null;
    dateStartString = req.getParameter(PARAM_DATE_START);
    dateEndString = req.getParameter(PARAM_DATE_END);
    dateStart = null;
    dateEnd = null;
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
    cvxCodes = req.getParameter(PARAM_CVX_CODES);
    if (StringUtils.isEmpty(cvxCodes)) {
      cvxCodes = CovidServlet.COVID_CVX_CODES;
    }
    includePhi =
        req.getParameter(PARAM_CVX_CODES) == null || req.getParameter(PARAM_INCLUDE_PHI) != null;
    runningMessage = "Initialized " + sdf.format(new Date());

  }

  public boolean canGenerate() {
    return dateStart != null && dateEnd != null;
  }

  public boolean isFileReady() {
    return running == false && file != null;
  }

  @Override
  public void run() {
    running = true;
    runningMessage = "Generating";
    boolean allVaccines = false;
    Set<String> cvxCodeSet = new HashSet<>();
    {
      String codes[] = cvxCodes.split("\\,");
      for (String c : codes) {
        c = c.trim();
        if (StringUtils.isNotEmpty(c)) {
          cvxCodeSet.add(c);
          if (c.equals("*")) {
            allVaccines = true;
          }
        }
      }
    }
    runningMessage = "Looking for vaccinations";
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
    Random random = new Random();
    String filename = "VXUDownload-";
    for (int i = 0; i < 40; i++) {
      filename += (char) ('A' + random.nextInt(26));
    }
    filename += ".vxu.txt";
    file = new File(filename);
    PrintWriter out;
    try {
      FileWriter fileWriter = new FileWriter(file);
      out = new PrintWriter(fileWriter);
      int count = 0;
      for (VaccinationReported vaccinationReported : vaccinationReportedList) {
        count++;
        runningMessage = "Generating " + count;
        if (allVaccines || cvxCodeSet.contains(vaccinationReported.getVaccineCvxCode())) {
          if (vaccinationReported.getCompletionStatus().equals("NA")) {
            // not reporting missed appointments anymore
            continue;
          }
          out.print(incomingMessageHandler.buildVxu(vaccinationReported, orgAccess));
        }
      }
      out.close();
    } catch (IOException e) {
      runningMessage = "Exception: " + e.getMessage();
      e.printStackTrace();
    }
    running = false;
    runningMessage = "Generating complete";
  }

}
