package org.immregistries.iis.kernal.controllers.servlet.legacy;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.immregistries.iis.kernal.controllers.servlet.TenantController;
import org.immregistries.iis.kernal.logic.VXUDownloadGenerator;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.immregistries.iis.kernal.security.UserAccessUtil;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;

@SuppressWarnings("serial")
@RestController
@RequestMapping({"/VXUDownload", TenantController.TENANT_PATH + "/VXUDownload"})
public class VXUDownloadController extends VXUDownloadFormController {

	@PostMapping
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

	@GetMapping
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    HttpSession session = req.getSession(true);

    resp.setContentType("text/plain");
    PrintWriter out = new PrintWriter(resp.getOutputStream());
		UserAccess userAccess = UserAccessUtil.get().getUserAccess();
   if (userAccess == null) {
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
