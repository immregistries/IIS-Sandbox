package org.immregistries.iis.kernal.fhir.security;

import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import jakarta.persistence.TypedQuery;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.persisted.model.UserAccess;
import org.immregistries.iis.kernal.persisted.repository.UserAccessRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

public class UserAccessUtil implements ApplicationContextAware {

    private static ApplicationContext ac;

    @Override
    public void setApplicationContext(ApplicationContext ac) {
        UserAccessUtil.ac = ac;
    }

    public static UserAccessRepository getUserAccessRepository() {
        return (UserAccessRepository) ac.getBean("userAccessRepository");

    }

    public static final String GITHUB_PREFIX = "github-";
    public static final String SESSION_USER_ACCESS = "userAccess";
    private static String BAD_PASSWORD = "badpassword";

    public static UserAccess authenticateUserAccessUsernamePassword(String username, String password,
            Session dataSession) {
        if (username.startsWith(GITHUB_PREFIX) || StringUtils.isBlank(password)) {
            throw new AuthenticationException();
        }
        if (BAD_PASSWORD.equals(password)) {
            return null;
        }
        UserAccess userAccess = null;

        List<UserAccess> userAccessList = queryUserAccessWithUsername(username, dataSession);
        if (userAccessList.size() == 0) {
            /**
             * Registration
             */
            userAccess = registerUserAccessWithUsernamePassword(username, password, dataSession);
        } else if (userAccessList.size() == 1) {
            // if (BCrypt.checkpw(password, userAccessList.get(0).getAccessKey())) { TODO
            // after auth checks fix in fhir
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

        List<UserAccess> userAccessList = queryUserAccessWithUsername(username, dataSession);
        if (userAccessList.size() == 0) {
            /**
             * Registration
             */
            userAccess = registerUserAccessGithub(username, dataSession);
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
        TypedQuery<UserAccess> query = dataSession.createQuery(queryString, UserAccess.class);
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

    private static UserAccess registerUserAccessWithUsernamePassword(String username, String password,
            Session dataSession) {
        if (username.startsWith(GITHUB_PREFIX)) {
            throw new AuthenticationException();
        }
        UserAccess userAccess = new UserAccess();
        userAccess.setAccessName(username);
        // userAccess.setAccessKey(BCrypt.hashpw(password, BCrypt.gensalt(5))); TODO
        // after auth checks fix in fhir
        userAccess.setAccessKey(password);
        Transaction transaction = dataSession.beginTransaction();
        try {
            userAccess.setUserAccessId((Integer) dataSession.save(userAccess));
        } finally {
            transaction.commit();
        }
        return userAccess;
    }

    /**
     * asynchroneously provides and registers UserAccess Object from SecurityContext
     * or HttpServletRequest for Code use in Subscription Context
     *
     * @return
     */
    public static UserAccess getUserAccess() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof UserAccess) {
            return (UserAccess) authentication;
        }
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        Tenant tenant = CurrentTenantUtil.getTenant(request); // TODO test if commenting breaks anything, might be
                                                              // useless, or
        // only used
        // for subscription/ bulk
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
}
