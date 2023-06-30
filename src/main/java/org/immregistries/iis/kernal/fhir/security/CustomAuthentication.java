package org.immregistries.iis.kernal.fhir.security;

import org.immregistries.iis.kernal.model.OrgAccess;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;

public class CustomAuthentication implements Authentication {
	private OrgAccess orgAccess;
	private String credential;

	public Collection<? extends GrantedAuthority> getAuthorities() {
		return null;
	}

	public Object getCredentials() {
		return credential;
//		return "credentials";
	}

	public Object getDetails() {
		return orgAccess;
	}

	public OrgAccess getPrincipal() {
		return orgAccess;
	}

	public boolean isAuthenticated() {
		return orgAccess != null;
	}

	public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException{

	}

	public String getName() {
		return orgAccess.getAccessName();
	}
}
