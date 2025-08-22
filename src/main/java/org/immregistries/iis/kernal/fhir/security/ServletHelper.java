package org.immregistries.iis.kernal.fhir.security;

import ca.uhn.fhir.jpa.entity.PartitionEntity;
import ca.uhn.fhir.jpa.partition.IPartitionLookupSvc;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.immregistries.iis.kernal.HibernateConfig;
import org.immregistries.iis.kernal.fhir.Application;
import org.immregistries.iis.kernal.fhir.interceptors.PartitionCreationInterceptor;
import org.immregistries.iis.kernal.model.persisted.Tenant;
import org.immregistries.iis.kernal.model.persisted.UserAccess;
import org.immregistries.iis.kernal.servlet.TenantController;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.immregistries.iis.kernal.servlet.TenantUrlFilter.TENANT_NAME_URL;

public final class ServletHelper {
	// TODO Complete
	public static final List<String> FORBIDDEN_NAMES = List.of("pop", "iis", "home", "patient", "vaccination", "fhir", "tenant", "facility", "tenant");
	private static final Logger logger = LoggerFactory.getLogger(ServletHelper.class);
	public static final String GITHUB_PREFIX = "github-";
	public static final String SESSION_REQUEST_TENANT = "tenant";
	public static final String SESSION_USER_ACCESS = "userAccess";
	private static String BAD_PASSWORD = "badpassword";

	private static SessionFactory factory;

	/**
	 * Initialises data Session Factory if needed
	 *
	 * @return Data Session for Mysql
	 */
	public static Session getDataSession() {
		if (factory == null) {
//			factory = HibernateConfig.configuration().buildSessionFactory();
			factory = HibernateConfig.sessionFactory();
		}
		return factory.openSession();
	}


	/**
	 * Adds tenant prefix to url if tenant is not null
	 *
	 * @param tenant
	 * @param url
	 * @return
	 */
	public static String tenantifyUrl(Tenant tenant, String url) {
		if (tenant == null || tenant.getOrgId() < 0) {
			return url;
		}
		String organizationName = tenant.getOrganizationName();
		return Application.IIS_PATH_BASE + tenantifyUrl(organizationName, url);
	}

	/**
	 * @param tenantName organisation name
	 * @param url
	 * @return
	 */
	public static @NotNull String tenantifyUrl(String tenantName, String url) {
		if (!StringUtils.startsWith(url, "/")) {
			url = "/" + url;
		}
		return TenantController.TENANT_BASE_PATH + "/" + tenantName + url;
	}


	public static Tenant authenticateTenantNoUsername(String password, String facilityName, Session dataSession, PartitionCreationInterceptor partitionCreationInterceptor) {
		Query query = dataSession.createQuery("from Tenant where organizationName = ?1", Tenant.class);
		query.setParameter(1, facilityName);
		Tenant tenant = (Tenant) query.getSingleResult();
		if (tenant == null) {
			throw new RuntimeException("Invalid tenantName");
		}
		UserAccess tenantUserAccess = tenant.getUserAccess();
		String username = tenantUserAccess.getAccessName();

		UserAccess userAccess = authenticateUserAccessUsernamePassword(username, password, dataSession);
		return authenticateTenant(userAccess, facilityName, dataSession, partitionCreationInterceptor);
	}

	public static Tenant authenticateTenant(String username, String password, String facilityName, Session dataSession, PartitionCreationInterceptor partitionCreationInterceptor) {
		UserAccess userAccess = authenticateUserAccessUsernamePassword(username,password,dataSession);
		return authenticateTenant(userAccess, facilityName, dataSession, partitionCreationInterceptor);
	}

	public static Tenant authenticateTenant(OAuth2User oAuth2User, String facilityName, Session dataSession, PartitionCreationInterceptor partitionCreationInterceptor) {
		/**
		 * First user authentication with OAUTH
		 */
		UserAccess userAccess = authenticateUserAccessOAuth(oAuth2User,dataSession);
		return authenticateTenant(userAccess, facilityName, dataSession, partitionCreationInterceptor);
	}

