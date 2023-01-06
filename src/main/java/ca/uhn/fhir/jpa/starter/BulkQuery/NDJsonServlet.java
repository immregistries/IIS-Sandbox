package ca.uhn.fhir.jpa.starter.BulkQuery;

import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.starter.interceptors.SessionAuthorizationInterceptor;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import org.hibernate.Session;
import org.immregistries.iis.kernal.model.MessageReceived;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.servlet.PopServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class NDJsonServlet extends HttpServlet {
	private final Logger logger = LoggerFactory.getLogger(NDJsonServlet.class);

	@Autowired
	SessionAuthorizationInterceptor sessionAuthorizationInterceptor;

	@Autowired
	IFhirSystemDao fhirSystemDao;
	@Autowired
	BulkQueryGroupProviderR4 bulkQueryGroupProviderR4;


	//	@GetMapping
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
		String authHeader;
		Session dataSession = PopServlet.getDataSession();
		OrgAccess orgAccess = (OrgAccess) request.getSession(false).getAttribute("orgAccess");
		try {
			String tenantId = request.getParameter("tenantId");
			Integer ndJsonId = Integer.parseInt(request.getParameter("ndJsonId"));
			if (orgAccess == null) {
				if (request.getHeader("Authorization") == null || request.getHeader("Authorization").isEmpty()) {
					throw new AuthenticationException();
				} else {
					authHeader = request.getHeader("Authorization");
					orgAccess = sessionAuthorizationInterceptor.tryAuthHeader(authHeader, tenantId, dataSession);
					if (orgAccess == null) {
						throw new AuthenticationException();
					}
				}
			}
			MessageReceived ndJson = dataSession.get(MessageReceived.class, ndJsonId);
			if (ndJson.getOrgMaster().getOrgId() == orgAccess.getOrg().getOrgId()) { // check authorization
//			return ResponseEntity.ok(ndJson.getMessageResponse());
				resp.getOutputStream().print(ndJson.getMessageResponse());
			} else {
				throw new AuthenticationException();
			}
		} catch (Exception e) {
			throw e;
		} finally {
			dataSession.close();
		}

	}


}
