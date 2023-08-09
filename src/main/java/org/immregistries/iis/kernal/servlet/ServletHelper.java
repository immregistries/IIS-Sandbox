package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgMaster;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

public class ServletHelper {
	public static final String GITHUB_PREFIX = "github-";
	public static final String SESSION_ORGMASTER = "orgMaster";
	public static final String SESSION_ORGACCESS = "orgAccess";
	private static String BAD_PASSWORD = "badpassword";


	public static OrgMaster authenticateOrgMaster(String username, String password, String facilityName, Session dataSession) {
		OrgAccess orgAccess = authenticateOrgAccess(username,password,dataSession);
		OrgMaster orgMaster = null;
		Query query = dataSession.createQuery("from OrgMaster where organizationName = ?1");
		query.setParameter(1, facilityName);

		List<OrgMaster> orgMasterList = query.list();
		if (orgMasterList.size() > 0) {
			if (orgMasterList.get(0).getOrgAccess().equals(orgAccess)) {
				orgMaster = orgMasterList.get(0);
			}
		} else {
			orgMaster = registerOrgMaster(facilityName, orgAccess, dataSession);
		}
		return orgMaster;
	}

	public static OrgAccess authenticateOrgAccess(String username, String password, String facilityName, Session dataSession) {
		if (BAD_PASSWORD.equals(password)) {
			return null;
		}
		OrgMaster orgMaster = null;
		OrgAccess orgAccess = null;

		Query query = dataSession.createQuery("from OrgMaster where organizationName = ?1");
		query.setParameter(1, facilityName);

		List<OrgMaster> orgMasterList = query.list();
		if (orgMasterList.size() > 0) {
			orgMaster = orgMasterList.get(0);
			orgAccess = authenticateOrgAccess(username, password, dataSession);
		} else {
			orgAccess = registerOrgAccessWithUsernamePassword(username, password, dataSession);
			orgMaster = registerOrgMaster(facilityName, orgAccess, dataSession);
		}
		return orgAccess;
	}

	public static OrgAccess authenticateOrgAccess(String username, String password, Session dataSession) {
		if (BAD_PASSWORD.equals(password)) {
			return null;
		}
		OrgAccess orgAccess = null;
		String queryString = "from OrgAccess where accessName = ?0";
		Query query = dataSession.createQuery(queryString);
		query.setParameter(0, username);

		@SuppressWarnings("unchecked")
		List<OrgAccess> orgAccessList = query.list();
		if (orgAccessList.size() == 0) {
			// TODO register ?
			orgAccess = registerOrgAccessWithUsernamePassword(username, password, dataSession);
		} else if (orgAccessList.size() == 1) {
//      if (BCrypt.checkpw(password, orgAccessList.get(0).getAccessKey())) { TODO after auth checks fix in fhir
			if (password.equals(orgAccessList.get(0).getAccessKey())) {
				orgAccess = orgAccessList.get(0);
			} else {
				throw new AuthenticationException("password for user : " + username);
			}
		} else {
			throw new AuthenticationException("password for user : " + username);
		}
		return orgAccess;
	}

	/**
	 * asynchroneously provides and registers OrgAccess Object from SecurityContext
	 *
	 * @return
	 */
	public static OrgAccess getOrgAccess() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		OrgAccess orgAccess = null;
		if (authentication instanceof OrgAccess) {
			return (OrgAccess) authentication;
		}
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		HttpSession session = request.getSession(false);
		if (session != null) {
			orgAccess = (OrgAccess) session.getAttribute(SESSION_ORGACCESS);
			if (orgAccess == null && authentication instanceof OAuth2AuthenticationToken) {
				OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) authentication;
				OAuth2User oAuth2User = oAuth2AuthenticationToken.getPrincipal();
				orgAccess = ServletHelper.authenticateOrgAccess(
					GITHUB_PREFIX + oAuth2User.getAttribute("login"),
					oAuth2User.getName(),
					"github-" + oAuth2User.getAttribute("login"),
					PopServlet.getDataSession());
				session.setAttribute(SESSION_ORGACCESS, orgAccess);
			}
		}
		return orgAccess;
	}

	public static OrgMaster getOrgMaster() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		HttpSession session = request.getSession(false);
		return (OrgMaster) session.getAttribute(SESSION_ORGMASTER);
	}

	public static RequestDetails requestDetailsWithPartitionName() {
		RequestDetails requestDetails = new SystemRequestDetails();
		requestDetails.setTenantId(ServletHelper.getOrgMaster().getOrganizationName());
		return requestDetails;
	}

	private static OrgAccess registerOrgAccessWithUsernamePassword(String username, String password, Session dataSession) {
		if (username.startsWith(GITHUB_PREFIX)) {
			throw new AuthenticationException();
		}
		OrgAccess orgAccess = new OrgAccess();
		orgAccess.setAccessName(username);
//      orgAccess.setAccessKey(BCrypt.hashpw(password, BCrypt.gensalt(5))); TODO after auth checks fix in fhir
		orgAccess.setAccessKey(password);
		Transaction transaction = dataSession.beginTransaction();
		dataSession.save(orgAccess);
		transaction.commit();
		return orgAccess;
	}

	private static OrgMaster registerOrgMaster(String facilityName, OrgAccess orgAccess, Session dataSession) {
		if (facilityName.startsWith(GITHUB_PREFIX)) {
			throw new AuthenticationException();
		}
		OrgMaster orgMaster = new OrgMaster();
		orgMaster.setOrganizationName(facilityName);
		orgMaster.setOrgAccess(orgAccess);
		Transaction transaction = dataSession.beginTransaction();
		dataSession.save(orgMaster);
		transaction.commit();
		return orgMaster;
	}


}
