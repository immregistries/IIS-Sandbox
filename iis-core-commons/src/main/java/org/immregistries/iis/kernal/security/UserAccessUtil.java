package org.immregistries.iis.kernal.security;

import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.immregistries.iis.kernal.persisted.repository.UserAccessRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Service
public class UserAccessUtil implements InitializingBean {

    public static final String GITHUB_PREFIX = "github-";
    public static final String SESSION_USER_ACCESS = "userAccess";
    private static String BAD_PASSWORD = "badpassword";

    /**
     * Singleton to access the application context for the repositories
     */
	 private static UserAccessUtil instance;

	@Override
	public void afterPropertiesSet() throws Exception {
		instance = this;
	}

	public static UserAccessUtil get() {
		return instance;
	}

	@Autowired
	private UserAccessRepository userAccessRepository;
	@Autowired
	private RequestTenantUtil requestTenantUtil;


	public UserAccess authenticateUserAccessUsernamePassword(String username, String password) {
        if (username.startsWith(GITHUB_PREFIX) || StringUtils.isBlank(password)) {
            throw new AuthenticationException();
        }
        if (BAD_PASSWORD.equals(password)) {
            return null;
        }
		 UserAccess userAccess = null;

		Optional<UserAccess> optionalUserAccess = userAccessRepository.findByAccessName(username);
		 if (optionalUserAccess.isEmpty()) {
			 userAccess = registerUserAccessWithUsernamePassword(username, password);
		 } else {
            // if (BCrypt.checkpw(password, userAccessList.get(0).getAccessKey())) { TODO
            // after auth checks fix in fhir
			 if (password.equals(optionalUserAccess.get().getAccessKey())) { // TODO Change
				 userAccess = optionalUserAccess.get();
            } else {
                throw new AuthenticationException("password for user : " + username);
            }
        }
        SecurityContextHolder.getContext().setAuthentication(userAccess);
        return userAccess;
    }

	public UserAccess authenticateUserAccessOAuth(OAuth2User oAuth2User) {
        String username = GITHUB_PREFIX + oAuth2User.getAttribute("login");
        UserAccess userAccess = null;

		Optional<UserAccess> optionalUserAccess = userAccessRepository.findByAccessName(getUserAccess().getAccessName());
		 if (optionalUserAccess.isEmpty()) {
            /**
             * Registration
             */
            userAccess = registerUserAccessGithub(username);
		 } else {
			 if (StringUtils.isNotBlank(optionalUserAccess.get().getAccessKey())) {
                throw new AuthenticationException("OAuth login failure");
            }
			 userAccess = optionalUserAccess.get();
        }
        SecurityContextHolder.getContext().setAuthentication(userAccess);
        return userAccess;
    }

	private UserAccess registerUserAccessGithub(String username) {
        if (!username.startsWith(GITHUB_PREFIX)) {
            throw new AuthenticationException();
        }
        UserAccess userAccess = new UserAccess();
        userAccess.setAccessName(username);
        userAccess.setAccessKey("");
		return userAccessRepository.save(userAccess);
    }

	private UserAccess registerUserAccessWithUsernamePassword(String username, String password) {
        if (username.startsWith(GITHUB_PREFIX)) {
            throw new AuthenticationException();
        }
        UserAccess userAccess = new UserAccess();
        userAccess.setAccessName(username);
        // userAccess.setAccessKey(BCrypt.hashpw(password, BCrypt.gensalt(5))); TODO
        // after auth checks fix in fhir
        userAccess.setAccessKey(password);
		return userAccessRepository.save(userAccess);
    }

    /**
     * asynchroneously provides and registers UserAccess Object from SecurityContext
     * or HttpServletRequest for Code use in Subscription Context
     *
     * @return
     */
	 public UserAccess getUserAccess() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof UserAccess) {
            return (UserAccess) authentication;
        }
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
		 Tenant tenant = requestTenantUtil.extractTenant(request); // TODO test if commenting breaks anything, might be
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
