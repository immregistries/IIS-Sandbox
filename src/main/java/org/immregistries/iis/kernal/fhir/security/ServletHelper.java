package org.immregistries.iis.kernal.fhir.security;

import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgMaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

public class ServletHelper {
	static Logger logger = LoggerFactory.getLogger(ServletHelper.class);
	public static final String GITHUB_PREFIX = "github-";
	public static final String SESSION_ORGMASTER = "orgMaster";
	public static final String SESSION_ORGACCESS = "orgAccess";
	private static String BAD_PASSWORD = "badpassword";


	public static OrgMaster authenticateOrgMaster(String username, String password, String facilityName, Session dataSession) {
		/**
		 * First user authentication with USERNAME password
		 */
		OrgAccess orgAccess = authenticateOrgAccessUsernamePassword(username,password,dataSession);
		return  authenticateOrgMaster(orgAccess,facilityName,dataSession);
	}

	public static OrgMaster authenticateOrgMaster(OAuth2User oAuth2User, String facilityName, Session dataSession) {
		/**
		 * First user authentication with OAUTH
		 */
		OrgAccess orgAccess = authenticateOrgAccessOAuth(oAuth2User,dataSession);
		return authenticateOrgMaster(orgAccess,facilityName,dataSession);
	}
	public static OrgMaster authenticateOrgMaster(OrgAccess orgAccess, String facilityName, Session dataSession) {
		/**
		 * Users starting with the prefix can create a user with the same name, any other use of prefix are rejected
		 */
		if (facilityName.startsWith(GITHUB_PREFIX) ) {
			if (!orgAccess.getAccessName().startsWith(GITHUB_PREFIX)) {
				throw new AuthenticationException();
			} else if (!facilityName.equals(orgAccess.getAccessName())) {
				throw new AuthenticationException();
			}
		}

		OrgMaster orgMaster = null;
		Query query = dataSession.createQuery("from OrgMaster where organizationName = ?1");
		query.setParameter(1, facilityName);

		logger.info("OrgAccess {}\n\n\n{}", orgAccess,orgAccess);
		List<OrgMaster> orgMasterList = query.list();
		if (orgMasterList.size() > 0) {
			/**
			 * Important step verifying authorisation
			 */
			if (orgMasterList.get(0).getOrgAccess().getOrgAccessId() == orgAccess.getOrgAccessId()) {
				orgMaster = orgMasterList.get(0);
			}
		} else {
			orgMaster = registerOrgMaster(facilityName, orgAccess, dataSession);
		}
		return orgMaster;
	}



	public static OrgAccess authenticateOrgAccessUsernamePassword(String username, String password, Session dataSession) {
		if (username.startsWith(GITHUB_PREFIX) || StringUtils.isBlank(password)) {
			throw new AuthenticationException();
		}
		if (BAD_PASSWORD.equals(password)) {
			return null;
		}
		OrgAccess orgAccess = null;

		List<OrgAccess> orgAccessList = queryOrgAccessWithUsername(username,dataSession);
		if (orgAccessList.size() == 0) {
			/**
			 * Registration
			 */
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

	public static OrgAccess authenticateOrgAccessOAuth(OAuth2User oAuth2User, Session dataSession) {
		String username = GITHUB_PREFIX + oAuth2User.getAttribute("login");
		OrgAccess orgAccess = null;

		List<OrgAccess> orgAccessList = queryOrgAccessWithUsername(username,dataSession);
		if (orgAccessList.size() == 0) {
			/**
			 * Registration
			 */
			orgAccess = registerOrgAccessGithub(username,dataSession);
		} else if (orgAccessList.size() == 1) {
			if (StringUtils.isNotBlank(orgAccessList.get(0).getAccessKey())) {
				throw new AuthenticationException("OAuth login failure");
			}
			orgAccess = orgAccessList.get(0);
		} else {
			throw new AuthenticationException("OAuth login failure");
		}
		return orgAccess;
	}

	private static List<OrgAccess> queryOrgAccessWithUsername(String username, Session dataSession) {
		String queryString = "from OrgAccess where accessName = ?0";
		Query query = dataSession.createQuery(queryString);
		query.setParameter(0, username);

		return query.list();
	}

	private static OrgAccess registerOrgAccessGithub(String username, Session dataSession) {
		if (!username.startsWith(GITHUB_PREFIX)) {
			throw new AuthenticationException();
		}
		OrgAccess orgAccess = new OrgAccess();
		orgAccess.setAccessName(username);
		orgAccess.setAccessKey("");
		Transaction transaction = dataSession.beginTransaction();
		orgAccess.setOrgAccessId((Integer) dataSession.save(orgAccess));
		transaction.commit();
		return orgAccess;
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
		orgAccess.setOrgAccessId((Integer) dataSession.save(orgAccess));
		transaction.commit();
		return orgAccess;
	}

	private static OrgMaster registerOrgMaster(String facilityName, OrgAccess orgAccess, Session dataSession) {
		OrgMaster orgMaster = new OrgMaster();
		orgMaster.setOrganizationName(facilityName);
		orgMaster.setOrgAccess(orgAccess);
		Transaction transaction = dataSession.beginTransaction();
		orgMaster.setOrgId((Integer) dataSession.save(orgMaster));
		transaction.commit();
		return orgMaster;
	}



	/**
	 * asynchroneously provides and registers OrgAccess Object from SecurityContext
	 *
	 * @return
	 */
	public static OrgAccess getOrgAccess() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication instanceof OrgAccess) {
			return (OrgAccess) authentication;
		}
		OrgMaster orgMaster = getOrgMaster();
		if (orgMaster != null) {
			return orgMaster.getOrgAccess();
		}
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		HttpSession session = request.getSession(false);
		if (session != null) {
			return (OrgAccess) session.getAttribute(SESSION_ORGACCESS);
		} else {
			return null;
		}
	}

	public static OrgMaster getOrgMaster() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		OrgMaster orgMaster = null;
		if (request.getAttribute(SESSION_ORGMASTER) != null) {
			orgMaster = (OrgMaster) request.getAttribute(SESSION_ORGMASTER);
		}
		if (orgMaster == null) {
			HttpSession session = request.getSession(false);
			if ( session != null) {
				orgMaster = (OrgMaster) session.getAttribute(SESSION_ORGMASTER);
			}
		}
		return  orgMaster;
	}

	public static RequestDetails requestDetailsWithPartitionName() {
		RequestDetails requestDetails = new SystemRequestDetails();
		requestDetails.setTenantId(ServletHelper.getOrgMaster().getOrganizationName());
		return requestDetails;
	}



}
