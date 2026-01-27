package org.immregistries.iis.kernal.controllers.rest;

import org.immregistries.iis.kernal.controllers.IisRestPath;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController

@RequestMapping(IisRestPath.BasePath.REST_PATH + IisRestPath.BasePath.AUTHENTICATION_PATH)
public class AuthenticationRestController {

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
