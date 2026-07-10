package org.immregistries.iis.kernal.controllers.rest;

import org.immregistries.iis.kernal.controllers.IisRestPath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController

@RequestMapping(IisRestPath.BasePath.REST_PATH + IisRestPath.BasePath.AUTHENTICATION_PATH)
public class AuthenticationRestController {
	Logger logger = LoggerFactory.getLogger(AuthenticationRestController.class);

    @GetMapping()
	 public AuthInfo getAuthentication() {
		 Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		 List<AuthInfo.Authority> authorities = auth.getAuthorities().stream()
			 .map(a -> new AuthInfo.Authority(a.getAuthority()))
			 .collect(Collectors.toList());
		 return new AuthInfo(auth.isAuthenticated(), auth.getName(), auth.getPrincipal(), authorities);
    }


}
