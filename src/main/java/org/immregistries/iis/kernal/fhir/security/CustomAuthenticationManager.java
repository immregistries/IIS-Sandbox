package org.immregistries.iis.kernal.fhir.security;

import org.hibernate.Session;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.servlet.PopServlet;
import org.immregistries.iis.kernal.servlet.ServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;

@Component
public class CustomAuthenticationManager
	implements AuthenticationManager
{
	Logger logger = LoggerFactory.getLogger(CustomAuthenticationManager.class);

	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
//		logger.info("AUTHENTICATION name {}", authentication.getName());
//		logger.info("AUTHENTICATION credential {}", authentication.getCredentials());
//		logger.info("AUTHENTICATION authenticated {}", authentication.isAuthenticated());
//		logger.info("AUTHENTICATION principal {}", authentication.getPrincipal());
//		logger.info("AUTHENTICATION details {}", authentication.getDetails());
//		logger.info("AUTHENTICATION authority {}", authentication.getAuthorities().toString());

		Session dataSession = PopServlet.getDataSession();
		// If credentials is string password
		OrgAccess orgAccess = ServletHelper.authenticateOrgAccess(authentication.getName(), (String) authentication.getCredentials(), authentication.getName(), dataSession);
		if (orgAccess == null ) {
			throw new BadCredentialsException("");
		} else {
//			HttpSession session = req.getSession(true);
//			session.setAttribute("orgAccess", orgAccess);

		}
		return orgAccess;
	}
}
