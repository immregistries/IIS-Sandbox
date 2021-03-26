package org.immregistries.iis.kernal.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgMaster;
import org.immregistries.smm.tester.manager.query.QueryConverter;
import org.immregistries.smm.tester.manager.query.QueryType;
import org.immregistries.smm.transform.ScenarioManager;
import org.immregistries.smm.transform.TestCaseMessage;
import org.immregistries.smm.transform.Transformer;

@SuppressWarnings("serial")
public class QueryConverterServlet extends HttpServlet {

  public static final String PARAM_MESSAGE = "MESSAGEDATA";
  public static final String QUERY_TYPE = "queryType";

  private static SessionFactory factory;

  public static Session getDataSession() {
    if (factory == null) {
      factory = new AnnotationConfiguration().configure().buildSessionFactory();
    }
    return factory.openSession();
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    HttpSession session = req.getSession(true);
    resp.setContentType("text/html");
    PrintWriter out = new PrintWriter(resp.getOutputStream());
    try {
      String message = req.getParameter(PARAM_MESSAGE);
      QueryConverter queryConverter = null;
      if (req.getParameter(QUERY_TYPE) != null) {
        queryConverter =
            QueryConverter.getQueryConverter(QueryType.getValue(req.getParameter(QUERY_TYPE)));
      }
      if (message == null || message.equals("")) {
        TestCaseMessage testCaseMessage =
            ScenarioManager.createTestCaseMessage(ScenarioManager.SCENARIO_1_R_ADMIN_CHILD);
        Transformer transformer = new Transformer();
        transformer.transform(testCaseMessage);
        message = testCaseMessage.getMessageText();
      }
      if (queryConverter != null) {
        message = queryConverter.convert(message);
      }
      {
        HomeServlet.doHeader(out, session);
        out.println("    <h2>Convert VXU to QBP</h2>");
        out.println("    <form action=\"queryConverter\" method=\"POST\">");
        if (queryConverter == null) {
          out.println("      <h3>Update (VXU) Message</h3>");
        } else {
          out.println("      <h3>Query (QBP) Message</h3>");
        }
        out.println("      <textarea class=\"w3-input\" name=\"" + PARAM_MESSAGE
            + "\" rows=\"15\" cols=\"160\">" + message + "</textarea></td>");
        out.println("    <div class=\"w3-container w3-half w3-margin-top\">");

        if (queryConverter == null) {
          out.println("    <div class=\"w3-container w3-card-4\">");
          out.println(
              "      <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\""
                  + QUERY_TYPE + "\" value=\"" + QueryType.QBP_Z34 + "\"/>");
          out.println(
              "      <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\""
                  + QUERY_TYPE + "\" value=\"" + QueryType.QBP_Z44 + "\"/>");
          out.println("    </div>");
        }
        out.println("    </div>");
        out.println("    </form>");
        HomeServlet.doFooter(out, session);
      }
    } catch (Exception e) {
      e.printStackTrace(System.err);
    }
    out.flush();
    out.close();
  }

}
