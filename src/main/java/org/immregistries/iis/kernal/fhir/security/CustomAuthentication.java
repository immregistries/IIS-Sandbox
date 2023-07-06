package org.immregistries.iis.kernal.fhir.security;

import org.immregistries.iis.kernal.model.OrgAccess;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collection;

public class CustomAuthentication implements Serializable
//	implements Authentication
{
	private OrgAccess orgAccess;


	public Collection<? extends GrantedAuthority> getAuthorities() {
		return null;
	}

	public Object getCredentials() {
		if (orgAccess != null) {
			return orgAccess.getAccessKey();
		} else {
			return null;
		}
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

	public void setOrgAccess(OrgAccess orgAccess) {
		this.orgAccess = orgAccess;
	}
}
