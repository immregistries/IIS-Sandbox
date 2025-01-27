package org.immregistries.iis.kernal.servlet.deprecated;

import org.immregistries.iis.kernal.servlet.PopServlet;

//@RestController
//@RequestMapping("/event")
public class EventServlet extends PopServlet {
//	@Autowired
//	IncomingEventHandler incomingEventHandler;
//  private static SessionFactory factory;
//
//  @PostMapping
//  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
//      throws ServletException, IOException {
//    resp.setContentType("text/html");
//    PrintWriter out = new PrintWriter(resp.getOutputStream());
//    try {
//      String userId = req.getParameter(PARAM_USERID);
//      String password = req.getParameter(PARAM_PASSWORD);
//      String facilityId = req.getParameter(PARAM_TENANT_NAME);
//      HttpSession session = req.getSession(true);
//      Tenant tenant = ServletHelper.getTenant();
//      String ack = "";
//      Session dataSession = getDataSession();
//      try {
//        if (tenant == null) {
//          tenant = ServletHelper.authenticateTenant(userId, password, facilityId, dataSession);
//        }
//        if (tenant == null) {
//          resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//          out.println(
//              "Access is not authorized. Facilityid, userid and/or password are not recognized. ");
//        } else {
//          ack = incomingEventHandler.process(req, tenant);
//          session.setAttribute(SESSION_TENANT, tenant);
//        }
//      } finally {
//        dataSession.close();
//      }
//      resp.setContentType("text/plain");
//      out.print(ack);
//    } catch (Exception e) {
//      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//      e.printStackTrace(out);
//      e.printStackTrace(System.err);
//    }
//    out.flush();
//    out.close();
//  }
//
//  @GetMapping
//  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
//      throws ServletException, IOException {
//
//
//    resp.setContentType("text/html");
//    PrintWriter out = new PrintWriter(resp.getOutputStream());
//    try {
//
//      String userId = req.getParameter(PARAM_USERID);
//      if (userId == null || userId.equals("")) {
//        userId = "Mercy";
//      }
//      String password = req.getParameter(PARAM_PASSWORD);
//      if (password == null || password.equals("")) {
//        password = "password1234";
//      }
//      String facilityId = req.getParameter(PARAM_TENANT_NAME);
//      if (facilityId == null || facilityId.equals("")) {
//        facilityId = "Mercy Healthcare";
//      }
//      {
//        HomeServlet.doHeader(out, "IIS Sandbox");
//			out.println("    <h2>Send Now</h2>");
//        out.println("    <form action=\"event\" method=\"POST\" target=\"_blank\">");
//        out.println("    <div class=\"w3-container w3-half w3-margin-top\">");
//        out.println("    <div class=\"w3-container w3-card-4\">");
//        out.println("      <h3>Authentication</h3>");
//        UserAccess userAccess = ServletHelper.getUserAccess();
//        if (userAccess == null) {
//          out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_USERID
//              + "\" value=\"" + userId + "\"/>");
//          out.println("      <label>User Id</label>");
//          out.println("      <input class=\"w3-input\" type=\"password\" name=\"" + PARAM_PASSWORD
//              + "\"/>");
//          out.println("      <label>Password</label>");
//          out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_TENANT_NAME
//              + "\" value=\"" + facilityId + "\"/>");
//          out.println("      <label>Facility Id</label>");
//        } else {
//          out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_USERID
//              + "\" value=\"" + userId + "\"/ disabled>");
//          out.println("      <label>User Id</label>");
//          out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_TENANT_NAME
//              + "\" value=\"" + facilityId + "\" disabled/>");
//          out.println("      <label>Facility Id</label>");
//        }
//        out.println("      <h3>Patient</h3>");
//        for (String s : IncomingEventHandler.PARAMS_PATIENT) {
//          out.println("      <label>" + s + "</label>");
//          out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + s + "\"/>");
//        }
//        out.println("");
//        out.println("      <h3>Vaccination</h3>");
//        for (String s : IncomingEventHandler.PARAMS_VACCINATION) {
//          out.println("      <label>" + s + "</label>");
//          out.println("      <input class=\"w3-input\" type=\"text\" name=\"" + s + "\"/>");
//        }
//        out.println("      <br/>");
//        out.println(
//            "      <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"sumbit\" value=\"Submit\"/>");
//        out.println("    </div>");
//        out.println("    </div>");
//        out.println("    </form>");
//        HomeServlet.doFooter(out);
//      }
//    } catch (Exception e) {
//      e.printStackTrace(System.err);
//    }
//    out.flush();
//    out.close();
//  }

}