	public static Tenant authenticateTenant(UserAccess userAccess, String facilityName, Session dataSession, PartitionCreationInterceptor partitionCreationInterceptor) {
		/**
		 * Users starting with the prefix can create a user with the same name, any other use of prefix are rejected
		 */
		if (StringUtils.isBlank(facilityName)) {
			throw new AuthenticationException();
		}
		facilityName = URLEncoder.encode(facilityName, StandardCharsets.UTF_8);
		if (facilityName.startsWith(GITHUB_PREFIX)) { // TODO rethink
			if (!userAccess.getAccessName().startsWith(GITHUB_PREFIX)) {
				throw new AuthenticationException();
			} else if (!facilityName.equals(userAccess.getAccessName())) {
				throw new AuthenticationException();
			}
		}

		Tenant tenant = null;
		Query query = dataSession.createQuery("from Tenant where organizationName = ?1");
		query.setParameter(1, facilityName);

		List<Tenant> tenantList = query.getResultList();
		if (tenantList.size() > 0) {
			/**
			 * Important step verifying authorisation
			 */
			if (tenantList.get(0).getUserAccess().getUserAccessId() == userAccess.getUserAccessId()) {
				tenant = tenantList.get(0);
			}
		} else {
			tenant = registerTenant(facilityName, userAccess, dataSession);
			if (partitionCreationInterceptor != null) {
				partitionCreationInterceptor.getOrCreatePartitionId(tenant.getOrganizationName());
			}
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
		SecurityContextHolder.getContext().setAuthentication(userAccess);
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
		SecurityContextHolder.getContext().setAuthentication(userAccess);
		return userAccess;
	}

	private static List<UserAccess> queryUserAccessWithUsername(String username, Session dataSession) {
		String queryString = "from UserAccess where accessName = ?1";
		Query query = dataSession.createQuery(queryString);
		query.setParameter(1, username);

		return query.getResultList();
	}

	private static UserAccess registerUserAccessGithub(String username, Session dataSession) {
		if (!username.startsWith(GITHUB_PREFIX)) {
			throw new AuthenticationException();
		}
		UserAccess userAccess = new UserAccess();
		userAccess.setAccessName(username);
		userAccess.setAccessKey("");
		Transaction transaction = dataSession.beginTransaction();
		try {
			userAccess.setUserAccessId((Integer) dataSession.save(userAccess));
		} finally {
			transaction.commit();
		}

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
		try {
			userAccess.setUserAccessId((Integer) dataSession.save(userAccess));
		} finally {
			transaction.commit();
		}
		return userAccess;
	}

	private static Tenant registerTenant(String facilityName, UserAccess userAccess, Session dataSession) {
		Tenant tenant = new Tenant();
		if (FORBIDDEN_NAMES.contains(facilityName)) {
			throw new RuntimeException("Tenant name: " + facilityName + " is forbidden");
		}
		tenant.setOrganizationName(facilityName);
		tenant.setUserAccess(userAccess);
		Transaction transaction = dataSession.beginTransaction();
		dataSession.persist(tenant);
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
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		Tenant tenant = getTenant(request); // TODO test if commenting breaks anything, might be useless, or only used for subscription/ bulk
		if (tenant != null) {
			return tenant.getUserAccess();
		}
		/*
		 * Useful for special user like connectathon
		 */
		HttpSession session = request.getSession(false);
		if (session != null) {
			return (UserAccess) session.getAttribute(SESSION_USER_ACCESS);
		} else {
			return null;
		}
	}

	public static Tenant getTenant(String pathVariable, HttpServletRequest request, Session dataSession) {
		Tenant tenant = null;
		if (StringUtils.isBlank(pathVariable)) {
			tenant = getTenant(request);
		} else {
			UserAccess userAccess = null;
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (authentication instanceof UserAccess) {
				userAccess = (UserAccess) authentication;
			}
			tenant = authenticateTenant(userAccess, pathVariable, dataSession, null);
		}
//		if (tenant == null) {
//			throw new AuthenticationCredentialsNotFoundException("");
//		}
		request.setAttribute(SESSION_REQUEST_TENANT, tenant);
		return tenant;
	}

	public static Tenant getTenant(HttpServletRequest request, Session existingDataSession) {
		final Tenant tenant;
		Tenant requestTenant = (Tenant) request.getAttribute(SESSION_REQUEST_TENANT);
		String urlTenantName = (String) request.getAttribute(TENANT_NAME_URL);
		if (StringUtils.isBlank(urlTenantName)) {
			tenant = requestTenant;
		} else {
			if (requestTenant != null && StringUtils.equals(requestTenant.getOrganizationName(), urlTenantName)) {
				tenant = requestTenant;
			} else if (existingDataSession != null) {
				tenant = getTenant(urlTenantName, request, existingDataSession);
			} else try (Session dataSession = getDataSession()) {
				tenant = getTenant(urlTenantName, request, dataSession);
			}
		}
		return tenant;
	}

	public static Tenant getTenant(HttpServletRequest request) {
		return getTenant(request, null);
	}

	public static @NotNull Tenant getTenantRedirectIfNone(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		return getTenantRedirectIfNone(req, resp, null);
	}

	public static @NotNull Tenant getTenantRedirectIfNone(HttpServletRequest req, HttpServletResponse resp, Session existingDataSession) throws IOException {
		Tenant tenant = getTenant(req, existingDataSession);
		if (tenant == null) {
			if (ServletHelper.getUserAccess() != null) {
				resp.sendRedirect(Application.IIS_PATH_BASE + TenantController.TENANT_BASE_PATH);
			}
			throw new AuthenticationCredentialsNotFoundException("");
		}
		return tenant;
	}

	public static Tenant getTenant() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		return getTenant(request);
	}

	public static RequestDetails requestDetailsWithPartitionName(IPartitionLookupSvc partitionLookupSvc) {
		PartitionEntity partitionEntity = partitionLookupSvc.getPartitionByName(ServletHelper.getTenant().getOrganizationName());
		if (partitionEntity == null) {
//			return SystemRequestDetails.forAllPartitions();
			throw new RuntimeException("No partition found");
		}
		RequestDetails requestDetails = SystemRequestDetails.forRequestPartitionId(partitionEntity.toRequestPartitionId());
		requestDetails.setTenantId(ServletHelper.getTenant().getOrganizationName());
		return requestDetails;
	}



}
