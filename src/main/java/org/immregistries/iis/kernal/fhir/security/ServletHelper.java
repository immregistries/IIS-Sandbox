package org.immregistries.iis.kernal.fhir.security;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.immregistries.iis.kernal.model.Tenant;
import org.immregistries.iis.kernal.model.UserAccess;
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
	private static final Logger logger = LoggerFactory.getLogger(ServletHelper.class);
	public static final String GITHUB_PREFIX = "github-";
	public static final String SESSION_TENANT = "tenant";
	public static final String SESSION_USER_ACCESS = "userAccess";
	private static String BAD_PASSWORD = "badpassword";

	private static SessionFactory factory;

	public static Session getDataSession() {
		if (factory == null) {
			factory = new Configuration().configure().buildSessionFactory();
		}
		return factory.openSession();
	}


	public static Tenant authenticateTenant(String username, String password, String facilityName, Session dataSession) {
		/**
		 * First user authentication with USERNAME password
		 */
		UserAccess userAccess = authenticateUserAccessUsernamePassword(username,password,dataSession);
		return  authenticateTenant(userAccess,facilityName,dataSession);
	}

	public static Tenant authenticateTenant(OAuth2User oAuth2User, String facilityName, Session dataSession) {
		/**
		 * First user authentication with OAUTH
		 */
		UserAccess userAccess = authenticateUserAccessOAuth(oAuth2User,dataSession);
		return authenticateTenant(userAccess,facilityName,dataSession);
	}

	public static Tenant authenticateTenant(UserAccess userAccess, String facilityName, Session dataSession) {
		/**
		 * Users starting with the prefix can create a user with the same name, any other use of prefix are rejected
		 */
		if (StringUtils.isBlank(facilityName)) {
			throw new AuthenticationException();
		}
		if (facilityName.startsWith(GITHUB_PREFIX) ) {
			if (!userAccess.getAccessName().startsWith(GITHUB_PREFIX)) {
				throw new AuthenticationException();
			} else if (!facilityName.equals(userAccess.getAccessName())) {
				throw new AuthenticationException();
			}
		}

		Tenant tenant = null;
		Query query = dataSession.createQuery("from Tenant where organizationName = ?1");
		query.setParameter(1, facilityName);

		List<Tenant> tenantList = query.list();
		if (tenantList.size() > 0) {
			/**
			 * Important step verifying authorisation
			 */
			if (tenantList.get(0).getUserAccess().getUserAccessId() == userAccess.getUserAccessId()) {
				tenant = tenantList.get(0);
			}
		} else {
			tenant = registerTenant(facilityName, userAccess, dataSession);
		}
		return tenant;
	}



	public static UserAccess authenticateUserAccessUsernamePassword(String username, String password, Session dataSession) {
		if (username.startsWith(GITHUB_PREFIX) || StringUtils.isBlank(password)) {
			throw new AuthenticationException();
		}
		if (BAD_PASSWORD.equals(password)) {
			return null;
		}
		UserAccess userAccess = null;

		List<UserAccess> userAccessList = queryUserAccessWithUsername(username,dataSession);
		if (userAccessList.size() == 0) {
			/**
			 * Registration
			 */
			userAccess = registerUserAccessWithUsernamePassword(username, password, dataSession);
		} else if (userAccessList.size() == 1) {
//      if (BCrypt.checkpw(password, userAccessList.get(0).getAccessKey())) { TODO after auth checks fix in fhir
			if (password.equals(userAccessList.get(0).getAccessKey())) {
				userAccess = userAccessList.get(0);
			} else {
				throw new AuthenticationException("password for user : " + username);
			}
		} else {
			throw new AuthenticationException("password for user : " + username);
		}
		return userAccess;
	}

	public static UserAccess authenticateUserAccessOAuth(OAuth2User oAuth2User, Session dataSession) {
		String username = GITHUB_PREFIX + oAuth2User.getAttribute("login");
		UserAccess userAccess = null;

		List<UserAccess> userAccessList = queryUserAccessWithUsername(username,dataSession);
		if (userAccessList.size() == 0) {
			/**
			 * Registration
			 */
			userAccess = registerUserAccessGithub(username,dataSession);
		} else if (userAccessList.size() == 1) {
			if (StringUtils.isNotBlank(userAccessList.get(0).getAccessKey())) {
				throw new AuthenticationException("OAuth login failure");
			}
			userAccess = userAccessList.get(0);
		} else {
			throw new AuthenticationException("OAuth login failure");
		}
		return userAccess;
	}

	private static List<UserAccess> queryUserAccessWithUsername(String username, Session dataSession) {
		String queryString = "from UserAccess where accessName = ?0";
		Query query = dataSession.createQuery(queryString);
		query.setParameter(0, username);

		return query.list();
	}

	private static UserAccess registerUserAccessGithub(String username, Session dataSession) {
		if (!username.startsWith(GITHUB_PREFIX)) {
			throw new AuthenticationException();
		}
		UserAccess userAccess = new UserAccess();
		userAccess.setAccessName(username);
		userAccess.setAccessKey("");
		Transaction transaction = dataSession.beginTransaction();
		userAccess.setUserAccessId((Integer) dataSession.save(userAccess));
		transaction.commit();
		return userAccess;
	}
	private static UserAccess registerUserAccessWithUsernamePassword(String username, String password, Session dataSession) {
		if (username.startsWith(GITHUB_PREFIX)) {
			throw new AuthenticationException();
		}
		UserAccess userAccess = new UserAccess();
		userAccess.setAccessName(username);
//      userAccess.setAccessKey(BCrypt.hashpw(password, BCrypt.gensalt(5))); TODO after auth checks fix in fhir
		userAccess.setAccessKey(password);
		Transaction transaction = dataSession.beginTransaction();
		userAccess.setUserAccessId((Integer) dataSession.save(userAccess));
		transaction.commit();
		return userAccess;
	}

	private static Tenant registerTenant(String facilityName, UserAccess userAccess, Session dataSession) {
		Tenant tenant = new Tenant();
		tenant.setOrganizationName(facilityName);
		tenant.setUserAccess(userAccess);
		Transaction transaction = dataSession.beginTransaction();
		tenant.setOrgId((Integer) dataSession.save(tenant));
		transaction.commit();
		return tenant;
	}



	/**
	 * asynchroneously provides and registers UserAccess Object from SecurityContext
	 *
	 * @return
	 */
	public static UserAccess getUserAccess() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication instanceof UserAccess) {
			return (UserAccess) authentication;
		}
		Tenant tenant = getTenant();
		if (tenant != null) {
			return tenant.getUserAccess();
		}
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		HttpSession session = request.getSession(false);
		if (session != null) {
			return (UserAccess) session.getAttribute(SESSION_USER_ACCESS);
		} else {
			return null;
		}
	}

	public static Tenant getTenant(String pathVariable, Session dataSession, HttpServletRequest request) {
		Tenant tenant = null;
		if (StringUtils.isBlank(pathVariable)) {
			tenant = getTenant();
		} else {
			UserAccess userAccess = getUserAccess();
			tenant = authenticateTenant(userAccess, pathVariable, dataSession);
		}
//		if (tenant == null) {
//			throw new AuthenticationCredentialsNotFoundException("");
//		}
		request.setAttribute(SESSION_TENANT, tenant);
		return tenant;
	}
	public static Tenant getTenant() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		Tenant tenant = null;
		if (request.getAttribute(SESSION_TENANT) != null) {
			tenant = (Tenant) request.getAttribute(SESSION_TENANT);
		}
		if (tenant == null) {
			HttpSession session = request.getSession(false);
			if ( session != null) {
				tenant = (Tenant) session.getAttribute(SESSION_TENANT);
			}
		}
		return tenant;
	}

	public static RequestDetails requestDetailsWithPartitionName() {
		RequestDetails requestDetails = new SystemRequestDetails();
		requestDetails.setTenantId(ServletHelper.getTenant().getOrganizationName());
		return requestDetails;
	}



}
