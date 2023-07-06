package org.immregistries.iis.kernal.fhir.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component()
public class RejectedRequestRedirector
	implements RequestRejectedHandler, AuthenticationFailureHandler
{
	Logger logger = LoggerFactory.getLogger(RejectedRequestRedirector.class);

	public void handle(HttpServletRequest request, HttpServletResponse response, RequestRejectedException requestRejectedException) throws IOException, ServletException {
//		if (requestRejectedException.get)
		logger.info("REJECTED {}", requestRejectedException.getMessage());

		response.sendRedirect("/pop?error");
	}

	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
		logger.info("FAILURE {}", exception.getMessage());
		response.sendRedirect("/pop?error");

	}

}
