package org.immregistries.iis.kernal.servlet;

import org.hibernate.Session;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;


import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;

@SuppressWarnings("serial")
public class VXUDownloadServlet extends VXUDownloadFormServlet {


  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    HttpSession session = req.getSession(true);

    resp.setContentType("text/plain");
    PrintWriter out = new PrintWriter(resp.getOutputStream());
    OrgAccess orgAccess = (OrgAccess) session.getAttribute("orgAccess");
   if (orgAccess == null) {
//      RequestDispatcher dispatcher = req.getRequestDispatcher("home");
//      dispatcher.forward(req, resp);
//      return;
		 throw new AuthenticationCredentialsNotFoundException("");
    }

    try {
      VXUDownloadGenerator generator = (VXUDownloadGenerator) session.getAttribute(CACHED_GENERATOR);
      if (generator.isFileReady()) {
        FileInputStream fileInputStream = new FileInputStream(generator.getFile());
        BufferedReader in = new BufferedReader(new InputStreamReader(fileInputStream));
        String line;
        while ((line = in.readLine()) != null) {
          out.print(line);
          out.print("\r");
        }
        in.close();
      }

    } catch (

    Exception e) {
      System.err.println("Unable to render page: " + e.getMessage());
      e.printStackTrace(System.err);
    }
    out.flush();
    out.close();
  }


}
