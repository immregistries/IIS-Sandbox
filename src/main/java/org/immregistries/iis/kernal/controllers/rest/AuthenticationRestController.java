package org.immregistries.iis.kernal.controllers.rest;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RestUrlUtil.REST_PATH + AuthenticationRestController.AUTHENTICATION_KEY_PATH)
public class AuthenticationRestController {

	public static final String AUTHENTICATION_KEY_PATH = "/authentication";

	/**
     * Get the current authentication state.
     * 
     * @return The current Authentication object.
     */
    @GetMapping()
    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
